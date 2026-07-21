ALTER TABLE product
    ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE, 
    ADD COLUMN created_at TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMP;

UPDATE product SET created_at = NOW(), updated_at = NOW() WHERE created_at IS NULL;