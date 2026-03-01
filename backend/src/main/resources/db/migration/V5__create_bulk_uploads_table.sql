CREATE TABLE bulk_uploads (
  id BIGSERIAL PRIMARY KEY,
  entity_type VARCHAR(50) NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  row_count INTEGER NOT NULL DEFAULT 0,
  valid_count INTEGER NOT NULL DEFAULT 0,
  error_count INTEGER NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'STAGED',
  summary JSONB,
  uploaded_by BIGINT REFERENCES users(id),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE bulk_upload_rows (
  id BIGSERIAL PRIMARY KEY,
  bulk_upload_id BIGINT NOT NULL REFERENCES bulk_uploads(id) ON DELETE CASCADE,
  row_number INTEGER NOT NULL,
  data JSONB NOT NULL,
  is_valid BOOLEAN NOT NULL DEFAULT TRUE,
  error_message TEXT
);
CREATE INDEX idx_bulk_upload_rows_upload ON bulk_upload_rows(bulk_upload_id);
