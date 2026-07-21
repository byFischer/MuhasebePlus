-- report.report_id: SERIAL (INTEGER) -> BIGINT (entity uses Long)
ALTER TABLE report ALTER COLUMN report_id TYPE BIGINT;
ALTER SEQUENCE report_report_id_seq AS BIGINT;

-- customer_note.note_id: SERIAL (INTEGER) -> BIGINT (entity uses Long)
ALTER TABLE customer_note ALTER COLUMN note_id TYPE BIGINT;
ALTER SEQUENCE customer_note_note_id_seq AS BIGINT;
