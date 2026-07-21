CREATE TABLE widget_definitions (
    definition_id BIGSERIAL PRIMARY KEY,
    company_id BIGINT REFERENCES company(company_id),
    user_id BIGINT REFERENCES "user"(user_id),
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    widget_type VARCHAR(50) NOT NULL,
    config JSONB NOT NULL DEFAULT '{}',
    data_source VARCHAR(50) NOT NULL,
    query_config JSONB NOT NULL DEFAULT '{}',
    visual_config JSONB NOT NULL DEFAULT '{}',
    is_template BOOLEAN NOT NULL DEFAULT FALSE,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE dashboard_widget
    ADD COLUMN IF NOT EXISTS definition_id BIGINT REFERENCES widget_definitions(definition_id);

CREATE INDEX idx_widget_defs_company ON widget_definitions(company_id);
CREATE INDEX idx_widget_defs_user ON widget_definitions(user_id);
CREATE INDEX idx_widget_defs_type ON widget_definitions(widget_type);
CREATE INDEX idx_dashboard_widgets_definition ON dashboard_widget(definition_id);
