-- V24: Evrensel Şablon Motoru (Universal Template Engine)
-- Eski transaction_template yapısını genişletip, tüm modüllerde kullanılabilir hale getirir.

-- ============================================================
-- 1. YENİ TABLOLAR
-- ============================================================

CREATE TABLE template (
    template_id BIGSERIAL PRIMARY KEY,
    company_id BIGINT REFERENCES company(company_id),
    user_id BIGINT,
    template_code VARCHAR(20) UNIQUE,
    template_name VARCHAR(255) NOT NULL,
    template_type VARCHAR(30) NOT NULL,
    description VARCHAR(255),
    period VARCHAR(20),
    payload JSONB,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE template_usage_log (
    log_id BIGSERIAL PRIMARY KEY,
    template_id BIGINT REFERENCES template(template_id),
    user_id BIGINT,
    used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    target_entity_type VARCHAR(50),
    target_entity_id BIGINT
);

-- ============================================================
-- 2. İNDEKSLER
-- ============================================================

CREATE INDEX idx_template_company ON template(company_id);
CREATE INDEX idx_template_type ON template(template_type);
CREATE INDEX idx_template_code ON template(template_code);
CREATE INDEX idx_template_deleted ON template(is_deleted);
CREATE INDEX idx_usage_log_template ON template_usage_log(template_id);
CREATE INDEX idx_usage_log_used_at ON template_usage_log(used_at);

-- Şirket + aktif + kod benzersizliği
CREATE UNIQUE INDEX uq_template_code_company_active
    ON template(template_code, company_id)
    WHERE is_deleted = false;

-- Şirket + aktif + isim benzersizliği
CREATE UNIQUE INDEX uq_template_name_company_active
    ON template(template_name, company_id)
    WHERE is_deleted = false;

-- ============================================================
-- 3. ESKİ VERİLERİ MİGRATE ET
-- ============================================================

INSERT INTO template (
    company_id,
    user_id,
    template_code,
    template_name,
    template_type,
    description,
    period,
    is_deleted,
    deleted_at,
    created_at,
    updated_at,
    payload
)
SELECT
    company_id,
    user_id,
    'TPL-' || LPAD(id::text, 2, '0'),
    template_name,
    transaction_type,
    description,
    'aylık',
    is_deleted,
    deleted_at,
    created_at,
    updated_at,
    jsonb_build_object(
        'defaultAmount', default_amount,
        'category', category
    )
FROM transaction_template
WHERE NOT EXISTS (SELECT 1 FROM template WHERE template.company_id = transaction_template.company_id AND template.template_name = transaction_template.template_name);

-- ============================================================
-- 4. TRIGGER: updated_at OTOMATİK GÜNCELLEME
-- ============================================================

CREATE OR REPLACE FUNCTION update_template_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_template_updated_at
    BEFORE UPDATE ON template
    FOR EACH ROW
    EXECUTE FUNCTION update_template_updated_at();
