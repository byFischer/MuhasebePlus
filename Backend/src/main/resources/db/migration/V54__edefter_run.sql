CREATE TABLE edefter_run (
    run_id               BIGSERIAL PRIMARY KEY,
    company_id           BIGINT      NOT NULL REFERENCES company(company_id),
    year                 INTEGER     NOT NULL,
    month                INTEGER     NOT NULL,
    journal_xml          TEXT,
    ledger_xml           TEXT,
    journal_entry_count  INTEGER,
    journal_line_count   INTEGER,
    status               VARCHAR(20) NOT NULL DEFAULT 'GENERATED',
    generated_at         TIMESTAMP,
    is_deleted           BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at           TIMESTAMP,
    created_at           TIMESTAMP,
    updated_at           TIMESTAMP,
    CONSTRAINT uq_edefter_run UNIQUE (company_id, year, month)
);

CREATE INDEX idx_edefter_run_company ON edefter_run(company_id);
