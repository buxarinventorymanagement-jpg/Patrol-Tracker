package com.patroltracker.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "archive_logs")
public class ArchiveLog {

    @Id
    @Column(name = "archive_id", length = 64)
    private String archiveId;

    @Column(name = "duty_id", length = 64)
    private String dutyId;

    @Column(name = "archived_at")
    private OffsetDateTime archivedAt;

    @Column(name = "summary_stats", columnDefinition = "TEXT")
    private String summaryStats; // JSON format e.g. {"complianceRate": 100, "totalScans": 8}

    @Column(name = "raw_logs", columnDefinition = "TEXT")
    private String rawLogs; // JSON format array of raw scan logs

    public ArchiveLog() {
        this.archivedAt = OffsetDateTime.now();
    }

    public ArchiveLog(String archiveId, String dutyId, String summaryStats, String rawLogs) {
        this.archiveId = archiveId;
        this.dutyId = dutyId;
        this.archivedAt = OffsetDateTime.now();
        this.summaryStats = summaryStats;
        this.rawLogs = rawLogs;
    }

    // Getters and Setters
    public String getArchiveId() { return archiveId; }
    public void setArchiveId(String archiveId) { this.archiveId = archiveId; }

    public String getDutyId() { return dutyId; }
    public void setDutyId(String dutyId) { this.dutyId = dutyId; }

    public OffsetDateTime getArchivedAt() { return archivedAt; }
    public void setArchivedAt(OffsetDateTime archivedAt) { this.archivedAt = archivedAt; }

    public String getSummaryStats() { return summaryStats; }
    public void setSummaryStats(String summaryStats) { this.summaryStats = summaryStats; }

    public String getRawLogs() { return rawLogs; }
    public void setRawLogs(String rawLogs) { this.rawLogs = rawLogs; }
}
