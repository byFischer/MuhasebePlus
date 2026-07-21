INSERT INTO chart_of_account (company_id, account_code, account_name, account_type, is_leaf, is_system)
SELECT c.company_id, '590', 'Donem Net Kari', 'EQUITY', true, true
FROM company c
WHERE NOT EXISTS (
    SELECT 1
    FROM chart_of_account coa
    WHERE coa.company_id = c.company_id
      AND coa.account_code = '590'
      AND coa.is_deleted = false
);

INSERT INTO chart_of_account (company_id, account_code, account_name, account_type, is_leaf, is_system)
SELECT c.company_id, '591', 'Donem Net Zarari', 'EQUITY', true, true
FROM company c
WHERE NOT EXISTS (
    SELECT 1
    FROM chart_of_account coa
    WHERE coa.company_id = c.company_id
      AND coa.account_code = '591'
      AND coa.is_deleted = false
);
