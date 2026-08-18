package com.patroltracker.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "duty_allocation")
public class DutyAllocation {

    @Id
    @Column(name = "duty_id", length = 64)
    private String dutyId;

    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(name = "station_in_charge_id", length = 64)
    private String stationInChargeId;

    @Column(name = "shift_name", nullable = false)
    private String shiftName;

    @Column(name = "start_time")
    private OffsetDateTime startTime;

    @Column(name = "end_time")
    private OffsetDateTime endTime;

    @Column(name = "checkpoints_list", nullable = false, columnDefinition = "TEXT")
    private String checkpointsList;

    @Column(name = "sms_status", length = 50)
    private String smsStatus = "Dispatched"; // Dispatched, Delivered, Pending

    @Column(length = 50)
    private String status = "Assigned"; // Assigned, In Progress, Completed, Missed

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public DutyAllocation() {
        this.createdAt = OffsetDateTime.now();
    }

    public DutyAllocation(String dutyId, String userId, String stationInChargeId, String shiftName, OffsetDateTime startTime, OffsetDateTime endTime, String checkpointsList, String smsStatus, String status) {
        this.dutyId = dutyId;
        this.userId = userId;
        this.stationInChargeId = stationInChargeId != null ? stationInChargeId : "usr-003";
        this.shiftName = shiftName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.checkpointsList = checkpointsList;
        this.smsStatus = smsStatus != null ? smsStatus : "Dispatched";
        this.status = status;
        this.createdAt = OffsetDateTime.now();
    }

    public DutyAllocation(String dutyId, String userId, String shiftName, OffsetDateTime startTime, OffsetDateTime endTime, String checkpointsList, String status) {
        this(dutyId, userId, "usr-003", shiftName, startTime, endTime, checkpointsList, "Dispatched", status);
    }

    // Getters and Setters
    public String getDutyId() { return dutyId; }
    public void setDutyId(String dutyId) { this.dutyId = dutyId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getStationInChargeId() { return stationInChargeId; }
    public void setStationInChargeId(String stationInChargeId) { this.stationInChargeId = stationInChargeId; }

    public String getShiftName() { return shiftName; }
    public void setShiftName(String shiftName) { this.shiftName = shiftName; }

    public OffsetDateTime getStartTime() { return startTime; }
    public void setStartTime(OffsetDateTime startTime) { this.startTime = startTime; }

    public OffsetDateTime getEndTime() { return endTime; }
    public void setEndTime(OffsetDateTime endTime) { this.endTime = endTime; }

    public String getCheckpointsList() { return checkpointsList; }
    public void setCheckpointsList(String checkpointsList) { this.checkpointsList = checkpointsList; }

    public String getSmsStatus() { return smsStatus; }
    public void setSmsStatus(String smsStatus) { this.smsStatus = smsStatus; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
