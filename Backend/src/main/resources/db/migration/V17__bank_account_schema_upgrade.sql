-- bank_account: PG enum currency_type -> VARCHAR(10)
ALTER TABLE bank_account
    ALTER COLUMN currency TYPE VARCHAR(10) USING currency::text;

-- Audit ve soft-delete kolonlari
ALTER TABLE bank_account ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE bank_account ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE bank_account ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE bank_account ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- Sirket bazinda ayni IBAN birden fazla aktif hesapta olamaz (soft-deleted kayitlari kapsamaz)
CREATE UNIQUE INDEX IF NOT EXISTS uq_bank_account_iban_company_active
    ON bank_account(iban, company_id)
    WHERE is_deleted = false;
