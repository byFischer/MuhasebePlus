-- dashboard_preference tablosunu yeni schema'ya yukselt
ALTER TABLE dashboard_preference ADD COLUMN IF NOT EXISTS company_id BIGINT REFERENCES company(company_id);
ALTER TABLE dashboard_preference ADD COLUMN IF NOT EXISTS name VARCHAR(100) DEFAULT 'Ana Dashboard';
ALTER TABLE dashboard_preference ADD COLUMN IF NOT EXISTS is_default BOOLEAN DEFAULT TRUE;
ALTER TABLE dashboard_preference ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE dashboard_preference ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE dashboard_preference ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE dashboard_preference ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE dashboard_preference ADD COLUMN IF NOT EXISTS accent_color VARCHAR(20) DEFAULT '#2f6df6';

-- widget_layout VARCHAR(255) -> JSONB
ALTER TABLE dashboard_preference ALTER COLUMN widget_layout TYPE JSONB USING
    CASE
        WHEN widget_layout IS NULL OR widget_layout = '' THEN NULL
        ELSE widget_layout::JSONB
    END;

-- dashboard_layout tablosu: kullanici bazi N layout tutabilir
CREATE TABLE IF NOT EXISTS dashboard_layout (
    layout_id    BIGSERIAL PRIMARY KEY,
    user_id      BIGINT REFERENCES "user"(user_id),
    company_id   BIGINT REFERENCES company(company_id),
    name         VARCHAR(100) NOT NULL DEFAULT 'Ana Dashboard',
    is_default   BOOLEAN      NOT NULL DEFAULT FALSE,
    theme        VARCHAR(50)  DEFAULT 'dark',
    accent_color VARCHAR(20)  DEFAULT '#2f6df6',
    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    is_deleted   BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at   TIMESTAMP
);

-- dashboard_widget tablosu: bir layout'ta N widget instance
CREATE TABLE IF NOT EXISTS dashboard_widget (
    widget_id   BIGSERIAL PRIMARY KEY,
    layout_id   BIGINT NOT NULL REFERENCES dashboard_layout(layout_id) ON DELETE CASCADE,
    widget_type VARCHAR(50) NOT NULL,
    title       VARCHAR(100),
    position_x  INT         NOT NULL DEFAULT 0,
    position_y  INT         NOT NULL DEFAULT 0,
    width       INT         NOT NULL DEFAULT 4,
    height      INT         NOT NULL DEFAULT 3,
    config      JSONB,
    created_at  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);

-- ai_widget_recipe tablosu: AI ile uretilmis widget tarifleri (cache + audit)
CREATE TABLE IF NOT EXISTS ai_widget_recipe (
    recipe_id          BIGSERIAL PRIMARY KEY,
    user_prompt        TEXT,
    generated_config   JSONB,
    model_used         VARCHAR(50),
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by_user_id BIGINT REFERENCES "user"(user_id)
);

-- ai_insight_log tablosu: LLM cagri kayitlari
CREATE TABLE IF NOT EXISTS ai_insight_log (
    log_id       BIGSERIAL PRIMARY KEY,
    user_id      BIGINT REFERENCES "user"(user_id),
    company_id   BIGINT REFERENCES company(company_id),
    endpoint     VARCHAR(100),
    input_tokens INT  DEFAULT 0,
    output_tokens INT DEFAULT 0,
    latency_ms   INT  DEFAULT 0,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ai_quota tablosu: sirket basi aylik token limiti
CREATE TABLE IF NOT EXISTS ai_quota (
    company_id              BIGINT PRIMARY KEY REFERENCES company(company_id),
    monthly_token_budget    INT    NOT NULL DEFAULT 200000,
    tokens_used_this_month  INT    NOT NULL DEFAULT 0,
    reset_at                TIMESTAMP,
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Performans icin indeksler
CREATE INDEX IF NOT EXISTS idx_dashboard_layout_company ON dashboard_layout(company_id, is_deleted);
CREATE INDEX IF NOT EXISTS idx_dashboard_widget_layout  ON dashboard_widget(layout_id);
CREATE INDEX IF NOT EXISTS idx_ai_insight_log_company   ON ai_insight_log(company_id, created_at DESC);
