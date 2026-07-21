-- Customer status and group fields
ALTER TABLE customer ADD COLUMN IF NOT EXISTS status VARCHAR(10) DEFAULT 'ACTIVE';
ALTER TABLE customer ADD COLUMN IF NOT EXISTS customer_group VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_customer_status ON customer(status);
CREATE INDEX IF NOT EXISTS idx_customer_group ON customer(customer_group);
