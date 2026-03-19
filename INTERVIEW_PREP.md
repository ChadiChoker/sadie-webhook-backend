# SADIE Webhook Receiver — Interview Preparation Guide

This file explains every part of the project in simple terms so you can confidently talk about it in an interview.

---

## What is this project?

CARLA is an AI phone assistant for car rental companies. When a customer calls, an AI called **Sadie Core** handles the conversation. Sadie fires HTTP requests (called webhooks) to our backend at key moments:

1. When a call **starts**
2. When the AI needs **rental data** (tool calls)
3. When the call **ends**

**Your job** was to build that backend server — receive these webhooks, store the data in a database, and expose an API for a dashboard to read it.

---

## The Big Picture — How it all connects

```
Customer calls dealership
        ↓
Sadie Core (AI) handles the call
        ↓
Sadie fires webhooks to YOUR backend (this project)
        ↓
Your backend stores everything in PostgreSQL
        ↓
Frontend dashboard reads from your backend API
```

---

## Project Folder Structure — Every file explained

```
sadie-webhook-backend/
├── src/
│   └── main/
│       ├── java/com/sadie/sadie_webhook_backend/
│       │   ├── SadieWebhookBackendApplication.java   ← Entry point
│       │   ├── config/
│       │   ├── controller/
│       │   ├── service/
│       │   ├── repository/
│       │   ├── entity/
│       │   ├── dto/
│       │   └── exception/
│       └── resources/
│           ├── application.properties
│           └── db/migration/
├── pom.xml
├── README.md
├── DOCUMENTATION.md
└── INTERVIEW_PREP.md   ← This file
```

---

## Entry Point

### `SadieWebhookBackendApplication.java`
**What it is:** The main class. The very first thing that runs when you start the app.

**What it does:** It has the `@SpringBootApplication` annotation which tells Spring Boot to start up, scan all your classes, connect to the database, and start the web server on port 8080.

**Interview tip:** "This is the main entry point. Spring Boot auto-configures everything — it detects my controllers, services, repositories, and wires them together automatically."

---

## `config/` folder — Application Configuration

### `WebhookAuthFilter.java`
**What it is:** A security filter that runs on every single request before it reaches any controller.

**What it does:**
- Reads the `x-sadie-core-secret` header from the incoming request
- Compares it to the secret stored in the environment variable `SADIE_CLIENT_SERVER_SECRET`
- If they match → let the request through
- If they don't match → immediately return `401 Unauthorized`

**Why it matters:** This is how we verify that requests are genuinely coming from Sadie Core and not from someone else. It's a shared secret authentication pattern.

**Interview tip:** "I used `OncePerRequestFilter` from Spring Security to intercept every request. It extends that class and overrides `doFilterInternal`. I used `shouldNotFilter` to only apply it to `/beta/api/**` paths so health checks or other routes aren't affected."

---

### `WebConfig.java`
**What it is:** CORS (Cross-Origin Resource Sharing) configuration.

**What it does:** Allows the React frontend running on `localhost:5173` to make requests to this backend on `localhost:8080`. Without this, browsers block cross-origin requests.

**Interview tip:** "CORS is a browser security feature. Since my frontend and backend run on different ports during development, I had to explicitly allow the frontend origin."

---

## `controller/` folder — HTTP Request Handlers

Controllers are the entry point for HTTP requests. They receive requests, call the right service, and return responses. They contain NO business logic.

### `WebhookController.java`
**Endpoint:** `POST /beta/api/sadie/webhook`

**What it does:** Receives all webhook events from Sadie. Reads the `type` field from the body and passes it to `WebhookService` to decide what to do.

**Handles two event types on the SAME endpoint:**
- `"type": "assistant-request"` → a call just started → creates session with `IN_PROGRESS`
- `"type": "end-of-call-report"` → a call just ended → updates session to `COMPLETED`, saves messages

```
POST /beta/api/sadie/webhook
        │
        ├── "type": "assistant-request"   → creates session (call started)
        │
        └── "type": "end-of-call-report" → updates session (call ended)
```

**Why one endpoint for two event types?**
This is a deliberate design decision called **event routing**. Sadie Core always fires to the same URL — it doesn't have separate URLs per event. Our backend reads the `type` field and routes to the correct handler internally. This is how most real-world webhook systems work (Stripe, GitHub, Twilio all use the same pattern). It keeps the API surface small and simple.

**Why NOT two separate endpoints?**
We could have done `POST /webhook/start` and `POST /webhook/end` — but that would mean Sadie Core needs to know about our internal structure. With a single endpoint, if we add a new event type in the future we just add a new handler in the service — we never change the URL.

**Interview tip:** "I used a single endpoint for multiple event types — this is called event routing. Sadie Core fires everything to one URL and includes a `type` field so my backend knows what happened. The controller just receives the request and delegates to the service. The service reads the `type` and calls the right handler. This keeps the API clean and easy to extend."

---

### `ToolController.java`
**Endpoints:**
- `POST /beta/api/sadie/tools/search-availability`
- `POST /beta/api/sadie/tools/create-reservation`

**What it does:** Receives tool call requests from Sadie when the AI needs rental data during a conversation. Returns hardcoded mock responses.

**Important rule:** These endpoints NEVER return a 5xx error. If anything goes wrong, they return `HTTP 200` with `"success": false`.

**Interview tip:** "The assessment required that tool endpoints never return 5xx. The AI relies on these responses to decide what to say next, so a server error would break the conversation. I handled this with try/catch in the service layer."

---

### `CallController.java`
**Endpoints:**
- `GET /beta/api/calls` → list all calls
- `GET /beta/api/calls/{callId}` → full detail of one call

**What it does:** Read-only endpoints for the frontend dashboard to display call data.

---

## `service/` folder — Business Logic

Services contain all the actual logic. Controllers call services. Services call repositories.

### `WebhookService.java` (interface)
Defines what the webhook service can do. Using an interface is good practice — it separates what a service does from how it does it.

### `WebhookServiceImpl.java` (implementation)
**The most important class in the project.**

**`handleAssistantRequest` method:**
1. Check if a session already exists for this `call_id`
2. If yes → return immediately (no duplicate created)
3. If no → create a new `CallSession` with `status = IN_PROGRESS`
4. Save to database

**Why the duplicate check?** Network issues can cause Sadie to send the same event twice. We only ever want one session per call. This is called **idempotency**.

**`handleEndOfCallReport` method:**
1. Find the existing session by `call_id`
2. Update it: set `COMPLETED`, add transcript, summary, recording URL, duration, category
3. Save all the conversation messages to `call_messages` table
4. Retroactively link any tool call logs that arrived before this event

**Interview tip:** "The end-of-call-report handler is wrapped in a single `@Transactional` annotation. This means all the DB operations (session update + messages insert + tool log linking) either all succeed or all fail together. This prevents partial data."

---

### `ToolServiceImpl.java`
**What it does:**
1. Saves the incoming request to `tool_call_logs` (audit trail)
2. Builds the hardcoded mock response
3. Updates the log with the response
4. Returns the response

**Why log tool calls?** The dashboard needs to show what the AI asked for and what it got back during a call. This is the audit trail.

**Interview tip:** "Even though the responses are hardcoded, I still log every request and response. This gives full visibility into what the AI was doing during each call."

---

### `CallServiceImpl.java`
**What it does:** Queries the database and builds the response DTOs for the frontend.

- `listSessions()` → gets all sessions sorted by most recent
- `getDetail()` → gets one session + its messages + its tool logs

---

## `repository/` folder — Database Access

Repositories talk directly to the database. In Spring Data JPA, you just define an interface and Spring generates all the SQL automatically.

### `CallSessionRepository.java`
```java
boolean existsByCallId(String callId);        // Used for idempotency check
Optional<CallSession> findByCallId(String callId);  // Used to find a session
```
**Interview tip:** "Spring Data JPA generates the SQL from the method name. `findByCallId` automatically becomes `SELECT * FROM call_sessions WHERE call_id = ?`."

### `CallMessageRepository.java`
```java
List<CallMessage> findBySessionIdOrderBySecondsFromStartAsc(UUID sessionId);
```
Returns messages for a session ordered by when they happened in the call.

### `ToolCallLogRepository.java`
```java
int linkToSession(CallSession session, String callId);
```
This is a custom `@Query` that retroactively links tool logs to a session after the call ends.

---

## `entity/` folder — Database Tables as Java Classes

Entities map directly to database tables. Each field maps to a column.

### `CallSession.java`
Maps to the `call_sessions` table. Represents one phone call.

Key annotations:
- `@Entity` → this class is a database table
- `@Table(name = "call_sessions")` → the table name
- `@Id @GeneratedValue(strategy = GenerationType.UUID)` → auto-generate UUID primary key
- `@Column(name = "call_id", unique = true)` → maps to a column, must be unique
- `@Enumerated(EnumType.STRING)` → stores the enum as text (IN_PROGRESS / COMPLETED)
- `@PrePersist` / `@PreUpdate` → automatically sets `created_at` and `updated_at`

### `CallMessage.java`
Maps to `call_messages`. One row per message in the conversation.

Has a `@ManyToOne` relationship to `CallSession` — many messages belong to one session.

### `ToolCallLog.java`
Maps to `tool_call_logs`. Audit log of tool calls.

Uses `@JdbcTypeCode(SqlTypes.JSON)` to store and retrieve `Map<String, Object>` as PostgreSQL JSONB.

### `CallStatus.java`
A simple Java enum with two values: `IN_PROGRESS` and `COMPLETED`.

---

## `dto/` folder — Data Transfer Objects

DTOs are plain objects used to transfer data. They are NOT entities — they don't map to DB tables.

**Why use DTOs instead of returning entities directly?**
- Control exactly what fields are exposed in the API
- Avoid accidentally exposing internal fields
- Separate the DB model from the API model

### `dto/webhook/WebhookEventRequest.java`
Maps the incoming JSON from Sadie Core to a Java object. Uses `@JsonProperty` to map snake_case JSON fields (`customer_number`) to camelCase Java fields (`customerNumber`).

### `dto/tool/ToolRequest.java`
Maps the incoming tool call JSON to a Java object.

### `dto/api/CallSessionDto.java`
What we return when listing sessions. Only includes the fields the frontend needs.

### `dto/api/CallDetailDto.java`
Extends `CallSessionDto` and adds `messages` and `toolLogs` lists. Returned for the detail view.

---

## `exception/` folder — Error Handling

### `GlobalExceptionHandler.java`
A `@RestControllerAdvice` class that catches exceptions thrown anywhere in the app and converts them to proper JSON responses.

| Exception | HTTP Status | Response |
|-----------|-------------|----------|
| `ResponseStatusException` | varies (404, etc.) | `{"success": false, "error": "..."}` |
| `HttpMessageNotReadableException` | 400 | `{"success": false, "error": "Invalid JSON body"}` |
| `NoResourceFoundException` | 404 | `{"success": false, "error": "Not found"}` |
| Any other exception | 500 | `{"success": false, "error": "An unexpected error occurred"}` |

**Interview tip:** "Without this, Spring would return its default HTML error page or a different JSON format. The GlobalExceptionHandler ensures all errors return a consistent JSON structure."

---

## `resources/` folder — Configuration Files

### `application.properties`
The main configuration file. Sets:
- Database connection URL, username, password
- JPA settings (validate schema — never auto-create or drop)
- Flyway settings (auto-run migrations on startup)
- The webhook secret (read from environment variable with fallback to `test-secret`)
- Server port (8080)

### `db/migration/` folder
Contains Flyway SQL migration files. These create the database tables.

**Why Flyway?** It tracks which migrations have been applied. If you add a new `V4__...sql` file, it only runs that one. This is how you safely change the database schema in production without losing data.

| File | What it creates |
|------|----------------|
| `V1__create_call_sessions.sql` | The main calls table |
| `V2__create_call_messages.sql` | The messages table |
| `V3__create_tool_call_logs.sql` | The tool audit log table |

**Golden rule:** Never edit a migration file after it's been applied. Always add a new one.

---

## `pom.xml` — Project Dependencies

Maven's configuration file. Lists all the libraries the project uses:

| Dependency | Why we use it |
|-----------|---------------|
| `spring-boot-starter-web` | Builds REST APIs, includes Tomcat web server |
| `spring-boot-starter-data-jpa` | Talk to the database using Java objects |
| `spring-boot-starter-validation` | Validate request fields |
| `postgresql` | PostgreSQL database driver |
| `flyway-core` | Database migration management |
| `flyway-database-postgresql` | Flyway support for PostgreSQL specifically |
| `lombok` | Auto-generates getters, setters, constructors to reduce boilerplate |

---

## Common Interview Questions

**Q: Why Spring Boot?**
"Spring Boot makes it easy to build production-ready REST APIs quickly. It auto-configures everything — I don't need to set up Tomcat, configure JPA manually, or write boilerplate. I can focus on business logic."

**Q: What is Flyway and why use it?**
"Flyway is a database migration tool. Instead of manually running SQL, I write versioned migration files. Flyway tracks which ones ran and applies new ones automatically on startup. This makes the schema reproducible on any machine."

**Q: What does @Transactional do?**
"It wraps the method in a database transaction. All DB operations inside it either all succeed or all fail together. If an exception is thrown, everything is rolled back. I used it on the end-of-call-report handler because it does multiple DB operations — session update, messages insert, and tool log linking — and they must all succeed or fail as one unit."

**Q: How did you handle the 'no duplicate sessions' requirement?**
"Two layers: first, in the service I call `existsByCallId()` before inserting. Second, the `call_id` column has a UNIQUE constraint in the database. If two requests somehow arrive at the same millisecond, the DB constraint rejects the second one and I catch the `DataIntegrityViolationException` and treat it as a no-op."

**Q: Why did tool endpoints never return 5xx?**
"Because Sadie Core (the AI) uses the response to decide what to say to the caller next. If the server crashes with a 500, the AI has no idea how to handle that. By always returning 200 with `success: false` and recovery steps, the AI can tell the caller there was an issue and try again gracefully."

**Q: What is the difference between an Entity and a DTO?**
"An Entity maps directly to a database table — it's managed by JPA/Hibernate. A DTO is just a plain object for transferring data. I use DTOs in my API responses to control exactly what fields are exposed and to decouple the database model from the API contract."

**Q: What is CORS and why did you configure it?**
"CORS is a browser security policy that blocks requests from one origin to another. My frontend on port 5173 makes requests to my backend on port 8080 — different ports means different origins. I configured Spring to allow requests from `localhost:5173` so the browser doesn't block them."

**Q: Why use interfaces for your services?**
"It's a best practice for separation of concerns. The interface defines the contract — what the service does. The implementation defines how it does it. It also makes the code easier to test since you can swap in a mock implementation."

---

## Assessment Acceptance Criteria — All Completed ✅

These are the exact requirements from the assessment document and how each one was met.

### Backend

| ✅ | Requirement | How it was implemented |
|----|-------------|----------------------|
| ✅ | All 3 event types handled | `WebhookController` handles `assistant-request` and `end-of-call-report`. `ToolController` handles both tool endpoints. |
| ✅ | `x-sadie-core-secret` validated on every endpoint — 401 if missing or wrong | `WebhookAuthFilter` runs before every request under `/beta/api/**` |
| ✅ | `assistant-request` creates session with `IN_PROGRESS` | `WebhookServiceImpl.handleAssistantRequest()` sets `status = IN_PROGRESS` |
| ✅ | Duplicate `call_id` does not create a second row | `existsByCallId()` check + UNIQUE constraint on `call_sessions.call_id` |
| ✅ | Tool endpoints return exact mock responses (all 4 fields: success, data, description, steps) | `ToolServiceImpl` returns hardcoded responses matching the document exactly |
| ✅ | Tool calls logged with request + response payloads | `ToolCallLog` saved to DB before and after each tool call |
| ✅ | `end-of-call-report` updates session to `COMPLETED` with all fields | `WebhookServiceImpl.handleEndOfCallReport()` updates all fields |
| ✅ | Messages saved to `call_messages` | Bulk saved inside `handleEndOfCallReport()` |
| ✅ | Flyway migrations run cleanly on fresh database | 3 migration files — tested on fresh DB |
| ✅ | No 5xx responses under any circumstance | try/catch in service + GlobalExceptionHandler |

### Frontend (to be completed)

| ⏳ | Call log page lists sessions with status, category, duration, timestamp |
| ⏳ | Clicking a row shows transcript + tool call audit log |
| ⏳ | Data loads from the REST API endpoints |

---

## Key Facts to Remember for the Interview

**About the mock data:**
- The hardcoded responses are the **correct, complete implementation** — not a placeholder
- The assessment explicitly says: *"Returning mock data is the expected, correct implementation"*
- No real reservation system exists. No API keys exist. No external services are involved.
- You are not cutting corners — this IS the requirement.

**About the tool response structure:**
Every tool response always has exactly 4 fields:
```json
{
  "success": true/false,
  "data": { ... },
  "description": "Human-readable explanation for the AI",
  "steps": ["Step 1 the AI should follow", "Step 2"]
}
```
The `steps` array is what Sadie Core uses to decide what to say to the caller next.

**About the `restaurant_number` field:**
Sadie Core uses restaurant terminology internally. The field `restaurant_number` in the webhook payload is actually the **dealership's phone number**. We just store it as `dealership_number`.

**About authentication:**
- These endpoints use a **shared secret** — NOT JWT
- The secret is passed as a header: `x-sadie-core-secret`
- Validated against environment variable `SADIE_CLIENT_SERVER_SECRET`
- For local dev the secret is `test-secret`

**About the database conventions used:**
- All primary keys are **UUIDs** (not auto-increment integers)
- Every table has a **`tenant_id`** column (supports multiple dealerships)
- Every table has **`created_at`** and **`updated_at`** columns
- All timestamps are stored as **TIMESTAMPTZ** (timezone-aware)

**About `ended_reason` values:**
| Value | Meaning |
|-------|---------|
| `customer-ended-call` | Caller hung up |
| `assistant-ended-call` | AI ended the call |
| `transfer-requested` | Call transferred |
| `timeout` | Call timed out |
| `error` | Technical error |

**About `category` values:**
| Value | Meaning |
|-------|---------|
| `BOOKING` | Customer made a reservation |
| `CANCELLATION` | Customer cancelled |
| `MODIFICATION` | Customer changed a booking |
| `STATUS_CHECK` | Customer asked about a booking |
| `INQUIRY` | General question |
| `OTHER` | Anything else |

---

## The Full Request Lifecycle (what happens when Sadie sends a webhook)

```
1. Sadie fires:  POST /beta/api/sadie/webhook
                 Header: x-sadie-core-secret: abc123
                 Body:   { "type": "assistant-request", "call_id": "xyz", ... }

2. WebhookAuthFilter runs:
   - Reads header value: "abc123"
   - Compares to env var: "abc123" ✓
   - Passes request through

3. WebhookController receives request:
   - Deserializes JSON body into WebhookEventRequest object
   - Calls webhookService.handle(request)

4. WebhookServiceImpl.handle():
   - Reads request.getType() → "assistant-request"
   - Calls handleAssistantRequest(request)

5. handleAssistantRequest():
   - Calls callSessionRepository.existsByCallId("xyz") → false
   - Creates new CallSession object
   - Sets fields: callId, tenantId, status=IN_PROGRESS, startedAt=now()
   - Calls callSessionRepository.save(session) → SQL INSERT

6. Returns: { "success": true }

7. Controller wraps in ResponseEntity.ok() → HTTP 200

8. Response sent back to Sadie Core
```
