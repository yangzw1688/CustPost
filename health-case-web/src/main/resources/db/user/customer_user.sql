-- Successful customer logins only. Administrators have separate accounts.
CREATE TABLE IF NOT EXISTS customer_user (
    phone VARCHAR(11) NOT NULL PRIMARY KEY,
    first_login_at DATETIME NOT NULL,
    last_login_at DATETIME NOT NULL,
    login_count BIGINT NOT NULL DEFAULT 1,
    INDEX idx_customer_user_login (last_login_at, phone)
);
