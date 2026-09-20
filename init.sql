CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE documents (
                           id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                           title VARCHAR(255) NOT NULL,
                           original_filename VARCHAR(512) NOT NULL,
                           content_type VARCHAR(127) NOT NULL,
                           file_size BIGINT NOT NULL DEFAULT 0,

                           created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_documents_created_at ON documents(created_at);