-- invoice_line_item tablosu: Fatura kalem detaylari (FAT.1)
-- Not: payment_status kolonu V5 ile VARCHAR'a cevrildi; 'draft' degeri Java enum'una eklendi,
-- DB tarafinda ek ALTER gerekmez.

-- Invoice tablosuna audit kolonlari (BaseEntity ile uyum icin)
ALTER TABLE invoice
    ADD COLUMN created_at TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMP;

UPDATE invoice SET created_at = NOW(), updated_at = NOW() WHERE created_at IS NULL;

CREATE TABLE invoice_line_item (
    line_item_id  SERIAL        PRIMARY KEY,
    invoice_id    BIGINT        NOT NULL REFERENCES invoice(invoice_id) ON DELETE CASCADE,
    product_id    INT           NOT NULL,
    quantity      INT           NOT NULL CHECK (quantity > 0),
    unit_price    DECIMAL(15,2) NOT NULL CHECK (unit_price >= 0),
    vat_rate      DECIMAL(5,2)  NOT NULL DEFAULT 0 CHECK (vat_rate >= 0 AND vat_rate <= 100),
    line_total    DECIMAL(15,2) NOT NULL CHECK (line_total >= 0),
    is_deleted    BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP
);

CREATE INDEX idx_invoice_line_item_invoice ON invoice_line_item(invoice_id);
CREATE INDEX idx_invoice_line_item_product ON invoice_line_item(product_id);
