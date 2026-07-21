ALTER TABLE "user"
    ALTER COLUMN user_role TYPE VARCHAR(50) USING user_role::text;

ALTER TABLE invoice
    DROP CONSTRAINT IF EXISTS invoice_customer_id_fkey;
