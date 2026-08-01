-- ============================================================
-- AUTH-DB SCHEMAS & TABLES (9 Core Tables)
-- ============================================================

-- 1. Users Table
CREATE TABLE users (
    id                    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    email                 VARCHAR(150) UNIQUE NOT NULL,
    username              VARCHAR(50)  UNIQUE,
    password              VARCHAR(255) NOT NULL,
    first_name            VARCHAR(100),
    last_name             VARCHAR(100),
    phone                 VARCHAR(20),
    profile_image         TEXT,
    email_verified        BOOLEAN     NOT NULL DEFAULT FALSE,
    phone_verified        BOOLEAN     NOT NULL DEFAULT FALSE,
    two_factor_enabled    BOOLEAN     NOT NULL DEFAULT FALSE,
    two_factor_secret     VARCHAR(255),
    provider              VARCHAR(20)  NOT NULL DEFAULT 'LOCAL'
                              CHECK (provider IN ('LOCAL','GOOGLE','GITHUB','FACEBOOK')),
    provider_id           VARCHAR(255),
    account_status        VARCHAR(30)  NOT NULL DEFAULT 'PENDING_VERIFICATION'
                              CHECK (account_status IN (
                                  'ACTIVE','INACTIVE',
                                  'BLOCKED','PENDING_VERIFICATION'
                              )),
    last_login            TIMESTAMP WITH TIME ZONE,
    failed_login_attempts INT         NOT NULL DEFAULT 0 CHECK (failed_login_attempts >= 0),
    locked_until          TIMESTAMP WITH TIME ZONE,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Roles Table
CREATE TABLE roles (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(500),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Addresses Table
CREATE TABLE addresses (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    address_type  VARCHAR(20)  NOT NULL DEFAULT 'HOME'
                      CHECK (address_type IN ('HOME','WORK','OTHER')),
    full_name     VARCHAR(200) NOT NULL,
    phone         VARCHAR(20)  NOT NULL,
    address_line1 VARCHAR(300) NOT NULL,
    address_line2 VARCHAR(300),
    city          VARCHAR(100) NOT NULL,
    state         VARCHAR(100) NOT NULL,
    country       VARCHAR(100) NOT NULL DEFAULT 'India',
    postal_code   VARCHAR(20),
    is_default    BOOLEAN     NOT NULL DEFAULT FALSE,
    latitude      DOUBLE PRECISION,
    longitude     DOUBLE PRECISION,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. Permissions Table
CREATE TABLE permissions (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(500),
    resource    VARCHAR(100),
    action      VARCHAR(50)
);

-- 5. Login History Table
CREATE TABLE login_history (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    ip_address     VARCHAR(50),
    device_info    VARCHAR(255),
    browser        VARCHAR(100),
    os             VARCHAR(100),
    location       VARCHAR(200),
    success        BOOLEAN     NOT NULL DEFAULT TRUE,
    failure_reason VARCHAR(200),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. Email Verifications Table
CREATE TABLE email_verifications (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      VARCHAR(255) UNIQUE NOT NULL,
    type       VARCHAR(50)  NOT NULL
                   CHECK (type IN (
                       'EMAIL_VERIFICATION',
                       'PASSWORD_RESET',
                       'PHONE_VERIFICATION',
                       'EMAIL_CHANGE'
                   )),
    used       BOOLEAN     NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 7. Refresh Tokens Table
CREATE TABLE refresh_tokens (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(512) UNIQUE NOT NULL,
    revoked     BOOLEAN     NOT NULL DEFAULT FALSE,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    device_info VARCHAR(255),
    ip_address  VARCHAR(50),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 8. User Roles Junction Table
CREATE TABLE user_roles (
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id    UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- 9. Role Permissions Junction Table
CREATE TABLE role_permissions (
    role_id       UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- ============================================================
-- INDEXES
-- ============================================================
CREATE INDEX idx_users_email           ON users(email);
CREATE UNIQUE INDEX idx_users_lower_email ON users(LOWER(email));
CREATE INDEX idx_users_username        ON users(username);
CREATE INDEX idx_users_phone           ON users(phone);
CREATE INDEX idx_users_provider        ON users(provider, provider_id);
CREATE INDEX idx_users_status          ON users(account_status);
CREATE INDEX idx_users_created_at      ON users(created_at DESC);
CREATE INDEX idx_refresh_tokens_token  ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user   ON refresh_tokens(user_id, revoked);
CREATE INDEX idx_email_verif_token     ON email_verifications(token);
CREATE INDEX idx_email_verif_user      ON email_verifications(user_id, type);
CREATE INDEX idx_addresses_user        ON addresses(user_id);
CREATE UNIQUE INDEX idx_addresses_single_default ON addresses (user_id) WHERE is_default = TRUE;
CREATE INDEX idx_login_history_user    ON login_history(user_id);
CREATE INDEX idx_login_history_date    ON login_history(created_at DESC);

-- ============================================================
-- TRIGGERS
-- ============================================================
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_addresses_updated_at
    BEFORE UPDATE ON addresses
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- ============================================================
-- SEED DATA
-- ============================================================
INSERT INTO roles (id, name, description) VALUES
    (gen_random_uuid(), 'ROLE_ADMIN',            'System Administrator'),
    (gen_random_uuid(), 'ROLE_CUSTOMER',         'Regular Customer'),
    (gen_random_uuid(), 'ROLE_SELLER',           'Product Seller'),
    (gen_random_uuid(), 'ROLE_DELIVERY_PARTNER', 'Delivery Partner'),
    (gen_random_uuid(), 'ROLE_SUPPORT',          'Customer Support Agent');

INSERT INTO permissions (id, name, description, resource, action) VALUES
    (gen_random_uuid(), 'user:read',        'Read user data',        'USER',    'READ'),
    (gen_random_uuid(), 'user:write',       'Write user data',        'USER',    'WRITE'),
    (gen_random_uuid(), 'user:delete',      'Delete user',            'USER',    'DELETE'),
    (gen_random_uuid(), 'product:read',     'Read products',          'PRODUCT', 'READ'),
    (gen_random_uuid(), 'product:write',    'Write products',         'PRODUCT', 'WRITE'),
    (gen_random_uuid(), 'order:read',       'Read orders',            'ORDER',   'READ'),
    (gen_random_uuid(), 'order:write',      'Write orders',           'ORDER',   'WRITE');

-- Assign permissions to ADMIN & CUSTOMER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'ROLE_ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'ROLE_CUSTOMER' AND p.name IN ('user:read', 'user:write', 'product:read', 'order:read', 'order:write');
