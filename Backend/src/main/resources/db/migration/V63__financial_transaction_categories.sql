ALTER TABLE bank_account
    ADD COLUMN IF NOT EXISTS account_type VARCHAR(20) NOT NULL DEFAULT 'BANK';

UPDATE bank_account
SET account_type = 'BANK'
WHERE account_type IS NULL OR account_type = '';

ALTER TABLE transaction
    ADD COLUMN IF NOT EXISTS transaction_category VARCHAR(30) NOT NULL DEFAULT 'GENERAL',
    ADD COLUMN IF NOT EXISTS transfer_account_id BIGINT;

UPDATE transaction
SET transaction_category = 'GENERAL'
WHERE transaction_category IS NULL OR transaction_category = '';

CREATE INDEX IF NOT EXISTS idx_transaction_transfer_account ON transaction(transfer_account_id);
