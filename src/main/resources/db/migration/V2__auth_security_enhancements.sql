-- ============================================================
-- V2__auth_security_enhancements.sql
-- Security enhancements: Max 4 Attempts, 72H Lockout,
-- Captcha/Recovery, Security Questions, Dynamic Attributes,
-- Password History, and Blockchain Audit Anchoring.
-- ============================================================

-- 1. Extend Users table for strict security & dynamic attributes
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS temp_password_issued_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS temp_password_expiry TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS recovery_phrase_hash VARCHAR(255),
    ADD COLUMN IF NOT EXISTS dynamic_attributes JSONB DEFAULT '{}'::jsonb,
    ADD COLUMN IF NOT EXISTS lockout_count INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_known_ip VARCHAR(50),
    ADD COLUMN IF NOT EXISTS last_known_device VARCHAR(255);

-- 2. Security Questions Table
CREATE TABLE IF NOT EXISTS security_questions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_text VARCHAR(255) UNIQUE NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Seed default security questions
INSERT INTO security_questions (question_text) VALUES
    ('What was the name of your first pet?'),
    ('In what city or town were you born?'),
    ('What is your mother''s maiden name?'),
    ('What was the name of your primary school?'),
    ('What was your favorite food as a child?')
ON CONFLICT (question_text) DO NOTHING;

-- 3. User Security Question Answers
CREATE TABLE IF NOT EXISTS user_security_answers (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    question_id   UUID NOT NULL REFERENCES security_questions(id) ON DELETE CASCADE,
    answer_hash   VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_question UNIQUE (user_id, question_id)
);

-- 4. Single-use Backup Recovery Codes
CREATE TABLE IF NOT EXISTS user_backup_codes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code_hash   VARCHAR(255) NOT NULL,
    used        BOOLEAN NOT NULL DEFAULT FALSE,
    used_at     TIMESTAMP WITH TIME ZONE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. Password History Table (Supports Bloom Filter backing store)
CREATE TABLE IF NOT EXISTS password_histories (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. Google-style Background Verification Challenges (Suspicious sign-ins & 72h unlocks)
CREATE TABLE IF NOT EXISTS account_verification_challenges (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    challenge_type VARCHAR(50) NOT NULL CHECK (challenge_type IN ('EMAIL_OTP', 'PHONE_SMS_OTP', 'SECURITY_QUESTION_CHALLENGE', 'SUSPICIOUS_LOGIN_VERIFY')),
    challenge_code VARCHAR(255) NOT NULL,
    trigger_reason VARCHAR(255),
    ip_address     VARCHAR(50),
    device_info    VARCHAR(255),
    is_verified    BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 7. Blockchain Audit Anchors (Immutable Tamper-Proof Merkle Proofs)
CREATE TABLE IF NOT EXISTS blockchain_audit_anchors (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id           VARCHAR(100) UNIQUE NOT NULL,
    merkle_root_hash   VARCHAR(128) NOT NULL,
    event_count        INT NOT NULL DEFAULT 0,
    tx_hash            VARCHAR(128),
    blockchain_network VARCHAR(50) NOT NULL DEFAULT 'POLYGON_AMOY',
    status             VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'COMMITTED', 'FAILED')),
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at       TIMESTAMP WITH TIME ZONE
);

-- Indexes for high performance
CREATE INDEX IF NOT EXISTS idx_user_backup_codes_user ON user_backup_codes(user_id);
CREATE INDEX IF NOT EXISTS idx_user_security_answers_user ON user_security_answers(user_id);
CREATE INDEX IF NOT EXISTS idx_password_histories_user ON password_histories(user_id);
CREATE INDEX IF NOT EXISTS idx_verification_challenges_user ON account_verification_challenges(user_id);
CREATE INDEX IF NOT EXISTS idx_verification_challenges_expires ON account_verification_challenges(expires_at);
