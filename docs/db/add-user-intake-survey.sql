-- Reference DDL for deployments that disable Hibernate ddl-auto=update.
-- Current prod creates this new table via Hibernate on startup. Do not reset existing data.
CREATE TABLE IF NOT EXISTS user_intake_survey (
    user_id BIGINT NOT NULL PRIMARY KEY,
    template_version VARCHAR(40) NOT NULL,
    answers_json TEXT NOT NULL,
    scores_json TEXT,
    current_tab INT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6),
    revision BIGINT
);
