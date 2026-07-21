-- invoice_payment tablosu
CREATE TABLE IF NOT EXISTS invoice_payment (
    payment_id      BIGSERIAL PRIMARY KEY,
    company_id      BIGINT NOT NULL REFERENCES company(company_id),
    invoice_id      BIGINT NOT NULL REFERENCES invoice(invoice_id),
    amount          NUMERIC(15,2) NOT NULL,
    payment_date    DATE NOT NULL,
    payment_method  VARCHAR(20) NOT NULL,
    bank_account_id BIGINT NOT NULL REFERENCES bank_account(account_id),
    transaction_id  BIGINT REFERENCES "transaction"(transaction_id),
    notes           VARCHAR(500),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_invoice_payment_invoice ON invoice_payment(invoice_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_invoice_payment_company ON invoice_payment(company_id) WHERE is_deleted = false;

-- Default "Nakit Kasa" hesabı her şirket için
INSERT INTO bank_account (company_id, bank_name, iban, currency, created_at, updated_at, is_deleted)
SELECT c.company_id, 'Nakit Kasa', 'CASH-' || c.company_id, 'TRY', NOW(), NOW(), false
FROM company c
WHERE NOT EXISTS (
    SELECT 1 FROM bank_account ba
    WHERE ba.company_id = c.company_id AND ba.bank_name = 'Nakit Kasa' AND ba.is_deleted = false
);
