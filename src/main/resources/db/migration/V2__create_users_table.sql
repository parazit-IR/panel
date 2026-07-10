CREATE TABLE users (
    id UUID PRIMARY KEY,
    telegram_user_id BIGINT NOT NULL,
    username VARCHAR(64),
    first_name VARCHAR(128) NOT NULL,
    last_name VARCHAR(128),
    language VARCHAR(16) NOT NULL,
    status VARCHAR(32) NOT NULL,
    blocked BOOLEAN NOT NULL,
    last_interaction_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_users_telegram_user_id UNIQUE (telegram_user_id),
    CONSTRAINT chk_users_telegram_user_id_positive CHECK (telegram_user_id > 0),
    CONSTRAINT chk_users_username_not_blank CHECK (username IS NULL OR btrim(username) <> ''),
    CONSTRAINT chk_users_first_name_not_blank CHECK (btrim(first_name) <> ''),
    CONSTRAINT chk_users_last_name_not_blank CHECK (last_name IS NULL OR btrim(last_name) <> ''),
    CONSTRAINT chk_users_language CHECK (language IN ('FA', 'EN')),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'))
);

CREATE INDEX idx_users_telegram_user_id ON users (telegram_user_id);
CREATE INDEX idx_users_status ON users (status);
CREATE INDEX idx_users_created_at ON users (created_at);
