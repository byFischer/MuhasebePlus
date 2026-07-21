-- Accounting period table for closed period protection
CREATE TABLE IF NOT EXISTS accounting_period (
    period_id      BIGSERIAL PRIMARY KEY,
    company_id     BIGINT        NOT NULL REFERENCES company(company_id),
    year           INTEGER       NOT NULL,
    month          INTEGER       NOT NULL CHECK (month BETWEEN 1 AND 12),
    status         VARCHAR(10)   NOT NULL DEFAULT 'OPEN',
    closed_at      TIMESTAMP,
    closed_by      BIGINT,
    reopened_at    TIMESTAMP,
    reopened_by    BIGINT,
    reopen_reason  VARCHAR(500),
    created_at     TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_accounting_period UNIQUE (company_id, year, month)
);

CREATE INDEX IF NOT EXISTS idx_accounting_period_company ON accounting_period(company_id);
CREATE INDEX IF NOT EXISTS idx_accounting_period_status  ON accounting_period(company_id, status);
