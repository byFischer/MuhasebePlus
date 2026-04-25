-- invoice_line_item.product_id icin eksik FK constraint ekleniyor
ALTER TABLE invoice_line_item
    ADD CONSTRAINT fk_invoice_line_item_product
    FOREIGN KEY (product_id) REFERENCES product(product_id);
