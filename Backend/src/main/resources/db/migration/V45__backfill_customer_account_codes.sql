-- Mevcut müşteriler için hesap kodu yoksa varsayılan ata
-- Satış müşterisi → 120.00.{seq}, Alım müşterisi → 320.00.{seq}
-- Bu migration idempotent: account_code dolu olanları atlar.

DO $$
DECLARE
    rec RECORD;
    seq INTEGER := 1;
    prefix VARCHAR;
BEGIN
    FOR rec IN
        SELECT customer_id, company_id,
               CASE WHEN customer_role = 'SELLER' THEN '320' ELSE '120' END AS grp
        FROM customer
        WHERE (account_code IS NULL OR account_code = '')
          AND is_deleted = false
        ORDER BY company_id, customer_id
    LOOP
        prefix := rec.grp;
        -- Şirket + grup bazında sıra sayacı
        SELECT COALESCE(MAX(CAST(SPLIT_PART(account_code, '.', 3) AS INTEGER)), 0) + 1
          INTO seq
          FROM customer
         WHERE company_id = rec.company_id
           AND account_code LIKE prefix || '.00.%'
           AND account_code ~ '^[0-9]+\.[0-9]+\.[0-9]+$';

        UPDATE customer
           SET account_code = prefix || '.00.' || LPAD(seq::TEXT, 3, '0')
         WHERE customer_id = rec.customer_id;
    END LOOP;
END $$;
