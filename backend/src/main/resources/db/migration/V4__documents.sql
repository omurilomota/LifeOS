CREATE TABLE documents (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    uploaded_by_user_id UUID NOT NULL REFERENCES users(id),
    original_filename VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    mime_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT NOT NULL,
    checksum_sha256 VARCHAR(64) NOT NULL,
    extraction_status VARCHAR(30) NOT NULL,
    extracted_text TEXT,
    extraction_error VARCHAR(500),
    review_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_documents_extraction_status CHECK (extraction_status IN ('PENDING', 'PROCESSING', 'READY', 'FAILED')),
    CONSTRAINT ck_documents_review_status CHECK (review_status IN ('PENDING', 'REVIEWED', 'REJECTED'))
);

CREATE INDEX idx_documents_household_created ON documents(household_id, created_at DESC);
CREATE INDEX idx_documents_household_extraction ON documents(household_id, extraction_status);

CREATE TABLE record_documents (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    record_id UUID NOT NULL REFERENCES records(id) ON DELETE CASCADE,
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    link_type VARCHAR(60) NOT NULL DEFAULT 'ATTACHMENT',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_record_documents UNIQUE (record_id, document_id, link_type)
);

CREATE INDEX idx_record_documents_household_record ON record_documents(household_id, record_id);
CREATE INDEX idx_record_documents_household_document ON record_documents(household_id, document_id);

CREATE TABLE extraction_jobs (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL,
    provider VARCHAR(60) NOT NULL DEFAULT 'BASIC',
    attempt_count INT NOT NULL DEFAULT 0,
    result_json TEXT,
    error_code VARCHAR(80),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_extraction_jobs_status CHECK (status IN ('PENDING', 'PROCESSING', 'READY', 'FAILED'))
);

CREATE INDEX idx_extraction_jobs_household_doc ON extraction_jobs(household_id, document_id);
