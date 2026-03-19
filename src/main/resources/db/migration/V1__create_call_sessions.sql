CREATE TABLE call_sessions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         VARCHAR(255) NOT NULL,
    call_id           VARCHAR(255) NOT NULL UNIQUE,
    assistant_id      VARCHAR(255),
    customer_number   VARCHAR(50),
    dealership_number VARCHAR(50),
    status            VARCHAR(20)  NOT NULL DEFAULT 'IN_PROGRESS',
    transcript        TEXT,
    summary           TEXT,
    recording_url     TEXT,
    duration_seconds  NUMERIC(10,2),
    duration_minutes  NUMERIC(10,2),
    category          VARCHAR(255),
    ended_reason      VARCHAR(255),
    started_at        TIMESTAMPTZ,
    ended_at          TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_call_sessions_call_id   ON call_sessions(call_id);
CREATE INDEX idx_call_sessions_tenant_id ON call_sessions(tenant_id);
