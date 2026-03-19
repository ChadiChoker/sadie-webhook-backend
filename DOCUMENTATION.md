# SADIE Webhook Receiver — Technical Documentation

## Overview

SADIE Webhook Receiver is the backend server for the CARLA AI phone assistant platform. It receives HTTP webhook events from Sadie Core (the AI engine), stores call data in PostgreSQL, and exposes read endpoints for the frontend dashboard.

> All tool call responses use hardcoded mock data. No external APIs or live systems are involved.

---

## Architecture

```
Sadie Core (AI Engine)
        │
        │  HTTP Webhooks
        ▼
┌─────────────────────────┐
│   WebhookAuthFilter     │  Validates x-sadie-core-secret header
└────────────┬────────────┘
             │
    ┌────────▼────────┐
    │   Controllers   │
    │  ─────────────  │
    │  Webhook        │  POST /beta/api/sadie/webhook
    │  Tool           │  POST /beta/api/sadie/tools/**
    │  Call           │  GET  /beta/api/calls/**
    └────────┬────────┘
             │
    ┌────────▼────────┐
    │    Services     │
    │  ─────────────  │
    │  WebhookService │  Handles assistant-request & end-of-call-report
    │  ToolService    │  Returns mock responses & logs tool calls
    │  CallService    │  Reads sessions & details for the dashboard
    └────────┬────────┘
             │
    ┌────────▼────────┐
    │  Repositories   │  Spring Data JPA
    └────────┬────────┘
             │
    ┌────────▼────────┐
    │   PostgreSQL    │  call_sessions, call_messages, tool_call_logs
    └─────────────────┘
```

---

## Single Endpoint, Multiple Event Types

`POST /beta/api/sadie/webhook` handles two different events on the **same URL**.

```
POST /beta/api/sadie/webhook
        │
        ├── "type": "assistant-request"    → creates call session (IN_PROGRESS)
        │
        └── "type": "end-of-call-report"  → updates session (COMPLETED) + saves messages
```

**Why one endpoint?**
This is the **event routing** pattern. Sadie Core always fires to the same URL and includes a `type` field in the body. The backend reads `type` and routes to the correct handler internally. Benefits:
- Sadie Core only needs to know one URL
- Adding new event types requires no URL changes — just a new handler in the service
- Follows how real webhook systems work (Stripe, GitHub, Twilio use the same pattern)

The `WebhookController` receives the request and passes it to `WebhookServiceImpl.handle()`. The service reads `request.getType()` and dispatches:
```java
if ("assistant-request".equals(type))    return handleAssistantRequest(request);
if ("end-of-call-report".equals(type))  return handleEndOfCallReport(request);
```

---

## Webhook Flow

### Flow 1 — New Call

```
Sadie Core  →  POST /beta/api/sadie/webhook  (type: assistant-request)
                        │
                        ▼
             Check if call_id already exists
                        │
              ┌─────────┴──────────┐
            Exists              Not exists
              │                    │
           Return               Create call_session
         no-op 200             (status: IN_PROGRESS)
                                   │
                                Return 200
```

### Flow 2 — Tool Call

```
Sadie Core  →  POST /beta/api/sadie/tools/search-availability
                        │
                        ▼
             Save ToolCallLog (request_json, tool_name, call_id)
                        │
                        ▼
             Link to CallSession if exists
                        │
                        ▼
             Return hardcoded mock response
                        │
                        ▼
             Update ToolCallLog (response_json)
                        │
                   Return 200
```

### Flow 3 — Call Ends

```
Sadie Core  →  POST /beta/api/sadie/webhook  (type: end-of-call-report)
                        │
                        ▼
             Find CallSession by call_id
                        │
                        ▼
             Update session:
               - status = COMPLETED
               - transcript, summary, recording_url
               - category, ended_reason, duration
               - started_at, ended_at
                        │
                        ▼
             Save all messages → call_messages
                        │
                        ▼
             Retroactively link orphaned tool_call_logs
             (WHERE call_id = ? AND session_id IS NULL)
                        │
                   Return 200
```

---

## Authentication

All endpoints under `/beta/api/**` are protected by `WebhookAuthFilter`.

```
Request Header:  x-sadie-core-secret: <value>
                         │
              ┌──────────▼──────────┐
              │  Compare with env   │
              │  SADIE_CLIENT_      │
              │  SERVER_SECRET      │
              └──────────┬──────────┘
                         │
               ┌─────────┴──────────┐
             Match               No match
               │                    │
          Pass to               Return 401
          handler               {"error":"Unauthorized"}
```

Default secret for local development: `test-secret`

---

## Error Handling Strategy

The system uses 3 layers of error protection, especially for tool endpoints:

| Layer | Location | Purpose |
|-------|----------|---------|
| 1 | `ToolServiceImpl` | try/catch around all logic — returns `success:false` |
| 2 | `ToolController` | Secondary safety net |
| 3 | `GlobalExceptionHandler` | Last resort for any unhandled exception |

**Rule: Tool endpoints never return 5xx under any circumstance.**

Other endpoints return appropriate HTTP status codes:
- `400` — Invalid JSON body
- `401` — Missing or wrong secret
- `404` — Session not found
- `500` — Unexpected server error

---

## Idempotency

The `assistant-request` event is idempotent — sending the same `call_id` twice never creates duplicate rows.

Two-layer protection:
1. **Service layer**: `existsByCallId(callId)` check before insert
2. **Database layer**: `UNIQUE` constraint on `call_sessions.call_id`

If a race condition causes two concurrent requests with the same `call_id`, the DB constraint rejects the second insert. The `DataIntegrityViolationException` is caught and treated as a no-op.

---

## Retroactive Tool Log Linking

Tool calls may arrive before or after the `end-of-call-report`. The `session_id` column on `tool_call_logs` is nullable to handle this.

**When a tool call arrives:**
- If a session exists for the `call_id` → link immediately
- If no session exists yet → save with `session_id = NULL`

**When `end-of-call-report` arrives:**
- After saving the session, run:
```sql
UPDATE tool_call_logs
SET session_id = :sessionId
WHERE call_id = :callId AND session_id IS NULL
```

This ensures all tool logs are always linked to their session eventually.

---

## Data Types

| Java Type | PostgreSQL Type | Used For |
|-----------|----------------|---------|
| `UUID` | `UUID` | All primary keys |
| `String` | `VARCHAR` | IDs, names, categories |
| `OffsetDateTime` | `TIMESTAMPTZ` | All timestamps |
| `BigDecimal` | `NUMERIC` | Duration, price, timing |
| `Map<String, Object>` | `JSONB` | Tool call request/response |
| `CallStatus` (enum) | `VARCHAR` | Session status |

---

## Configuration Reference

| Property | Default | Description |
|----------|---------|-------------|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/sadie_db` | DB connection |
| `spring.datasource.username` | `postgres` | DB user |
| `spring.jpa.hibernate.ddl-auto` | `validate` | Hibernate validates schema only |
| `spring.flyway.enabled` | `true` | Auto-run migrations on startup |
| `sadie.secret` | `test-secret` | Webhook auth secret |
| `server.port` | `8080` | HTTP port |

Override `sadie.secret` via environment variable:
```bash
export SADIE_CLIENT_SERVER_SECRET=your-production-secret
```

---

## Flyway Migrations

| Version | File | Description |
|---------|------|-------------|
| V1 | `V1__create_call_sessions.sql` | Main call session table |
| V2 | `V2__create_call_messages.sql` | Transcript messages table |
| V3 | `V3__create_tool_call_logs.sql` | Tool call audit log table |

> Never modify an existing migration file. Add a new version (V4, V5...) for any schema changes.

---

## Mock Response Reference

### search-availability
```json
{
  "success": true,
  "data": {
    "available": true,
    "vehicles": [
      { "vehicleClassId": "cls_economy", "className": "Economy", "pricePerDay": 39.00, "totalPrice": 117.00 },
      { "vehicleClassId": "cls_midsize", "className": "Mid-Size", "pricePerDay": 59.00, "totalPrice": 177.00 },
      { "vehicleClassId": "cls_suv",     "className": "SUV",      "pricePerDay": 89.00, "totalPrice": 267.00 }
    ]
  },
  "description": "3 vehicle classes available for the requested dates",
  "steps": [
    "Present the 3 available options with prices",
    "Ask the caller which vehicle class they prefer",
    "Confirm their selection before proceeding"
  ]
}
```

### create-reservation (success)
```json
{
  "success": true,
  "data": {
    "confirmationNumber": "87430618",
    "status": "CONFIRMED",
    "totalPrice": 177.00,
    "currency": "USD"
  },
  "description": "Reservation confirmed for John Smith",
  "steps": [
    "Tell the caller their reservation is confirmed",
    "Give them confirmation number 87430618",
    "Let them know a payment link will be sent by SMS",
    "Ask if there is anything else you can help with"
  ]
}
```

### create-reservation (missing fields)
```json
{
  "success": false,
  "data": { "error": "Missing required fields: firstName, lastName" },
  "description": "Cannot create reservation without customer name",
  "steps": [
    "Politely ask the caller for their first and last name",
    "Retry the reservation with the complete information"
  ]
}
```
