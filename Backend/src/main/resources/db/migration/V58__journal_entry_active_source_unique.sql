ALTER TABLE journal_entry DROP CONSTRAINT IF EXISTS uq_entry_source;

CREATE UNIQUE INDEX IF NOT EXISTS uq_entry_source_active
    ON journal_entry(company_id, source_type, source_id)
    WHERE source_id IS NOT NULL
      AND is_deleted = false
      AND is_reversed = false;
