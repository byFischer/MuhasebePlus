-- Journal Entry (Yevmiye Defteri)
CREATE TABLE IF NOT EXISTS journal_entry (
    entry_id        BIGSERIAL PRIMARY KEY,
    company_id      BIGINT        NOT NULL REFERENCES company(company_id),
    entry_number    VARCHAR(30)   NOT NULL,
    entry_date      DATE          NOT NULL,
    description     VARCHAR(500),
    source_type     VARCHAR(20)   NOT NULL DEFAULT 'MANUAL',
    source_id       BIGINT,
    is_reversed     BOOLEAN       NOT NULL DEFAULT FALSE,
    reversed_entry_id BIGINT      REFERENCES journal_entry(entry_id),
    is_deleted      BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_entry_number UNIQUE (company_id, entry_number),
    -- Aynı kaynak için iki fiş kesilemez (idempotency)
    CONSTRAINT uq_entry_source UNIQUE (company_id, source_type, source_id)
);

CREATE INDEX IF NOT EXISTS idx_je_company    ON journal_entry(company_id);
CREATE INDEX IF NOT EXISTS idx_je_date       ON journal_entry(company_id, entry_date);
CREATE INDEX IF NOT EXISTS idx_je_source     ON journal_entry(company_id, source_type, source_id);

-- Journal Entry Line (Fiş Satırları)
CREATE TABLE IF NOT EXISTS journal_entry_line (
    line_id         BIGSERIAL PRIMARY KEY,
    company_id      BIGINT        NOT NULL REFERENCES company(company_id),
    entry_id        BIGINT        NOT NULL REFERENCES journal_entry(entry_id) ON DELETE CASCADE,
    account_id      BIGINT        NOT NULL REFERENCES chart_of_account(account_id),
    debit_amount    NUMERIC(15,2) NOT NULL DEFAULT 0,
    credit_amount   NUMERIC(15,2) NOT NULL DEFAULT 0,
    description     VARCHAR(500),
    line_order      INTEGER       NOT NULL DEFAULT 0,
    CONSTRAINT chk_debit_or_credit CHECK (
        (debit_amount > 0 AND credit_amount = 0) OR
        (credit_amount > 0 AND debit_amount = 0)
    )
);

CREATE INDEX IF NOT EXISTS idx_jel_entry   ON journal_entry_line(entry_id);
CREATE INDEX IF NOT EXISTS idx_jel_account ON journal_entry_line(account_id);
CREATE INDEX IF NOT EXISTS idx_jel_company ON journal_entry_line(company_id);

-- Sequence counter per company (FIS-2025-0001)
CREATE TABLE IF NOT EXISTS journal_entry_sequence (
    company_id  BIGINT  NOT NULL REFERENCES company(company_id),
    year        INTEGER NOT NULL,
    last_seq    INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (company_id, year)
);
