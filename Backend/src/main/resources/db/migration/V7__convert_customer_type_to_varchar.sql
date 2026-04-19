ALTER TABLE customer
    ALTER COLUMN "type" TYPE VARCHAR(50) USING "type"::text;

UPDATE customer
SET "type" = UPPER("type")
WHERE "type" IS NOT NULL;