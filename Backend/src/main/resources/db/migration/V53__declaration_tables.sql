-- KDV1/KDV2 beyanname tabloları
CREATE TABLE vat_declaration (
    declaration_id              BIGSERIAL PRIMARY KEY,
    company_id                  BIGINT        NOT NULL REFERENCES company(company_id),
    declaration_type            VARCHAR(10)   NOT NULL,
    year                        INTEGER       NOT NULL,
    month                       INTEGER       NOT NULL,
    status                      VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    total_sales_base            NUMERIC(15,2),
    total_sales_vat             NUMERIC(15,2),
    total_purchases_base        NUMERIC(15,2),
    total_purchases_vat         NUMERIC(15,2),
    previous_period_carryover   NUMERIC(15,2) DEFAULT 0,
    payable_vat                 NUMERIC(15,2),
    next_period_carryover       NUMERIC(15,2),
    xml_content                 TEXT,
    generated_at                TIMESTAMP,
    submitted_at                TIMESTAMP,
    is_deleted                  BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at                  TIMESTAMP,
    created_at                  TIMESTAMP,
    updated_at                  TIMESTAMP,
    CONSTRAINT uq_vat_declaration_company_type_period
        UNIQUE (company_id, declaration_type, year, month)
);

CREATE TABLE vat_declaration_line (
    line_id         BIGSERIAL PRIMARY KEY,
    declaration_id  BIGINT        NOT NULL REFERENCES vat_declaration(declaration_id),
    line_code       VARCHAR(10)   NOT NULL,
    description     VARCHAR(255),
    base_amount     NUMERIC(15,2),
    vat_amount      NUMERIC(15,2),
    line_order      INTEGER       NOT NULL DEFAULT 0,
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP
);

CREATE INDEX idx_vat_declaration_company ON vat_declaration(company_id);
CREATE INDEX idx_vat_declaration_line_decl ON vat_declaration_line(declaration_id);

-- Ba-Bs beyanname tabloları
CREATE TABLE babs_declaration (
    declaration_id  BIGSERIAL PRIMARY KEY,
    company_id      BIGINT       NOT NULL REFERENCES company(company_id),
    babs_type       VARCHAR(5)   NOT NULL,
    year            INTEGER      NOT NULL,
    month           INTEGER      NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    record_count    INTEGER      DEFAULT 0,
    total_amount    NUMERIC(15,2),
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP,
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP,
    CONSTRAINT uq_babs_declaration UNIQUE (company_id, babs_type, year, month)
);

CREATE TABLE babs_declaration_line (
    line_id         BIGSERIAL PRIMARY KEY,
    declaration_id  BIGINT        NOT NULL REFERENCES babs_declaration(declaration_id),
    tax_number      VARCHAR(20),
    customer_name   VARCHAR(255),
    total_amount    NUMERIC(15,2),
    document_count  INTEGER,
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP
);

-- Muhtasar beyanname tabloları
CREATE TABLE withholding_declaration (
    declaration_id              BIGSERIAL PRIMARY KEY,
    company_id                  BIGINT        NOT NULL REFERENCES company(company_id),
    year                        INTEGER       NOT NULL,
    month                       INTEGER       NOT NULL,
    status                      VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    total_withholding_base      NUMERIC(15,2),
    total_withholding_amount    NUMERIC(15,2),
    xml_content                 TEXT,
    is_deleted                  BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at                  TIMESTAMP,
    created_at                  TIMESTAMP,
    updated_at                  TIMESTAMP,
    CONSTRAINT uq_withholding_declaration UNIQUE (company_id, year, month)
);

CREATE TABLE withholding_declaration_line (
    line_id                   BIGSERIAL PRIMARY KEY,
    declaration_id            BIGINT        NOT NULL REFERENCES withholding_declaration(declaration_id),
    withholding_tax_code_id   INTEGER       REFERENCES withholding_tax_code(code_id),
    withholding_code          VARCHAR(10),
    description               VARCHAR(255),
    base_amount               NUMERIC(15,2),
    withholding_amount        NUMERIC(15,2),
    document_count            INTEGER,
    line_order                INTEGER       NOT NULL DEFAULT 0,
    created_at                TIMESTAMP,
    updated_at                TIMESTAMP
);

-- GenericDeclaration (KDV2, Geçici Vergi, Kurumlar Vergisi)
CREATE TABLE generic_declaration (
    declaration_id      BIGSERIAL PRIMARY KEY,
    company_id          BIGINT       NOT NULL REFERENCES company(company_id),
    declaration_type    VARCHAR(30)  NOT NULL,
    year                INTEGER      NOT NULL,
    period              INTEGER      NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    data_snapshot       JSONB,
    xml_content         TEXT,
    total_payable       NUMERIC(15,2),
    is_deleted          BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMP,
    created_at          TIMESTAMP,
    updated_at          TIMESTAMP,
    CONSTRAINT uq_generic_declaration UNIQUE (company_id, declaration_type, year, period)
);
