-- invoice_vat_summary: Fatura başına KDV oran kırılımı
CREATE TABLE invoice_vat_summary (
    summary_id               BIGSERIAL PRIMARY KEY,
    company_id               BIGINT         NOT NULL REFERENCES company(company_id),
    invoice_id               BIGINT         NOT NULL REFERENCES invoice(invoice_id),
    vat_rate                 NUMERIC(5,2)   NOT NULL,
    vat_base_amount          NUMERIC(15,2)  NOT NULL,
    vat_amount               NUMERIC(15,2)  NOT NULL,
    withholding_base_amount  NUMERIC(15,2),
    withholding_amount       NUMERIC(15,2),
    withholding_tax_code_id  INTEGER REFERENCES withholding_tax_code(code_id),
    exemption_amount         NUMERIC(15,2),
    vat_exemption_code_id    INTEGER REFERENCES vat_exemption_code(code_id),
    created_at               TIMESTAMP,
    updated_at               TIMESTAMP,
    CONSTRAINT uq_vat_summary_invoice_rate UNIQUE (invoice_id, vat_rate)
);

CREATE INDEX idx_vat_summary_company ON invoice_vat_summary(company_id);
CREATE INDEX idx_vat_summary_invoice ON invoice_vat_summary(invoice_id);

-- InvoiceLineItem: satır bazlı KDV tutarları + kod FK'ları
ALTER TABLE invoice_line_item
    ADD COLUMN IF NOT EXISTS vat_base_amount        NUMERIC(15,2),
    ADD COLUMN IF NOT EXISTS vat_amount             NUMERIC(15,2),
    ADD COLUMN IF NOT EXISTS withholding_tax_code_id INTEGER REFERENCES withholding_tax_code(code_id),
    ADD COLUMN IF NOT EXISTS vat_exemption_code_id   INTEGER REFERENCES vat_exemption_code(code_id),
    ADD COLUMN IF NOT EXISTS vat_exemption_reason    VARCHAR(500);
