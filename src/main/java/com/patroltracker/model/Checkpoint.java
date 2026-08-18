package com.patroltracker.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "checkpoints")
public class Checkpoint {

    @Id
    @Column(name = "checkpoint_id", length = 64)
    private String checkpointId;

    @Column(nullable = false)
    private String name;

    @Column(name = "qr_code_data", nullable = false, unique = true)
    private String qrCodeData;

    @Column(precision = 10, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 6)
    private BigDecimal longitude;

    @Column(name = "scan_interval_minutes")
    private Integer scanIntervalMinutes = 60;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(length = 50)
    private String status = "Active";

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public Checkpoint() {
        this.createdAt = OffsetDateTime.now();
    }

    public Checkpoint(String checkpointId, String name, String qrCodeData, BigDecimal latitude, BigDecimal longitude, Integer scanIntervalMinutes, String instructions) {
        this.checkpointId = checkpointId;
        this.name = name;
        this.qrCodeData = qrCodeData;
        this.latitude = latitude;
        this.longitude = longitude;
        this.scanIntervalMinutes = scanIntervalMinutes;
        this.instructions = instructions;
        this.status = "Active";
        this.createdAt = OffsetDateTime.now();
    }

    // Getters and Setters
    public String getCheckpointId() { return checkpointId; }
    public void setCheckpointId(String checkpointId) { this.checkpointId = checkpointId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getQrCodeData() { return qrCodeData; }
    public void setQrCodeData(String qrCodeData) { this.qrCodeData = qrCodeData; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public Integer getScanIntervalMinutes() { return scanIntervalMinutes; }
    public void setScanIntervalMinutes(Integer scanIntervalMinutes) { this.scanIntervalMinutes = scanIntervalMinutes; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
