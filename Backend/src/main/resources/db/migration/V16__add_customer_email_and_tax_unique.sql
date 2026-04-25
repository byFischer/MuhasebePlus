-- Musteri email kolonu ekleniyor
ALTER TABLE customer ADD COLUMN IF NOT EXISTS email VARCHAR(255);

-- Sirket bazinda ayni vergi numarasina sahip aktif musteri olamaz
-- Soft-deleted kayitlari kapsamayan partial unique index
CREATE UNIQUE INDEX IF NOT EXISTS uq_customer_taxnumber_company_active
    ON customer(tax_number, company_id)
    WHERE is_deleted = false;
