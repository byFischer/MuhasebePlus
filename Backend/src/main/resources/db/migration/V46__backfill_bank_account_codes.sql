-- Mevcut banka hesapları için hesap kodu yoksa 102.00.{seq} ata
-- Bu migration idempotent: account_code dolu olanları atlar.

ALTER TABLE bank_account ADD COLUMN IF NOT EXISTS account_code VARCHAR(20);

DO $$
DECLARE
    rec RECORD;
    seq INTEGER := 1;
BEGIN
    FOR rec IN
        SELECT account_id, company_id
        FROM bank_account
        WHERE (account_code IS NULL OR account_code = '')
          AND is_deleted = false
        ORDER BY company_id, account_id
    LOOP
        SELECT COALESCE(MAX(CAST(SPLIT_PART(account_code, '.', 3) AS INTEGER)), 0) + 1
          INTO seq
          FROM bank_account
         WHERE company_id = rec.company_id
           AND account_code LIKE '102.00.%'
           AND account_code ~ '^[0-9]+\.[0-9]+\.[0-9]+$';

        UPDATE bank_account
           SET account_code = '102.00.' || LPAD(seq::TEXT, 3, '0')
         WHERE account_id = rec.account_id;
    END LOOP;
END $$;
