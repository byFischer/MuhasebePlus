-- notification_id SERIAL (INT) -> BIGINT (proje genelindeki BIGINT standardina uyum)
ALTER TABLE notification ALTER COLUMN notification_id TYPE BIGINT;
ALTER SEQUENCE notification_notification_id_seq AS BIGINT;

-- notification_type PG enum -> VARCHAR(20), Java enum degerleri lowercase tutuluyor.
ALTER TABLE notification
    ALTER COLUMN notification_type TYPE VARCHAR(20) USING notification_type::text;

-- Audit ve bildirim okuma/e-posta durum kolonlari
ALTER TABLE notification ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE notification ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE notification ADD COLUMN IF NOT EXISTS read_at TIMESTAMP;
ALTER TABLE notification ADD COLUMN IF NOT EXISTS recipient_email VARCHAR(255);
ALTER TABLE notification ADD COLUMN IF NOT EXISTS email_sent BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE notification ADD COLUMN IF NOT EXISTS email_error VARCHAR(1000);

CREATE INDEX IF NOT EXISTS idx_notification_company_user_sent
    ON notification(company_id, user_id, sent_at DESC);

CREATE INDEX IF NOT EXISTS idx_notification_company_user_read
    ON notification(company_id, user_id, read_at);
