CREATE TABLE tool_call_logs (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id    UUID REFERENCES call_sessions(id) ON DELETE SET NULL,
    tenant_id     VARCHAR(255) NOT NULL,
    tool_name     VARCHAR(255) NOT NULL,
    call_id       VARCHAR(255),
    request_json  JSONB,
    response_json JSONB,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tool_call_logs_session_id ON tool_call_logs(session_id);
CREATE INDEX idx_tool_call_logs_call_id    ON tool_call_logs(call_id);
