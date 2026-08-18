-- ====================================================================
-- SUPABASE POSTGRESQL SCHEMA FOR PATROL TRACKER SYSTEM
-- Tables: user_table, checkpoints, duty_allocation, scan_logs, archive_logs
-- ====================================================================

CREATE TABLE IF NOT EXISTS user_table (
    user_id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'Guard',
    badge_number VARCHAR(50),
    password VARCHAR(255) DEFAULT 'guard123',
    phone_number VARCHAR(50) DEFAULT '+91-9876543210',
    designation VARCHAR(100) DEFAULT 'Constable (PC)',
    status VARCHAR(50) DEFAULT 'Active',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS checkpoints (
    checkpoint_id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    qr_code_data VARCHAR(255) UNIQUE NOT NULL,
    latitude NUMERIC(10, 6),
    longitude NUMERIC(10, 6),
    scan_interval_minutes INT DEFAULT 60,
    instructions TEXT,
    status VARCHAR(50) DEFAULT 'Active',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS duty_allocation (
    duty_id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64),
    station_in_charge_id VARCHAR(64),
    shift_name VARCHAR(100) NOT NULL,
    start_time TIMESTAMP WITH TIME ZONE,
    end_time TIMESTAMP WITH TIME ZONE,
    checkpoints_list TEXT NOT NULL,
    sms_status VARCHAR(50) DEFAULT 'Dispatched',
    status VARCHAR(50) DEFAULT 'Assigned',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS scan_logs (
    scan_id VARCHAR(64) PRIMARY KEY,
    checkpoint_id VARCHAR(64),
    user_id VARCHAR(64),
    duty_id VARCHAR(64),
    scan_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'On-Time',
    latitude NUMERIC(10, 6),
    longitude NUMERIC(10, 6),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS archive_logs (
    archive_id VARCHAR(64) PRIMARY KEY,
    duty_id VARCHAR(64),
    archived_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    summary_stats TEXT,
    raw_logs TEXT
);
