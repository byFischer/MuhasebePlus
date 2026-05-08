-- stock_movement tablosu
CREATE TABLE IF NOT EXISTS stock_movement (
    movement_id        BIGSERIAL PRIMARY KEY,
    company_id         BIGINT NOT NULL REFERENCES company(company_id),
    product_id         INTEGER NOT NULL REFERENCES product(product_id),
    quantity           INTEGER NOT NULL,
    movement_type      VARCHAR(30) NOT NULL,
    source_type        VARCHAR(20) NOT NULL,
    source_id          BIGINT,
    unit_cost          NUMERIC(15,2),
    reason             VARCHAR(255),
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at         TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_stock_movement_product ON stock_movement(product_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_stock_movement_company ON stock_movement(company_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_stock_movement_source  ON stock_movement(source_type, source_id);
CREATE INDEX IF NOT EXISTS idx_stock_movement_type    ON stock_movement(movement_type);

-- Mevcut stok değerlerini OPENING_BALANCE olarak ledger'a aktar
INSERT INTO stock_movement (company_id, product_id, quantity, movement_type, source_type, reason, created_at, updated_at, is_deleted)
SELECT s.company_id, s.product_id, s.quantity, 'OPENING_BALANCE', 'INITIAL',
       'Migration V29 - Mevcut stok devri', NOW(), NOW(), false
FROM stock s
WHERE s.is_deleted = false AND s.quantity IS NOT NULL AND s.quantity > 0;
