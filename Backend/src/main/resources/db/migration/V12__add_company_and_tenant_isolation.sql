-- ============================================================
-- Company tablosu
-- ============================================================
CREATE TABLE IF NOT EXISTS company (
    company_id   BIGSERIAL    PRIMARY KEY,
    company_name VARCHAR(255) NOT NULL,
    tax_office   VARCHAR(255),
    tax_number   VARCHAR(20)  UNIQUE,
    address      VARCHAR(255),
    city         VARCHAR(100),
    phone        VARCHAR(20),
    email        VARCHAR(255),
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- Tüm veri tablolarına company_id ekle
-- ============================================================

ALTER TABLE "user"               ADD COLUMN IF NOT EXISTS company_id BIGINT REFERENCES company(company_id);
ALTER TABLE customer             ADD COLUMN IF NOT EXISTS company_id BIGINT REFERENCES company(company_id);
ALTER TABLE product              ADD COLUMN IF NOT EXISTS company_id BIGINT REFERENCES company(company_id);
ALTER TABLE stock                ADD COLUMN IF NOT EXISTS company_id BIGINT REFERENCES company(company_id);
ALTER TABLE invoice              ADD COLUMN IF NOT EXISTS company_id BIGINT REFERENCES company(company_id);
ALTER TABLE invoice_line_item    ADD COLUMN IF NOT EXISTS company_id BIGINT REFERENCES company(company_id);
ALTER TABLE customer_note        ADD COLUMN IF NOT EXISTS company_id BIGINT REFERENCES company(company_id);
ALTER TABLE bank_account         ADD COLUMN IF NOT EXISTS company_id BIGINT REFERENCES company(company_id);
ALTER TABLE transaction          ADD COLUMN IF NOT EXISTS company_id BIGINT REFERENCES company(company_id);
ALTER TABLE report               ADD COLUMN IF NOT EXISTS company_id BIGINT REFERENCES company(company_id);
ALTER TABLE notification         ADD COLUMN IF NOT EXISTS company_id BIGINT REFERENCES company(company_id);
ALTER TABLE schedule_task        ADD COLUMN IF NOT EXISTS company_id BIGINT REFERENCES company(company_id);
ALTER TABLE dashboard_preference ADD COLUMN IF NOT EXISTS company_id BIGINT REFERENCES company(company_id);
ALTER TABLE system_log           ADD COLUMN IF NOT EXISTS company_id BIGINT REFERENCES company(company_id);

-- ============================================================
-- Performans index'leri (sık sorgulanan tablolar)
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_user_company     ON "user"(company_id);
CREATE INDEX IF NOT EXISTS idx_customer_company ON customer(company_id);
CREATE INDEX IF NOT EXISTS idx_product_company  ON product(company_id);
CREATE INDEX IF NOT EXISTS idx_stock_company    ON stock(company_id);
CREATE INDEX IF NOT EXISTS idx_invoice_company  ON invoice(company_id);