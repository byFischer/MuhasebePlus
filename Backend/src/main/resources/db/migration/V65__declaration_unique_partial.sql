-- Beyanname tekrar üretimi: soft-delete edilen (is_deleted = true) eski kayıtlar
-- unique key'i işgal ettiği için aynı dönem beyannamesi tekrar üretilemiyordu
-- (hem yeniden üretimde hem sildikten sonra). Unique CONSTRAINT'leri kaldırıp
-- yalnızca aktif (is_deleted = false) satırlar için partial unique index'lerle değiştir.

ALTER TABLE vat_declaration DROP CONSTRAINT IF EXISTS uq_vat_declaration_company_type_period;
CREATE UNIQUE INDEX uq_vat_declaration_company_type_period
    ON vat_declaration (company_id, declaration_type, year, month)
    WHERE is_deleted = false;

ALTER TABLE withholding_declaration DROP CONSTRAINT IF EXISTS uq_withholding_declaration;
CREATE UNIQUE INDEX uq_withholding_declaration
    ON withholding_declaration (company_id, year, month)
    WHERE is_deleted = false;

ALTER TABLE generic_declaration DROP CONSTRAINT IF EXISTS uq_generic_declaration;
CREATE UNIQUE INDEX uq_generic_declaration
    ON generic_declaration (company_id, declaration_type, year, period)
    WHERE is_deleted = false;
