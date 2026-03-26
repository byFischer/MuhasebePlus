-- ============================================================
-- MuhasebePlus - Initial Database Schema
-- ============================================================

-- ENUMS

CREATE TYPE user_role AS ENUM ('user', 'admin');
CREATE TYPE transaction_type AS ENUM ('INCOME', 'EXPENSE');
CREATE TYPE payment_status AS ENUM ('pending', 'paid', 'overdue');
CREATE TYPE notification_type AS ENUM ('info', 'warning', 'error');
CREATE TYPE report_type AS ENUM ('profit_loss', 'expense', 'income');
CREATE TYPE log_level AS ENUM ('info', 'warning', 'error');
CREATE TYPE task_type AS ENUM ('stock', 'invoice', 'report');
CREATE TYPE customer_type AS ENUM ('individual', 'corporate');
CREATE TYPE invoice_type AS ENUM ('sale', 'purchase');
CREATE TYPE currency_type AS ENUM ('TRY', 'USD', 'EUR');

-- ============================================================
-- TABLES
-- ============================================================

-- "user" keyword is reserved in PostgreSQL, quoted
CREATE TABLE "user" (
    user_id     BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,
    first_name  VARCHAR(100),
    last_name   VARCHAR(100),
    phone       VARCHAR(20),
    birth_date  DATE,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    user_role   user_role DEFAULT 'user'
);

CREATE TABLE customer (
    customer_id      SERIAL PRIMARY KEY,
    tax_number       VARCHAR(50),
    name             VARCHAR(100),
    address          VARCHAR(255),
    city             VARCHAR(100),
    phone            VARCHAR(20),
    type             customer_type,
    current_balance  DECIMAL(15,2),
    is_deleted       BOOLEAN DEFAULT FALSE,
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP
);

CREATE TABLE user_session (
    session_id  SERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES "user"(user_id),
    token       VARCHAR(500),
    expired_at  TIMESTAMP,
    ip_address  VARCHAR(50)
);

CREATE TABLE bank_account (
    account_id   BIGSERIAL PRIMARY KEY,
    user_id      BIGINT REFERENCES "user"(user_id),
    customer_id  INT    REFERENCES customer(customer_id),
    bank_name    VARCHAR(100),
    iban         VARCHAR(50),
    currency     currency_type
);

CREATE TABLE transaction_template (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES "user"(user_id),
    template_name   VARCHAR(255),
    default_amount  DECIMAL(15,2)
);

CREATE TABLE customer_note (
    note_id      SERIAL PRIMARY KEY,
    customer_id  INT NOT NULL REFERENCES customer(customer_id),
    content      TEXT,
    created_at   TIMESTAMP
);

CREATE TABLE invoice (
    invoice_id      BIGSERIAL PRIMARY KEY,
    invoice_number  VARCHAR(50) UNIQUE,
    customer_id     INT REFERENCES customer(customer_id),
    invoice_type    invoice_type,
    due_date        DATE,
    payment_status  payment_status DEFAULT 'pending',
    subtotal        DECIMAL(15,2),
    vat_amount      DECIMAL(15,2),
    total_amount    DECIMAL(15,2)
);

CREATE TABLE product (
    product_id   SERIAL PRIMARY KEY,
    barcode      VARCHAR(100) UNIQUE,
    name         VARCHAR(255),
    description  TEXT,
    unit         VARCHAR(50),
    sale_price   DECIMAL(15,2),
    vat_rate     DECIMAL(5,2),
    cost_price   DECIMAL(15,2)
);

CREATE TABLE stock (
    stock_id        SERIAL PRIMARY KEY,
    product_id      INT NOT NULL REFERENCES product(product_id),
    quantity        INT DEFAULT 0,
    min_quantity    INT,
    is_deleted      BOOLEAN DEFAULT FALSE,
    last_count_date TIMESTAMP,
    updated_at      TIMESTAMP,
    created_at      TIMESTAMP
);

CREATE TABLE transaction (
    transaction_id    BIGSERIAL PRIMARY KEY,
    account_id        BIGINT NOT NULL REFERENCES bank_account(account_id),
    invoice_id        BIGINT REFERENCES invoice(invoice_id),
    transaction_type  transaction_type,
    amount            DECIMAL(15,2),
    description       VARCHAR(255),
    category          VARCHAR(100),
    updated_at        TIMESTAMP,
    is_recurring      BOOLEAN DEFAULT FALSE
);

CREATE TABLE dashboard_preference (
    preference_id  BIGSERIAL PRIMARY KEY,
    user_id        BIGINT REFERENCES "user"(user_id),
    widget_layout  VARCHAR(255),
    theme          VARCHAR(50),
    saved_at       TIMESTAMP
);

CREATE TABLE report (
    report_id     SERIAL PRIMARY KEY,
    user_id       BIGINT REFERENCES "user"(user_id),
    report_type   report_type,
    start_date    TIMESTAMP,
    end_date      TIMESTAMP,
    generated_at  TIMESTAMP
);

CREATE TABLE system_log (
    log_id      SERIAL PRIMARY KEY,
    user_id     BIGINT REFERENCES "user"(user_id),
    log_level   log_level,
    details     TEXT,
    timestamp   TIMESTAMP,
    ip_address  VARCHAR(50)
);

CREATE TABLE notification (
    notification_id    SERIAL PRIMARY KEY,
    user_id            BIGINT REFERENCES "user"(user_id),
    notification_type  notification_type,
    subject            VARCHAR(255),
    message            TEXT,
    sent_at            TIMESTAMP
);

CREATE TABLE schedule_task (
    task_id    SERIAL PRIMARY KEY,
    task_name  VARCHAR(255),
    task_type  task_type,
    is_active  BOOLEAN DEFAULT TRUE
);
