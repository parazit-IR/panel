ALTER TABLE users
    ADD COLUMN referral_code VARCHAR(16);

ALTER TABLE users
    ADD CONSTRAINT uq_users_referral_code UNIQUE (referral_code);

ALTER TABLE users
    ADD CONSTRAINT chk_users_referral_code_format
        CHECK (referral_code IS NULL OR referral_code ~ '^[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8,16}$');

CREATE INDEX idx_users_referral_code ON users (referral_code);

CREATE TABLE referrals (
    id UUID PRIMARY KEY,
    referrer_user_id UUID NOT NULL,
    referred_user_id UUID NOT NULL,
    referral_code_used VARCHAR(16) NOT NULL,
    status VARCHAR(32) NOT NULL,
    referred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_referrals_referred_user_id UNIQUE (referred_user_id),
    CONSTRAINT fk_referrals_referrer_user_id FOREIGN KEY (referrer_user_id) REFERENCES users (id),
    CONSTRAINT fk_referrals_referred_user_id FOREIGN KEY (referred_user_id) REFERENCES users (id),
    CONSTRAINT chk_referrals_distinct_users CHECK (referrer_user_id <> referred_user_id),
    CONSTRAINT chk_referrals_referral_code_used_format
        CHECK (referral_code_used ~ '^[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8,16}$'),
    CONSTRAINT chk_referrals_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED'))
);

CREATE INDEX idx_referrals_referrer_user_id ON referrals (referrer_user_id);
CREATE INDEX idx_referrals_referred_user_id ON referrals (referred_user_id);
CREATE INDEX idx_referrals_status ON referrals (status);
CREATE INDEX idx_referrals_referred_at ON referrals (referred_at);
