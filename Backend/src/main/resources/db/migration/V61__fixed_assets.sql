CREATE TABLE IF NOT EXISTS asset_category (
  id                        BIGSERIAL PRIMARY KEY,
  company_id                BIGINT       NOT NULL REFERENCES company(company_id),
  name                      VARCHAR(100) NOT NULL,
  depreciation_method       VARCHAR(30)  NOT NULL DEFAULT 'STRAIGHT_LINE',
  useful_life_months        INT          NOT NULL DEFAULT 60,
  annual_rate               NUMERIC(5,2),
  asset_account_code        VARCHAR(20),
  depreciation_account_code VARCHAR(20),
  is_deleted                BOOLEAN      NOT NULL DEFAULT FALSE,
  deleted_at                TIMESTAMP,
  created_at                TIMESTAMP,
  updated_at                TIMESTAMP
);

CREATE TABLE IF NOT EXISTS fixed_asset (
  id                       BIGSERIAL PRIMARY KEY,
  company_id               BIGINT       NOT NULL REFERENCES company(company_id),
  category_id              BIGINT                REFERENCES asset_category(id),
  name                     VARCHAR(200) NOT NULL,
  serial_number            VARCHAR(100),
  acquisition_date         DATE         NOT NULL,
  acquisition_cost         NUMERIC(15,2) NOT NULL,
  salvage_value            NUMERIC(15,2) NOT NULL DEFAULT 0,
  accumulated_depreciation NUMERIC(15,2) NOT NULL DEFAULT 0,
  net_book_value           NUMERIC(15,2),
  status                   VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
  disposed_at              DATE,
  is_deleted               BOOLEAN      NOT NULL DEFAULT FALSE,
  deleted_at               TIMESTAMP,
  created_at               TIMESTAMP,
  updated_at               TIMESTAMP
);

CREATE TABLE IF NOT EXISTS depreciation_line (
  id                       BIGSERIAL PRIMARY KEY,
  fixed_asset_id           BIGINT        NOT NULL REFERENCES fixed_asset(id),
  period_year              INT           NOT NULL,
  period_month             INT           NOT NULL,
  depreciation_amount      NUMERIC(15,2) NOT NULL,
  accumulated_depreciation NUMERIC(15,2) NOT NULL,
  net_book_value           NUMERIC(15,2) NOT NULL,
  journal_entry_id         BIGINT,
  created_at               TIMESTAMP DEFAULT NOW(),
  UNIQUE (fixed_asset_id, period_year, period_month)
);
