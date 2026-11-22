CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    locale VARCHAR(20) NOT NULL DEFAULT 'pt-BR',
    timezone VARCHAR(64) NOT NULL DEFAULT 'America/Sao_Paulo',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE households (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    owner_user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE household_memberships (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_household_membership UNIQUE (household_id, user_id),
    CONSTRAINT ck_household_membership_role CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER', 'VIEWER'))
);

CREATE INDEX idx_household_memberships_user_id ON household_memberships(user_id);
CREATE INDEX idx_household_memberships_household_id ON household_memberships(household_id);

CREATE TABLE household_invitations (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    invited_email VARCHAR(320) NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    accepted_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_household_invitation_role CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER', 'VIEWER'))
);

CREATE INDEX idx_household_invitations_household_id ON household_invitations(household_id);
CREATE INDEX idx_household_invitations_email ON household_invitations(invited_email);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    household_id UUID REFERENCES households(id) ON DELETE SET NULL,
    actor_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id UUID,
    action VARCHAR(80) NOT NULL,
    metadata_json TEXT,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_audit_events_household_occurred ON audit_events(household_id, occurred_at DESC);
