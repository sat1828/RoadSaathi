CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "postgis";

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(20) DEFAULT 'DRIVER',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE hazard_reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reporter_id UUID REFERENCES users(id),
    hazard_type VARCHAR(32) NOT NULL,
    confidence FLOAT4,
    location GEOGRAPHY(Point, 4326) NOT NULL,
    photo_url TEXT,
    severity SMALLINT DEFAULT 1,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    nh_corridor VARCHAR(16),
    reported_at TIMESTAMPTZ DEFAULT NOW(),
    expires_at TIMESTAMPTZ DEFAULT NOW() + INTERVAL '48 hours',
    confirm_count INT DEFAULT 0,
    ai_brief TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_hazard_gist ON hazard_reports USING GIST(location);
CREATE INDEX idx_hazard_status ON hazard_reports(status);
CREATE INDEX idx_hazard_expires ON hazard_reports(expires_at);

CREATE TABLE driver_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id),
    fcm_token TEXT NOT NULL,
    current_pos GEOGRAPHY(Point, 4326),
    last_seen TIMESTAMPTZ DEFAULT NOW(),
    heading FLOAT4,
    speed_kmh FLOAT4
);

CREATE INDEX idx_driver_gist ON driver_sessions USING GIST(current_pos);

CREATE TABLE hazard_reports_history (LIKE hazard_reports INCLUDING ALL);
ALTER TABLE hazard_reports_history ADD COLUMN archived_at TIMESTAMPTZ DEFAULT NOW();
ALTER TABLE hazard_reports_history ADD COLUMN archived_reason VARCHAR(32);

CREATE TABLE hazard_clusters (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    center GEOGRAPHY(Point, 4326) NOT NULL,
    radius_km FLOAT4,
    nh_corridor VARCHAR(16),
    report_count INT DEFAULT 0,
    hazard_types TEXT[],
    ai_brief TEXT,
    first_reported_at TIMESTAMPTZ,
    last_alerted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_cluster_gist ON hazard_clusters USING GIST(center);
