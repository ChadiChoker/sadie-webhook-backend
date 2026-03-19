CREATE TABLE call_messages (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id          UUID NOT NULL REFERENCES call_sessions(id) ON DELETE CASCADE,
    tenant_id           VARCHAR(255) NOT NULL,
    role                VARCHAR(10) NOT NULL,
    message             TEXT,
    time                NUMERIC(20,4),
    end_time            NUMERIC(20,4),
    seconds_from_start  NUMERIC(10,4),
    duration            NUMERIC(10,4),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_call_messages_session_id ON call_messages(session_id);
