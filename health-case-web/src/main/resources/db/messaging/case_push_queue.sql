CREATE TABLE IF NOT EXISTS case_push_task (
    case_id BIGINT NOT NULL,
    task_id CHAR(36) NOT NULL PRIMARY KEY,
    task_order BIGINT NOT NULL AUTO_INCREMENT UNIQUE,
    trigger_type VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    case_status VARCHAR(20),
    status VARCHAR(20) NOT NULL,
    attempts INT NOT NULL,
    next_attempt_at DATETIME NOT NULL,
    dispatch_after DATETIME,
    receiver_fingerprint VARCHAR(64),
    lease_until DATETIME,
    lease_token CHAR(36),
    updated_at DATETIME NOT NULL,
    last_error VARCHAR(200),
    INDEX idx_case_push_case_order (case_id, task_order),
    INDEX idx_case_push_due (status, next_attempt_at),
    INDEX idx_case_push_lease (status, lease_until)
);

CREATE TABLE IF NOT EXISTS case_push_receipt (
    task_id CHAR(36) NOT NULL PRIMARY KEY,
    case_id BIGINT NOT NULL,
    received_at DATETIME NOT NULL,
    INDEX idx_case_push_receipt_case (case_id)
);
