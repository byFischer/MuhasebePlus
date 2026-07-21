ALTER TABLE invoice
    ALTER COLUMN invoice_type TYPE VARCHAR(50) USING invoice_type::text,
    ALTER COLUMN payment_status TYPE VARCHAR(50) USING payment_status::text;
