-- Cheque / Promissory Note Portfolio
CREATE TABLE IF NOT EXISTS cheque (
    cheque_id               BIGSERIAL PRIMARY KEY,
    company_id              BIGINT          NOT NULL REFERENCES company(company_id),
    cheque_number           VARCHAR(50)     NOT NULL,
    cheque_type             VARCHAR(20)     NOT NULL,  -- RECEIVABLE | PAYABLE
    cheque_kind             VARCHAR(20)     NOT NULL,  -- CHEQUE | PROMISSORY_NOTE
    customer_id             BIGINT,
    drawer_bank             VARCHAR(100),
    drawer_branch           VARCHAR(100),
    drawer_account_number   VARCHAR(50),
    amount                  NUMERIC(15,2)   NOT NULL,
    currency                VARCHAR(10)     NOT NULL DEFAULT 'TRY',
    issue_date              DATE            NOT NULL,
    due_date                DATE            NOT NULL,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'IN_PORTFOLIO',
    bank_account_id         BIGINT,
    transaction_id          BIGINT,
    invoice_payment_id      BIGINT,
    endorsed_to_customer_id BIGINT,
    notes                   VARCHAR(500),
    is_deleted              BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at              TIMESTAMP,
    created_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_cheque_number UNIQUE (company_id, cheque_number)
);

CREATE INDEX IF NOT EXISTS idx_cheque_company        ON cheque(company_id);
CREATE INDEX IF NOT EXISTS idx_cheque_status         ON cheque(company_id, status);
CREATE INDEX IF NOT EXISTS idx_cheque_due_date       ON cheque(company_id, due_date);
CREATE INDEX IF NOT EXISTS idx_cheque_customer       ON cheque(customer_id);

-- Cheque movement ledger (status change audit trail)
CREATE TABLE IF NOT EXISTS cheque_movement (
    movement_id     BIGSERIAL PRIMARY KEY,
    company_id      BIGINT          NOT NULL REFERENCES company(company_id),
    cheque_id       BIGINT          NOT NULL REFERENCES cheque(cheque_id),
    movement_type   VARCHAR(30)     NOT NULL,
    source_type     VARCHAR(20)     NOT NULL DEFAULT 'MANUAL',
    source_id       BIGINT,
    movement_date   DATE            NOT NULL,
    bank_account_id BIGINT,
    transaction_id  BIGINT,
    reason          VARCHAR(500),
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cheque_movement_cheque  ON cheque_movement(cheque_id);
CREATE INDEX IF NOT EXISTS idx_cheque_movement_company ON cheque_movement(company_id);
