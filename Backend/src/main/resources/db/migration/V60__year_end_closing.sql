-- Year-end closing: track closing journal entry and metadata on accounting_period
ALTER TABLE accounting_period
  ADD COLUMN IF NOT EXISTS year_end_closed_at      TIMESTAMP,
  ADD COLUMN IF NOT EXISTS year_end_closed_by      BIGINT,
  ADD COLUMN IF NOT EXISTS closing_journal_entry_id BIGINT;
