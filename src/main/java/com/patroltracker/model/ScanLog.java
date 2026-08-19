package com.patroltracker.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "scan_logs")
public class ScanLog {

    @Id
    @Column(name = "scan_id", length = 64)
    private String scanId;

    @Column(name = "checkpoint_id", length = 64)
    private String checkpointId;

    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(name = "duty_id", length = 64)
    private String dutyId;

    @Column(name = "scan_time")
    private OffsetDateTime scanTime;

    @Column(nullable = false, length = 50)
    private String status = "On-Time"; // On-Time, Late, Out-of-Order, Incident

    @Column(precision = 10, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 6)
    private BigDecimal longitude;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "qr_id", length = 128)
    private String qrId;

    @Column(name = "thana_name", length = 128)
    private String thanaName;

    @Column(name = "photo_proof", columnDefinition = "TEXT")
    private String photoProof;

    @Column(name = "patrol_status", length = 128)
    private String patrolStatus;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public ScanLog() {
        this.scanTime = OffsetDateTime.now();
        this.createdAt = OffsetDateTime.now();
    }

    public ScanLog(String scanId, String checkpointId, String userId, String dutyId, String status, BigDecimal latitude, BigDecimal longitude, String notes) {
        this.scanId = scanId;
        this.checkpointId = checkpointId;
        this.userId = userId;
        this.dutyId = dutyId;
        this.scanTime = OffsetDateTime.now();
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
        this.notes = notes;
        this.createdAt = OffsetDateTime.now();
    }

    // Getters and Setters
    public String getScanId() { return scanId; }
    public void setScanId(String scanId) { this.scanId = scanId; }

    public String getCheckpointId() { return checkpointId; }
    public void setCheckpointId(String checkpointId) { this.checkpointId = checkpointId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getDutyId() { return dutyId; }
    public void setDutyId(String dutyId) { this.dutyId = dutyId; }

    public OffsetDateTime getScanTime() { return scanTime; }
    public void setScanTime(OffsetDateTime scanTime) { this.scanTime = scanTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getQrId() { return qrId; }
    public void setQrId(String qrId) { this.qrId = qrId; }

    public String getThanaName() { return thanaName; }
    public void setThanaName(String thanaName) { this.thanaName = thanaName; }

    public String getPhotoProof() { return photoProof; }
    public void setPhotoProof(String photoProof) { this.photoProof = photoProof; }

    public String getPatrolStatus() { return patrolStatus; }
    public void setPatrolStatus(String patrolStatus) { this.patrolStatus = patrolStatus; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
