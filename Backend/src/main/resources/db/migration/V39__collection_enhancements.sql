-- Collection enhancements: late fee config and collection promises
CREATE TABLE IF NOT EXISTS late_fee_config (
    config_id           BIGSERIAL PRIMARY KEY,
    company_id          BIGINT NOT NULL,
    monthly_rate        DECIMAL(5,2) NOT NULL DEFAULT 2.00,
    grace_period_days   INTEGER NOT NULL DEFAULT 7,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP,
    updated_at          TIMESTAMP,
    CONSTRAINT fk_latefee_company FOREIGN KEY (company_id) REFERENCES company(company_id)
);

CREATE TABLE IF NOT EXISTS collection_promise (
    promise_id      BIGSERIAL PRIMARY KEY,
    company_id      BIGINT NOT NULL,
    invoice_id      BIGINT NOT NULL,
    promised_date   DATE NOT NULL,
    promised_amount DECIMAL(15,2) NOT NULL,
    notes           VARCHAR(500),
    fulfilled       BOOLEAN NOT NULL DEFAULT FALSE,
    fulfilled_at    DATE,
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP,
    CONSTRAINT fk_promise_company FOREIGN KEY (company_id) REFERENCES company(company_id)
);

CREATE INDEX IF NOT EXISTS idx_promise_invoice ON collection_promise(invoice_id);
CREATE INDEX IF NOT EXISTS idx_promise_company ON collection_promise(company_id);
