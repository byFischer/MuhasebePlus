ALTER TABLE customer ALTER COLUMN customer_id TYPE BIGINT;
ALTER SEQUENCE customer_customer_id_seq AS BIGINT;

ALTER TABLE user_session ALTER COLUMN session_id TYPE BIGINT;
ALTER SEQUENCE user_session_session_id_seq AS BIGINT;
