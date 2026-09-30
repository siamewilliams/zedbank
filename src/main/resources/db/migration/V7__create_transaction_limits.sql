CREATE TABLE transaction_limits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    limit_date DATE NOT NULL,
    total_debited DECIMAL(19,4) NOT NULL DEFAULT 0,
    transaction_count INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_acc_date (account_id, limit_date),
    FOREIGN KEY (account_id) REFERENCES accounts(id)
);