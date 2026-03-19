# SADIE Webhook Receiver — Backend

Spring Boot backend for the CARLA platform that receives, processes, and stores AI call webhook events from Sadie Core.

---

## Tech Stack

| Technology | Version |
|-----------|---------|
| Java | 17 |
| Spring Boot | 3.5.11 |
| PostgreSQL | 15 |
| Flyway | (managed by Spring Boot) |
| Spring Data JPA | (managed by Spring Boot) |
| Lombok | (managed by Spring Boot) |
| Maven | 3.9.2 |

---

## Project Structure

```
src/main/java/com/sadie/sadie_webhook_backend/
├── config/
│   ├── WebhookAuthFilter.java       # Validates x-sadie-core-secret header
│   └── WebConfig.java               # CORS configuration
├── controller/
│   ├── WebhookController.java       # POST /beta/api/sadie/webhook
│   ├── ToolController.java          # POST /beta/api/sadie/tools/**
│   └── CallController.java          # GET /beta/api/calls/**
├── service/
│   ├── WebhookService.java
│   ├── ToolService.java
│   ├── CallService.java
│   └── impl/
│       ├── WebhookServiceImpl.java
│       ├── ToolServiceImpl.java
│       └── CallServiceImpl.java
├── repository/
│   ├── CallSessionRepository.java
│   ├── CallMessageRepository.java
│   └── ToolCallLogRepository.java
├── entity/
│   ├── CallSession.java
│   ├── CallMessage.java
│   ├── ToolCallLog.java
│   └── CallStatus.java              # Enum: IN_PROGRESS, COMPLETED
├── dto/
│   ├── webhook/
│   │   └── WebhookEventRequest.java
│   ├── tool/
│   │   └── ToolRequest.java
│   └── api/
│       ├── CallSessionDto.java
│       ├── CallDetailDto.java
│       ├── CallMessageDto.java
│       └── ToolCallLogDto.java
└── exception/
    └── GlobalExceptionHandler.java
```

---

## Database Schema

Three tables managed by Flyway migrations:

### `call_sessions`
One row per phone call.

| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| tenant_id | VARCHAR | Dealership identifier |
| call_id | VARCHAR (UNIQUE) | Sadie call identifier |
| assistant_id | VARCHAR | AI assistant ID |
| customer_number | VARCHAR | Caller's phone number |
| dealership_number | VARCHAR | Dealership phone number |
| status | VARCHAR | IN_PROGRESS or COMPLETED |
| transcript | TEXT | Full call transcript |
| summary | TEXT | AI-generated call summary |
| recording_url | TEXT | URL to call recording |
| duration_seconds | NUMERIC | Call duration in seconds |
| duration_minutes | NUMERIC | Call duration in minutes |
| category | VARCHAR | BOOKING, CANCELLATION, INQUIRY, etc. |
| ended_reason | VARCHAR | How the call ended |
| started_at | TIMESTAMPTZ | Call start time |
| ended_at | TIMESTAMPTZ | Call end time |
| created_at | TIMESTAMPTZ | Row creation time |
| updated_at | TIMESTAMPTZ | Row last updated time |

### `call_messages`
Individual transcript messages linked to a session.

| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| session_id | UUID (FK) | References call_sessions |
| tenant_id | VARCHAR | Dealership identifier |
| role | VARCHAR | bot or user |
| message | TEXT | Message content |
| time | NUMERIC | Absolute timestamp (ms) |
| end_time | NUMERIC | Message end timestamp (ms) |
| seconds_from_start | NUMERIC | Offset from call start |
| duration | NUMERIC | Message duration (ms) |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |

### `tool_call_logs`
Audit log of every tool call made during a session.

| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| session_id | UUID (FK, nullable) | References call_sessions |
| tenant_id | VARCHAR | Dealership identifier |
| tool_name | VARCHAR | search-availability or create-reservation |
| call_id | VARCHAR | Sadie call identifier |
| request_json | JSONB | Raw incoming request payload |
| response_json | JSONB | Mock response returned |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |

---

## Setup & Running

### Prerequisites
- Java 17
- Maven 3.9+
- PostgreSQL 15

### 1. Create the database
```bash
psql -U postgres -c "CREATE DATABASE sadie_db;"
```

### 2. Configure environment
The app uses `test-secret` as the default secret for local development.
To override, set the environment variable:
```bash
export SADIE_CLIENT_SERVER_SECRET=your-secret-here
```

### 3. Run the application
```bash
mvn spring-boot:run
```

The app starts on `http://localhost:8080`.
Flyway automatically runs all migrations on startup.

---

## Authentication

Every endpoint under `/beta/api/**` requires the following header:

```
x-sadie-core-secret: <secret>
```

Returns `401 Unauthorized` if the header is missing or incorrect.

---

## API Endpoints

### Webhook Events

#### `POST /beta/api/sadie/webhook`
Handles two event types dispatched by Sadie Core.

**Event: `assistant-request`** — fires when a call begins.
```json
{
  "type": "assistant-request",
  "call_id": "...",
  "tenant_id": "...",
  "customer_number": "+1...",
  "restaurant_number": "+1...",
  "assistant_id": "..."
}
```
- Creates a new `call_sessions` row with `status = IN_PROGRESS`
- Duplicate `call_id` is silently ignored (idempotent)
- Returns: `HTTP 200`

---

**Event: `end-of-call-report`** — fires when a call ends.
```json
{
  "type": "end-of-call-report",
  "call_id": "...",
  "transcript": "...",
  "summary": "...",
  "category": "BOOKING",
  "ended_reason": "assistant-ended-call",
  "messages": [...]
}
```
- Updates session to `status = COMPLETED`
- Saves all messages to `call_messages`
- Links any orphaned `tool_call_logs` to the session
- Returns: `HTTP 200`

---

### Tool Endpoints

#### `POST /beta/api/sadie/tools/search-availability`
Returns hardcoded mock availability data.
```json
{
  "call_id": "...",
  "arguments": {
    "pickupDate": "2026-03-22T10:00:00",
    "returnDate": "2026-03-25T10:00:00",
    "pickupLocation": "Downtown",
    "driverAge": "28"
  }
}
```
- Logs request + response to `tool_call_logs`
- Always returns `HTTP 200` (never 5xx)

---

#### `POST /beta/api/sadie/tools/create-reservation`
Returns hardcoded mock reservation confirmation.
```json
{
  "call_id": "...",
  "arguments": {
    "vehicleClassId": "cls_midsize",
    "firstName": "John",
    "lastName": "Smith",
    ...
  }
}
```
- Returns error response if `firstName` or `lastName` is missing
- Logs request + response to `tool_call_logs`
- Always returns `HTTP 200` (never 5xx)

---

### Read Endpoints

#### `GET /beta/api/calls`
Returns all call sessions sorted by most recent first.

#### `GET /beta/api/calls/{callId}`
Returns full session detail including:
- Session fields + transcript + summary + recording URL
- `messages` array (ordered by secondsFromStart)
- `toolLogs` array (ordered by createdAt)

---

## Mock Responses

All tool responses are hardcoded as per the assessment specification.
No external APIs or real reservation systems are involved.

**search-availability** always returns 3 vehicle classes:
- Economy — $39/day, $117 total
- Mid-Size — $59/day, $177 total
- SUV — $89/day, $267 total

**create-reservation** always returns confirmation number `87430618`.

---

## Key Design Decisions

- **Idempotency**: `call_id` has a UNIQUE constraint in the DB. The service also does an `existsByCallId` check before inserting to avoid unnecessary exceptions.
- **No 5xx from tool endpoints**: Tool service wraps all logic in try/catch. Controller adds a second layer. GlobalExceptionHandler is the final safety net.
- **Retroactive tool log linking**: Tool calls may arrive before `end-of-call-report`. When the report arrives, orphaned logs are linked via `UPDATE ... WHERE call_id = ? AND session_id IS NULL`.
- **JSONB for tool logs**: `request_json` and `response_json` are stored as PostgreSQL JSONB for efficient querying.
