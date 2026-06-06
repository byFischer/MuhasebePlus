ALTER TABLE vat_period
    ADD COLUMN IF NOT EXISTS previous_carried_forward_vat NUMERIC(15,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS settlement_journal_entry_id BIGINT,
    ADD COLUMN IF NOT EXISTS locked_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS locked_by BIGINT;
