CREATE TABLE ai_suggestions (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    provider VARCHAR(60) NOT NULL,
    status VARCHAR(30) NOT NULL,
    suggestion_type VARCHAR(40) NOT NULL,
    payload_json TEXT NOT NULL,
    confidence VARCHAR(20),
    evidence_text TEXT,
    created_by_user_id UUID REFERENCES users(id),
    confirmed_by_user_id UUID REFERENCES users(id),
    confirmed_at TIMESTAMP WITH TIME ZONE,
    rejected_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_ai_suggestions_status CHECK (status IN ('PENDING', 'CONFIRMED', 'REJECTED')),
    CONSTRAINT ck_ai_suggestions_type CHECK (suggestion_type IN ('RECORD', 'OBLIGATION'))
);

CREATE INDEX idx_ai_suggestions_household_doc ON ai_suggestions(household_id, document_id, created_at DESC);
