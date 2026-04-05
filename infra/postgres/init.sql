-- ReactiveCommerce Platform — PostgreSQL Schema
-- Cada servicio tiene su propio schema para garantizar database-per-service

CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS product;
CREATE SCHEMA IF NOT EXISTS orders;
CREATE SCHEMA IF NOT EXISTS report;

-- ── AUTH SCHEMA ──────────────────────────────────────────────────────────
SET search_path TO auth;

CREATE TABLE IF NOT EXISTS users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255),
    role            VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN','CREATOR','BUYER','MODERATOR')),
    email_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    oauth_provider  VARCHAR(50),
    oauth_provider_id VARCHAR(255),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role  ON users(role);

-- ── PRODUCT SCHEMA ───────────────────────────────────────────────────────
SET search_path TO product;

CREATE TABLE IF NOT EXISTS assets (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title            VARCHAR(500) NOT NULL,
    description      TEXT,
    category         VARCHAR(20)  NOT NULL CHECK (category IN ('TEMPLATE','IMAGE','CODE','DESIGN','OTHER')),
    tags_json        TEXT,
    price            NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    license          VARCHAR(100),
    status           VARCHAR(30)  NOT NULL DEFAULT 'PENDING_MODERATION'
                     CHECK (status IN ('PENDING_MODERATION','APPROVED','REJECTED','PUBLISHED','UNPUBLISHED')),
    creator_id       UUID         NOT NULL,
    s3_key           VARCHAR(1000),
    thumbnail_s3_key VARCHAR(1000),
    format           VARCHAR(20),
    file_size_bytes  BIGINT,
    average_rating   NUMERIC(3,2) DEFAULT 0.0,
    total_reviews    INT          DEFAULT 0,
    total_sales      INT          DEFAULT 0,
    rejection_reason TEXT,
    moderated_by     UUID,
    moderated_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_assets_status      ON assets(status);
CREATE INDEX IF NOT EXISTS idx_assets_creator     ON assets(creator_id);
CREATE INDEX IF NOT EXISTS idx_assets_category    ON assets(category);
CREATE INDEX IF NOT EXISTS idx_assets_price       ON assets(price);
CREATE INDEX IF NOT EXISTS idx_assets_rating      ON assets(average_rating DESC);
CREATE INDEX IF NOT EXISTS idx_assets_sales       ON assets(total_sales DESC);
CREATE INDEX IF NOT EXISTS idx_assets_created     ON assets(created_at DESC);

-- ── ORDERS SCHEMA ────────────────────────────────────────────────────────
SET search_path TO orders;

CREATE TABLE IF NOT EXISTS orders (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    buyer_id        UUID         NOT NULL,
    asset_id        UUID         NOT NULL,
    amount          NUMERIC(12,2) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING','COMPLETED','FAILED','REFUNDED','CANCELLED')),
    failure_reason  TEXT,
    transaction_id  VARCHAR(255),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_orders_buyer       ON orders(buyer_id);
CREATE INDEX IF NOT EXISTS idx_orders_status      ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_expires     ON orders(expires_at) WHERE status = 'PENDING';
CREATE UNIQUE INDEX IF NOT EXISTS uq_orders_buyer_asset
    ON orders(buyer_id, asset_id) WHERE status = 'COMPLETED';

-- ── REPORT SCHEMA ────────────────────────────────────────────────────────
SET search_path TO report;

CREATE TABLE IF NOT EXISTS reports (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requested_by     UUID         NOT NULL,
    type             VARCHAR(30)  NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    period_from      TIMESTAMPTZ,
    period_to        TIMESTAMPTZ,
    s3_key           VARCHAR(1000),
    presigned_url    TEXT,
    progress_percent INT          DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    completed_at     TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_reports_requester ON reports(requested_by);
CREATE INDEX IF NOT EXISTS idx_reports_status    ON reports(status);

SET search_path TO public;
