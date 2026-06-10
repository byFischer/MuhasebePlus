-- Adds TDHP account 103 for issued cheques/payment orders to existing companies and the template.
INSERT INTO chart_of_account (company_id, account_code, account_name, account_type, is_leaf, is_system)
SELECT c.company_id, '103', 'Verilen Cekler ve Odeme Emirleri', 'ASSET', true, true
FROM company c
WHERE NOT EXISTS (
    SELECT 1
    FROM chart_of_account coa
    WHERE coa.company_id = c.company_id
      AND coa.account_code = '103'
      AND coa.is_deleted = false
);
