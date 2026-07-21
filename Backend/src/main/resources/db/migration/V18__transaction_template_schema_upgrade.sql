-- V12 multi-tenant migration'i transaction_template'i atlamis, burada ekliyoruz
ALTER TABLE transaction_template ADD COLUMN IF NOT EXISTS company_id BIGINT REFERENCES company(company_id);
CREATE INDEX IF NOT EXISTS idx_transaction_template_company ON transaction_template(company_id);

-- transaction_template: V1'de eksik olan kolonlar ekleniyor
ALTER TABLE transaction_template ADD COLUMN IF NOT EXISTS transaction_type VARCHAR(20);
ALTER TABLE transaction_template ADD COLUMN IF NOT EXISTS description VARCHAR(255);
ALTER TABLE transaction_template ADD COLUMN IF NOT EXISTS category VARCHAR(100);

-- Audit ve soft-delete kolonlari
ALTER TABLE transaction_template ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE transaction_template ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE transaction_template ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE transaction_template ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- Sirket bazinda ayni isimde aktif sablon olamaz (soft-deleted kayitlari kapsamaz)
CREATE UNIQUE INDEX IF NOT EXISTS uq_transaction_template_name_company_active
    ON transaction_template(template_name, company_id)
    WHERE is_deleted = false;
