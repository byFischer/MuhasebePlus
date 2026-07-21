ALTER TABLE customer_note
    ALTER COLUMN note_id TYPE BIGINT;

ALTER TABLE customer_note
    ALTER COLUMN customer_id TYPE BIGINT;

ALTER SEQUENCE customer_note_note_id_seq AS BIGINT;

ALTER TABLE customer_note
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;