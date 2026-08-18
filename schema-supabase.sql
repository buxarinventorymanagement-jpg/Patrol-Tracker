-- ====================================================================
-- SUPABASE POSTGRESQL SCHEMA FOR PATROL TRACKER SYSTEM
-- Tables: User_Table, Checkpoints, Duty_Allocation, Scan_Logs, Archive_Logs
-- ====================================================================

-- 1. USER TABLE (Officers, Guards, Supervisors, Admins)
CREATE TABLE IF NOT EXISTS user_table (
    user_id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'Guard', -- Guard, Supervisor, Admin
    badge_number VARCHAR(50),
    status VARCHAR(50) DEFAULT 'Active', -- Active, On Patrol, Off Duty
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
    user_id VARCHAR(64) REFERENCES user_table(user_id) ON DELETE CASCADE,
    shift_name VARCHAR(100) NOT NULL, -- Morning Shift, Evening Patrol, Night Watch
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    checkpoints_list TEXT NOT NULL, -- Comma-separated or JSON list of Checkpoint IDs
    status VARCHAR(50) DEFAULT 'Assigned', -- Assigned, In Progress, Completed, Missed
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. SCAN LOGS TABLE (Live QR scans & inspection logs)
CREATE TABLE IF NOT EXISTS scan_logs (
    scan_id VARCHAR(64) PRIMARY KEY,
    checkpoint_id VARCHAR(64) REFERENCES checkpoints(checkpoint_id) ON DELETE SET NULL,
    user_id VARCHAR(64) REFERENCES user_table(user_id) ON DELETE SET NULL,
    duty_id VARCHAR(64) REFERENCES duty_allocation(duty_id) ON DELETE SET NULL,
    scan_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'On-Time', -- On-Time, Late, Out-of-Order, Incident
    latitude NUMERIC(10, 6),
    longitude NUMERIC(10, 6),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 5. ARCHIVE LOGS TABLE (Historical reports & aggregated statistics)
CREATE TABLE IF NOT EXISTS archive_logs (
    archive_id VARCHAR(64) PRIMARY KEY,
    duty_id VARCHAR(64),
    archived_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    summary_stats TEXT, -- JSON summary (Compliance %, Total Scans, Missed Count)
    raw_logs TEXT -- JSON raw logs export
);

-- ====================================================================
-- INITIAL DEMO SEED DATA
-- ====================================================================

-- Seed Users
INSERT INTO user_table (user_id, name, role, badge_number, status)
VALUES 
  ('usr-001', 'Buxar Security Guard A', 'Guard', 'BG-9921', 'On Patrol'),
  ('usr-002', 'Officer Rahul Sharma', 'Guard', 'BG-1044', 'Active'),
  ('usr-003', 'Inspector Vikram Singh', 'Supervisor', 'BG-0001', 'Active'),
  ('usr-004', 'Admin Controller', 'Admin', 'ADM-8800', 'Active')
ON CONFLICT (user_id) DO NOTHING;

-- Seed Checkpoints
INSERT INTO checkpoints (checkpoint_id, name, qr_code_data, latitude, longitude, scan_interval_minutes, instructions)
VALUES 
  ('chk-101', 'Main Gate Entrance', 'QR-GATE-MAIN-101', 25.564700, 83.977700, 30, 'Inspect barrier gate lock, check visitor logbook'),
  ('chk-102', 'North Perimeter Fence', 'QR-PERIM-NORTH-102', 25.565800, 83.978500, 45, 'Check fence integrity and perimeter floodlights'),
  ('chk-103', 'Server & Control Room', 'QR-SERVER-CTRL-103', 25.564100, 83.976900, 15, 'Verify AC temperature and access authorization'),
  ('chk-104', 'Warehouse Building B', 'QR-WH-BLDG-B-104', 25.563500, 83.979100, 60, 'Inspect rear loading dock doors and fire extinguishers'),
  ('chk-105', 'Emergency South Exit', 'QR-EMERG-SOUTH-105', 25.562900, 83.977200, 30, 'Ensure exit path is unobstructed and panic bar functions')
ON CONFLICT (checkpoint_id) DO NOTHING;

-- Seed Duty Allocation
INSERT INTO duty_allocation (duty_id, user_id, shift_name, start_time, end_time, checkpoints_list, status)
VALUES 
  ('duty-801', 'usr-001', 'Day Shift - Sector Alpha', NOW() - INTERVAL '2 HOURS', NOW() + INTERVAL '6 HOURS', 'chk-101,chk-102,chk-103,chk-104', 'In Progress'),
  ('duty-802', 'usr-002', 'Night Watch - Sector Bravo', NOW() + INTERVAL '8 HOURS', NOW() + INTERVAL '16 HOURS', 'chk-103,chk-104,chk-105', 'Assigned')
ON CONFLICT (duty_id) DO NOTHING;

-- Seed Scan Logs
INSERT INTO scan_logs (scan_id, checkpoint_id, user_id, duty_id, scan_time, status, latitude, longitude, notes)
VALUES 
  ('scn-9001', 'chk-101', 'usr-001', 'duty-801', NOW() - INTERVAL '105 MINUTES', 'On-Time', 25.564700, 83.977700, 'Gate clear, all visitor entries recorded'),
  ('scn-9002', 'chk-102', 'usr-001', 'duty-801', NOW() - INTERVAL '60 MINUTES', 'On-Time', 25.565800, 83.978500, 'Perimeter lights checked. All normal.'),
  ('scn-9003', 'chk-103', 'usr-001', 'duty-801', NOW() - INTERVAL '15 MINUTES', 'On-Time', 25.564100, 83.976900, 'Server room AC running fine at 20C')
ON CONFLICT (scan_id) DO NOTHING;

-- Seed Archive Logs
INSERT INTO archive_logs (archive_id, duty_id, archived_at, summary_stats, raw_logs)
VALUES 
  ('arc-501', 'duty-790', NOW() - INTERVAL '1 DAY', '{"complianceRate": 100, "totalScans": 8, "missedScans": 0, "incidents": 0}', '[{"scanId": "scn-8801", "checkpoint": "Main Gate", "time": "Yesterday 18:00"}]')
ON CONFLICT (archive_id) DO NOTHING;
