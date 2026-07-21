-- Beyanname ve e-Defter için mükellef bilgileri
ALTER TABLE company
    ADD COLUMN IF NOT EXISTS tax_office_code         VARCHAR(10),
    ADD COLUMN IF NOT EXISTS trade_registry_no       VARCHAR(30),
    ADD COLUMN IF NOT EXISTS mersis_no               VARCHAR(20),
    ADD COLUMN IF NOT EXISTS nace                    VARCHAR(10),
    ADD COLUMN IF NOT EXISTS declaration_period_type VARCHAR(20) DEFAULT 'MONTHLY',
    ADD COLUMN IF NOT EXISTS corporate_tax_type      VARCHAR(30) DEFAULT 'GELIR_VERGISI';
