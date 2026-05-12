-- E-Fatura altyapı tabloları
CREATE TABLE IF NOT EXISTS einvoice_profile (
    profile_id          BIGSERIAL PRIMARY KEY,
    company_id          BIGINT NOT NULL,
    gib_alias           VARCHAR(100) NOT NULL,
    gib_password        VARCHAR(255) NOT NULL,
    certificate_path    VARCHAR(500),
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    default_series_code VARCHAR(10),
    created_at          TIMESTAMP,
    updated_at          TIMESTAMP,
    CONSTRAINT fk_einvoice_company FOREIGN KEY (company_id) REFERENCES company(company_id)
);

CREATE TABLE IF NOT EXISTS einvoice_log (
    log_id          BIGSERIAL PRIMARY KEY,
    company_id      BIGINT NOT NULL,
    invoice_id      BIGINT NOT NULL,
    uuid            VARCHAR(100),
    envelope_id     VARCHAR(100),
    status          VARCHAR(50),
    sent_at         TIMESTAMP,
    request_body    TEXT,
    response_body   TEXT,
    error_message   TEXT,
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP,
    CONSTRAINT fk_einvoicelog_company FOREIGN KEY (company_id) REFERENCES company(company_id)
);

CREATE INDEX IF NOT EXISTS idx_einvoice_log_invoice ON einvoice_log(invoice_id);
CREATE INDEX IF NOT EXISTS idx_einvoice_log_status ON einvoice_log(status);
