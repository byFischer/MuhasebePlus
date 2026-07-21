-- KDV Beyannamesi: VAT period summary and declaration lines
CREATE TABLE IF NOT EXISTS vat_period (
    id                   BIGSERIAL     PRIMARY KEY,
    company_id           BIGINT        NOT NULL REFERENCES company(company_id),
    year                 INTEGER       NOT NULL,
    month                INTEGER       NOT NULL CHECK (month BETWEEN 1 AND 12),
    status               VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    total_output_vat     NUMERIC(15,2) NOT NULL DEFAULT 0,
    total_input_vat      NUMERIC(15,2) NOT NULL DEFAULT 0,
    net_payable_vat      NUMERIC(15,2) NOT NULL DEFAULT 0,
    carried_forward_vat  NUMERIC(15,2) NOT NULL DEFAULT 0,
    created_at           TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_vat_period UNIQUE (company_id, year, month)
);

CREATE TABLE IF NOT EXISTS vat_period_line (
    id              BIGSERIAL     PRIMARY KEY,
    vat_period_id   BIGINT        NOT NULL REFERENCES vat_period(id) ON DELETE CASCADE,
    line_type       VARCHAR(10)   NOT NULL,
    vat_rate        INTEGER       NOT NULL,
    tax_base        NUMERIC(15,2) NOT NULL DEFAULT 0,
    vat_amount      NUMERIC(15,2) NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_vat_period_company     ON vat_period(company_id);
CREATE INDEX IF NOT EXISTS idx_vat_period_year_month  ON vat_period(company_id, year, month);
CREATE INDEX IF NOT EXISTS idx_vat_period_line        ON vat_period_line(vat_period_id);
