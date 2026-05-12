-- Enhanced customer fields for accounting-grade cari kart
ALTER TABLE customer ADD COLUMN IF NOT EXISTS account_code VARCHAR(20);
ALTER TABLE customer ADD COLUMN IF NOT EXISTS opening_balance DECIMAL(15,2) DEFAULT 0;
ALTER TABLE customer ADD COLUMN IF NOT EXISTS opening_balance_date DATE;
ALTER TABLE customer ADD COLUMN IF NOT EXISTS tax_office VARCHAR(100);
ALTER TABLE customer ADD COLUMN IF NOT EXISTS identity_number VARCHAR(11);
ALTER TABLE customer ADD COLUMN IF NOT EXISTS iban VARCHAR(34);
ALTER TABLE customer ADD COLUMN IF NOT EXISTS currency VARCHAR(3) DEFAULT 'TRY';
ALTER TABLE customer ADD COLUMN IF NOT EXISTS credit_limit DECIMAL(15,2);
ALTER TABLE customer ADD COLUMN IF NOT EXISTS customer_role VARCHAR(10) DEFAULT 'BOTH';

-- Unique account code per company
CREATE UNIQUE INDEX IF NOT EXISTS idx_customer_account_code_company ON customer(company_id, account_code) WHERE is_deleted = false;
