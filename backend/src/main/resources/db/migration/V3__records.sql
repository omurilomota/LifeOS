CREATE TABLE records (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    type VARCHAR(40) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(80),
    status VARCHAR(30) NOT NULL,
    responsible_member_id UUID REFERENCES household_memberships(id) ON DELETE SET NULL,
    acquired_at DATE,
    started_at DATE,
    archived_at TIMESTAMP WITH TIME ZONE,
    amount_minor BIGINT,
    currency VARCHAR(3),
    metadata_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_records_type CHECK (type IN ('ASSET', 'VEHICLE', 'PROPERTY', 'SERVICE', 'SUBSCRIPTION', 'CONTRACT', 'OTHER')),
    CONSTRAINT ck_records_status CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE INDEX idx_records_household_id ON records(household_id);
CREATE INDEX idx_records_household_status ON records(household_id, status);
CREATE INDEX idx_records_household_type ON records(household_id, type);
CREATE INDEX idx_records_household_title ON records(household_id, title);

CREATE TABLE record_relationships (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    from_record_id UUID NOT NULL REFERENCES records(id) ON DELETE CASCADE,
    to_record_id UUID NOT NULL REFERENCES records(id) ON DELETE CASCADE,
    relationship_type VARCHAR(60) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_record_relationships_not_self CHECK (from_record_id <> to_record_id),
    CONSTRAINT uq_record_relationship UNIQUE (household_id, from_record_id, to_record_id, relationship_type)
);

CREATE INDEX idx_record_relationships_household_from ON record_relationships(household_id, from_record_id);
CREATE INDEX idx_record_relationships_household_to ON record_relationships(household_id, to_record_id);
