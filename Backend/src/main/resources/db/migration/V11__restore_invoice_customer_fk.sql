ALTER TABLE invoice
    ADD CONSTRAINT fk_invoice_customer
    FOREIGN KEY (customer_id) REFERENCES customer(customer_id);
    