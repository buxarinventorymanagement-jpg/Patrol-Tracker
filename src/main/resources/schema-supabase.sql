-- ====================================================================
-- SUPABASE POSTGRESQL SCHEMA & INITIAL DEMO SEED DATA FOR PATROL TRACKER
-- Tables: user_table, checkpoints, duty_allocation, scan_logs, archive_logs
-- ====================================================================

-- 1. USER TABLE (Officers, Guards, Supervisors, Admins)
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

-- 2. CHECKPOINTS TABLE (Physical locations with QR codes)
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

-- 3. DUTY ALLOCATION TABLE (Shift assignments & route rosters)
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

-- 4. SCAN LOGS TABLE (Live QR scans & inspection logs)
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
    qr_id VARCHAR(128),
    thana_name VARCHAR(128),
    photo_proof TEXT,
    patrol_status VARCHAR(128),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 5. ARCHIVE LOGS TABLE (Historical reports & aggregated statistics)
CREATE TABLE IF NOT EXISTS archive_logs (
    archive_id VARCHAR(64) PRIMARY KEY,
    duty_id VARCHAR(64),
    archived_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    summary_stats TEXT,
    raw_logs TEXT
);

-- ====================================================================
-- INITIAL DEMO SEED DATA
-- ====================================================================

INSERT INTO user_table (user_id, name, role, badge_number, password, phone_number, designation, status)
VALUES 
  ('sp-admin', 'Dr. Rajesh Kumar, IPS', 'Admin', 'SP-0001', 'sp123', '+91-9990001112', 'Superintendent of Police (SP)', 'Active'),
  ('usr-003', 'Inspector Vikram Singh', 'Supervisor', 'SHO-1001', 'super123', '+91-9998887770', 'Station House Officer (SHO)', 'Active'),
  ('Patrol Tracker', 'District Police Monitor', 'Admin', 'ADM-8800', 'BXRadmin123', '+91-9990001112', 'Superintendent of Police (SP)', 'Active')
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO checkpoints (checkpoint_id, name, qr_code_data, latitude, longitude, scan_interval_minutes, instructions)
VALUES 
  ('chk-101', 'Main Gate Entrance', 'QR-GATE-MAIN-101', 25.564700, 83.977700, 30, 'Inspect barrier gate lock, check visitor logbook'),
  ('chk-102', 'North Perimeter Fence', 'QR-PERIM-NORTH-102', 25.565800, 83.978500, 45, 'Check fence integrity and perimeter floodlights'),
  ('chk-103', 'Server & Control Room', 'QR-SERVER-CTRL-103', 25.564100, 83.976900, 15, 'Verify AC temperature and access authorization'),
  ('chk-104', 'Warehouse Building B', 'QR-WH-BLDG-B-104', 25.563500, 83.979100, 60, 'Inspect rear loading dock doors and fire extinguishers'),
  ('chk-105', 'Emergency South Exit', 'QR-EMERG-SOUTH-105', 25.562900, 83.977200, 30, 'Ensure exit path is unobstructed and panic bar functions')
ON CONFLICT (checkpoint_id) DO NOTHING;

INSERT INTO duty_allocation (duty_id, user_id, station_in_charge_id, shift_name, start_time, end_time, checkpoints_list, sms_status, status)
VALUES 
  ('duty-801', 'usr-001', 'usr-003', 'Day Shift - Sector Alpha', NOW() - INTERVAL '2 HOURS', NOW() + INTERVAL '6 HOURS', 'chk-101,chk-102,chk-103,chk-104', 'Delivered', 'In Progress'),
  ('duty-802', 'usr-002', 'usr-003', 'Night Watch - Sector Bravo', NOW() + INTERVAL '8 HOURS', NOW() + INTERVAL '16 HOURS', 'chk-103,chk-104,chk-105', 'Dispatched', 'Assigned')
ON CONFLICT (duty_id) DO NOTHING;

INSERT INTO scan_logs (scan_id, checkpoint_id, user_id, duty_id, scan_time, status, latitude, longitude, notes, qr_id, thana_name, patrol_status)
VALUES 
  ('scn-9001', 'chk-101', 'usr-001', 'duty-801', NOW() - INTERVAL '105 MINUTES', 'On-Time', 25.564700, 83.977700, 'Gate clear, all visitor entries recorded', 'QR-GATE-MAIN-101', 'Buxar Town Thana', 'Active Patrol'),
  ('scn-9002', 'chk-102', 'usr-001', 'duty-801', NOW() - INTERVAL '60 MINUTES', 'Out of Range', 25.565807, 83.983709, 'Perimeter lights checked. All normal.', 'QR-PERIM-NORTH-102', 'Buxar Industrial Thana', 'Out of Range Warning'),
  ('scn-9003', 'chk-103', 'usr-001', 'duty-801', NOW() - INTERVAL '15 MINUTES', 'On-Time', 25.564100, 83.976900, 'Server room AC running fine at 20C', 'QR-SERVER-CTRL-103', 'Buxar Central Thana', 'Normal Patrol')
ON CONFLICT (scan_id) DO NOTHING;

INSERT INTO archive_logs (archive_id, duty_id, archived_at, summary_stats, raw_logs)
VALUES 
  ('arc-501', 'duty-790', NOW() - INTERVAL '1 DAY', '{"complianceRate": 100, "totalScans": 8, "missedScans": 0, "incidents": 0}', '[{"scanId": "scn-8801", "checkpoint": "Main Gate", "time": "Yesterday 18:00"}]')
ON CONFLICT (archive_id) DO NOTHING;
