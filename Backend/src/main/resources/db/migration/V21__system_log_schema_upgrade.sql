-- log_id SERIAL (INT) -> BIGINT (proje genelindeki BIGINT standardina uyum)
ALTER TABLE system_log ALTER COLUMN log_id TYPE BIGINT;
ALTER SEQUENCE system_log_log_id_seq AS BIGINT;

-- log_level PG enum -> VARCHAR(20)
ALTER TABLE system_log
    ALTER COLUMN log_level TYPE VARCHAR(20) USING log_level::text;

-- Mevcut lowercase verileri uppercase'e cevir (Java enum @Enumerated(STRING) ile uyum icin)
UPDATE system_log SET log_level = UPPER(log_level) WHERE log_level IS NOT NULL;

-- Audit kolonlari (BaseEntity'den miras alinan created_at/updated_at)
ALTER TABLE system_log ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE system_log ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Filtreleme sorgulari icin index
CREATE INDEX IF NOT EXISTS idx_system_log_company_timestamp ON system_log(company_id, timestamp DESC);
