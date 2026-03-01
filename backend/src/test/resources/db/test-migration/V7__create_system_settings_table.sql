CREATE TABLE system_settings (
    setting_key   VARCHAR(100) PRIMARY KEY,
    setting_value TEXT,
    is_secret     BOOLEAN NOT NULL DEFAULT FALSE,
    description   VARCHAR(255),
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by    VARCHAR(100)
);

INSERT INTO system_settings (setting_key, setting_value, is_secret, description)
VALUES
    ('google.api.key', NULL, TRUE,  'Google Custom Search API Key'),
    ('google.api.cx',  NULL, FALSE, 'Google Custom Search Engine ID (CX)'),
    ('validation.mode', 'free', FALSE, 'Validation mode: free | paid');
