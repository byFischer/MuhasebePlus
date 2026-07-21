CREATE TABLE IF NOT EXISTS budget (
    budget_id        BIGSERIAL PRIMARY KEY,
    company_id       BIGINT REFERENCES company(company_id),
    year             INTEGER      NOT NULL,
    month            INTEGER      NOT NULL,
    category         VARCHAR(100) NOT NULL,
    transaction_type VARCHAR(20)  NOT NULL,
    planned_amount   NUMERIC(15,2) NOT NULL,
    notes            VARCHAR(500),
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted       BOOLEAN   NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_budget_company ON budget(company_id, is_deleted);
