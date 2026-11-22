CREATE TABLE obligations (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    record_id UUID REFERENCES records(id) ON DELETE SET NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    responsible_member_id UUID REFERENCES household_memberships(id) ON DELETE SET NULL,
    due_at TIMESTAMP WITH TIME ZONE NOT NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    recurrence_rule VARCHAR(120),
    source_type VARCHAR(40) NOT NULL DEFAULT 'MANUAL',
    source_document_id UUID REFERENCES documents(id) ON DELETE SET NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_obligations_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT ck_obligations_status CHECK (status IN ('OPEN', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX idx_obligations_household_due ON obligations(household_id, due_at);
CREATE INDEX idx_obligations_household_status ON obligations(household_id, status);

CREATE TABLE reminders (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    obligation_id UUID NOT NULL REFERENCES obligations(id) ON DELETE CASCADE,
    scheduled_at TIMESTAMP WITH TIME ZONE NOT NULL,
    channel VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    delivered_at TIMESTAMP WITH TIME ZONE,
    dedupe_key VARCHAR(200) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_reminders_channel CHECK (channel IN ('IN_APP')),
    CONSTRAINT ck_reminders_status CHECK (status IN ('PENDING', 'DELIVERED', 'CANCELLED'))
);

CREATE INDEX idx_reminders_household_scheduled ON reminders(household_id, scheduled_at);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(60) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body TEXT,
    read_at TIMESTAMP WITH TIME ZONE,
    related_entity_type VARCHAR(60),
    related_entity_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_notifications_user_created ON notifications(user_id, created_at DESC);
CREATE INDEX idx_notifications_household_user_read ON notifications(household_id, user_id, read_at);
