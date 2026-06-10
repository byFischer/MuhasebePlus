-- Bank reconciliation: imported statements and match tracking
CREATE TABLE IF NOT EXISTS bank_statement (
    id               BIGSERIAL     PRIMARY KEY,
    company_id       BIGINT        NOT NULL REFERENCES company(company_id),
    bank_account_id  BIGINT        NOT NULL REFERENCES bank_account(account_id),
    statement_date   DATE,
    opening_balance  NUMERIC(15,2) NOT NULL DEFAULT 0,
    closing_balance  NUMERIC(15,2) NOT NULL DEFAULT 0,
    import_date      TIMESTAMP     NOT NULL DEFAULT NOW(),
    status           VARCHAR(20)   NOT NULL DEFAULT 'IN_PROGRESS',
    created_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS bank_statement_line (
    id                  BIGSERIAL     PRIMARY KEY,
    bank_statement_id   BIGINT        NOT NULL REFERENCES bank_statement(id) ON DELETE CASCADE,
    transaction_date    DATE          NOT NULL,
    description         VARCHAR(500),
    amount              NUMERIC(15,2) NOT NULL,
    balance             NUMERIC(15,2),
    is_matched          BOOLEAN       NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS reconciliation_match (
    id                  BIGSERIAL   PRIMARY KEY,
    company_id          BIGINT      NOT NULL,
    statement_line_id   BIGINT      NOT NULL REFERENCES bank_statement_line(id) ON DELETE CASCADE,
    transaction_id      BIGINT      REFERENCES transaction(transaction_id),
    match_type          VARCHAR(10) NOT NULL DEFAULT 'MANUAL',
    matched_at          TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_bank_stmt_company     ON bank_statement(company_id);
CREATE INDEX IF NOT EXISTS idx_bank_stmt_account     ON bank_statement(bank_account_id);
CREATE INDEX IF NOT EXISTS idx_bank_stmt_line_stmt   ON bank_statement_line(bank_statement_id);
CREATE INDEX IF NOT EXISTS idx_bank_stmt_line_match  ON bank_statement_line(is_matched);
CREATE INDEX IF NOT EXISTS idx_recon_match_line      ON reconciliation_match(statement_line_id);
