-- Invoice enhancements for accounting-grade faturalar
-- Invoice date separate from due date
ALTER TABLE invoice ADD COLUMN IF NOT EXISTS invoice_date DATE;
UPDATE invoice SET invoice_date = due_date WHERE invoice_date IS NULL;
ALTER TABLE invoice ALTER COLUMN invoice_date SET NOT NULL;

-- Currency and exchange rate
ALTER TABLE invoice ADD COLUMN IF NOT EXISTS currency VARCHAR(3) DEFAULT 'TRY';
ALTER TABLE invoice ADD COLUMN IF NOT EXISTS exchange_rate DECIMAL(10,4) DEFAULT 1.0000;

-- Discount
ALTER TABLE invoice ADD COLUMN IF NOT EXISTS discount_type VARCHAR(10);
ALTER TABLE invoice ADD COLUMN IF NOT EXISTS discount_amount DECIMAL(15,2) DEFAULT 0;

-- Withholding tax total
ALTER TABLE invoice ADD COLUMN IF NOT EXISTS withholding_tax_amount DECIMAL(15,2) DEFAULT 0;

-- Description and delivery address
ALTER TABLE invoice ADD COLUMN IF NOT EXISTS description VARCHAR(500);
ALTER TABLE invoice ADD COLUMN IF NOT EXISTS delivery_address VARCHAR(500);

-- Cancellation support
ALTER TABLE invoice ADD COLUMN IF NOT EXISTS cancelled BOOLEAN DEFAULT FALSE;
ALTER TABLE invoice ADD COLUMN IF NOT EXISTS cancellation_reason VARCHAR(500);
ALTER TABLE invoice ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP;

-- Return/ref invoice linking
ALTER TABLE invoice ADD COLUMN IF NOT EXISTS reference_invoice_id BIGINT;

-- Series
CREATE TABLE IF NOT EXISTS invoice_series (
    series_id           BIGSERIAL PRIMARY KEY,
    company_id          BIGINT NOT NULL,
    series_code         VARCHAR(10) NOT NULL,
    invoice_type        VARCHAR(10) NOT NULL,
    prefix              VARCHAR(20),
    year                INTEGER,
    last_sequence_number BIGINT NOT NULL DEFAULT 0,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP,
    updated_at          TIMESTAMP,
    CONSTRAINT fk_series_company FOREIGN KEY (company_id) REFERENCES company(company_id)
);

ALTER TABLE invoice ADD COLUMN IF NOT EXISTS series_id BIGINT;
ALTER TABLE invoice ADD COLUMN IF NOT EXISTS sequence_number BIGINT;

-- Line item discounts and withholding tax
ALTER TABLE invoice_line_item ADD COLUMN IF NOT EXISTS discount_rate DECIMAL(5,2) DEFAULT 0;
ALTER TABLE invoice_line_item ADD COLUMN IF NOT EXISTS withholding_tax_rate DECIMAL(5,2) DEFAULT 0;

-- Indexes
CREATE INDEX IF NOT EXISTS idx_invoice_series_company ON invoice(series_id);
CREATE INDEX IF NOT EXISTS idx_invoice_reference ON invoice(reference_invoice_id);
CREATE INDEX IF NOT EXISTS idx_invoice_cancelled ON invoice(cancelled) WHERE cancelled = true;
