CREATE TABLE IF NOT EXISTS case_payment_review (
    case_id BIGINT NOT NULL PRIMARY KEY,
    decision VARCHAR(12) NOT NULL,
    payout_amount DECIMAL(12, 2),
    review_remark VARCHAR(200) NOT NULL,
    reviewer_phone VARCHAR(20) NOT NULL,
    reviewed_at DATETIME NOT NULL,
    payment_confirmed_by VARCHAR(20),
    payment_confirmed_at DATETIME
);
