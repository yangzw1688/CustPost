CREATE TABLE IF NOT EXISTS insurance_company (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    short_name VARCHAR(80) NOT NULL DEFAULT '',
    contact_name VARCHAR(60) NOT NULL DEFAULT '',
    contact_phone VARCHAR(32) NOT NULL DEFAULT '',
    address VARCHAR(240) NOT NULL DEFAULT '',
    remarks VARCHAR(1000) NOT NULL DEFAULT '',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    active_environment VARCHAR(8) NOT NULL DEFAULT 'TEST',
    version BIGINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS insurer_connection (
    company_id BIGINT NOT NULL,
    environment VARCHAR(8) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'NOT_CONFIGURED',
    endpoint VARCHAR(512) NOT NULL DEFAULT '',
    auth_type VARCHAR(16) NOT NULL DEFAULT 'NONE',
    token_ciphertext VARCHAR(2048) NOT NULL DEFAULT '',
    version BIGINT NOT NULL DEFAULT 1,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(company_id, environment)
);
CREATE TABLE IF NOT EXISTS service_project (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    insurer_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    owner_name VARCHAR(60) NOT NULL DEFAULT '',
    owner_phone VARCHAR(32) NOT NULL DEFAULT '',
    start_date DATE,
    end_date DATE,
    description VARCHAR(2000) NOT NULL DEFAULT '',
    document_requirements VARCHAR(2000) NOT NULL DEFAULT '',
    version BIGINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_project_insurer(insurer_id, id)
);
-- Existing installations also need service_policy.project_id (nullable BIGINT).
-- The startup initializer adds it and its index only when absent.
