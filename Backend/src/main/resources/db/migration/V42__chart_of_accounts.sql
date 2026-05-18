-- Chart of Accounts (Tek Düzen Hesap Planı)
CREATE TABLE IF NOT EXISTS chart_of_account (
    account_id      BIGSERIAL PRIMARY KEY,
    company_id      BIGINT        NOT NULL REFERENCES company(company_id),
    account_code    VARCHAR(20)   NOT NULL,
    account_name    VARCHAR(255)  NOT NULL,
    account_type    VARCHAR(20)   NOT NULL,  -- ASSET | LIABILITY | EQUITY | INCOME | EXPENSE | COST | MEMO
    parent_id       BIGINT        REFERENCES chart_of_account(account_id),
    is_leaf         BOOLEAN       NOT NULL DEFAULT TRUE,
    is_system       BOOLEAN       NOT NULL DEFAULT FALSE,
    description     VARCHAR(500),
    is_deleted      BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_account_code UNIQUE (company_id, account_code)
);

CREATE INDEX IF NOT EXISTS idx_coa_company      ON chart_of_account(company_id);
CREATE INDEX IF NOT EXISTS idx_coa_code         ON chart_of_account(company_id, account_code);
CREATE INDEX IF NOT EXISTS idx_coa_parent       ON chart_of_account(parent_id);
