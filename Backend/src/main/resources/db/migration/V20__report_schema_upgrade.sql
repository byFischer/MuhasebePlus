-- report_type PG enum -> VARCHAR(30)
ALTER TABLE report
    ALTER COLUMN report_type TYPE VARCHAR(30) USING report_type::text;

-- start_date / end_date TIMESTAMP -> DATE (use case tarih araligi diyor)
ALTER TABLE report
    ALTER COLUMN start_date TYPE DATE USING start_date::date,
    ALTER COLUMN end_date TYPE DATE USING end_date::date;

-- Yeni kolonlar
ALTER TABLE report ADD COLUMN IF NOT EXISTS format VARCHAR(10);
ALTER TABLE report ADD COLUMN IF NOT EXISTS file_path VARCHAR(500);
ALTER TABLE report ADD COLUMN IF NOT EXISTS file_size BIGINT;

-- Audit ve soft-delete kolonlari
ALTER TABLE report ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE report ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE report ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE report ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
