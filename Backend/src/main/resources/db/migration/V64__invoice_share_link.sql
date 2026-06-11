-- Mali müşavir fatura paylaşım linki
CREATE TABLE invoice_share_link (
    id               BIGSERIAL PRIMARY KEY,
    company_id       BIGINT      NOT NULL REFERENCES company(company_id),
    token            VARCHAR(64) NOT NULL,
    is_active        BOOLEAN     NOT NULL DEFAULT TRUE,
    created_by       BIGINT      REFERENCES "user"(user_id),
    revoked_at       TIMESTAMP,
    last_accessed_at TIMESTAMP,
    access_count     BIGINT      NOT NULL DEFAULT 0,
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP
);

CREATE UNIQUE INDEX uq_invoice_share_link_token ON invoice_share_link(token);

-- Şirket başına yalnızca bir aktif link; iptal edilen satırlar audit geçmişi olarak kalır
CREATE UNIQUE INDEX uq_invoice_share_link_active_company
    ON invoice_share_link(company_id) WHERE is_active;
