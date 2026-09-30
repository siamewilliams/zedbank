CREATE TABLE idempotency_keys (
    idempotency_key VARCHAR(80) PRIMARY KEY,
    request_hash VARCHAR(128) NOT NULL,
    response_body TEXT,
    response_status INT,
    created_at DATETIME NOT NULL,
    expires_at DATETIME NOT NULL,
    INDEX idx_idem_expires (expires_at)
);