-- transaction_type PG enum -> VARCHAR(20)
ALTER TABLE transaction
    ALTER COLUMN transaction_type TYPE VARCHAR(20) USING transaction_type::text;

-- transaction_date kolonu V1'de yok, ekleniyor
ALTER TABLE transaction ADD COLUMN IF NOT EXISTS transaction_date DATE DEFAULT CURRENT_DATE;
UPDATE transaction SET transaction_date = CURRENT_DATE WHERE transaction_date IS NULL;
ALTER TABLE transaction ALTER COLUMN transaction_date SET NOT NULL;

-- created_at V1'de yok
ALTER TABLE transaction ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Soft-delete kolonlari
ALTER TABLE transaction ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE transaction ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- Hesap bakiye sorgulari (GROUP BY account_id) icin index
CREATE INDEX IF NOT EXISTS idx_transaction_account ON transaction(account_id);
CREATE INDEX IF NOT EXISTS idx_transaction_company ON transaction(company_id);
