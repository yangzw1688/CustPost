CREATE TABLE IF NOT EXISTS case_submission (
    phone VARCHAR(11) NOT NULL,
    request_id CHAR(32) NOT NULL,
    fingerprint CHAR(64) NOT NULL,
    case_id BIGINT,
    PRIMARY KEY (phone, request_id)
);
