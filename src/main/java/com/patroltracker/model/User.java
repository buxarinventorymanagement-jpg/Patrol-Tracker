package com.patroltracker.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "user_table")
public class User {

    @Id
    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 50)
    private String role; // Guard, Supervisor, Admin

    @Column(name = "badge_number", length = 50)
    private String badgeNumber;

    @Column(length = 255)
    private String password;

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @Column(length = 100)
    private String designation = "Constable (PC)"; // Sub-Inspector (SI), Head Constable (HC), Constable (PC), etc.

    @Column(length = 50)
    private String status; // Active, On Patrol, Off Duty

    @Column(name = "thana_name", length = 128)
    private String thanaName = "Buxar Town Thana";

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public User() {
        this.createdAt = OffsetDateTime.now();
    }

    public User(String userId, String name, String role, String badgeNumber, String password, String phoneNumber, String status) {
        this(userId, name, role, badgeNumber, password, phoneNumber, "Constable (PC)", status);
    }

    public User(String userId, String name, String role, String badgeNumber, String password, String phoneNumber, String designation, String status) {
        this.userId = userId;
        this.name = name;
        this.role = role;
        this.badgeNumber = badgeNumber;
        this.password = password != null ? password : "password123";
        this.phoneNumber = phoneNumber != null ? phoneNumber : "+91-9876543210";
        this.designation = designation != null ? designation : "Constable (PC)";
        this.status = status;
        this.createdAt = OffsetDateTime.now();
    }

    // Legacy constructor compatibility
    public User(String userId, String name, String role, String badgeNumber, String status) {
        this(userId, name, role, badgeNumber, "password123", "+91-9876543210", "Constable (PC)", status);
    }

    public User(String userId, String name, String role, String badgeNumber, String designation, String status) {
        this(userId, name, role, badgeNumber, "password123", "+91-9876543210", designation, status);
    }

    public User(String userId, String name, String role, String badgeNumber, String password, String phoneNumber, String designation, String thanaName, String status) {
        this.userId = userId;
        this.name = name;
        this.role = role;
        this.badgeNumber = badgeNumber;
        this.password = password != null ? password : "password123";
        this.phoneNumber = phoneNumber != null ? phoneNumber : "+91-9876543210";
        this.designation = designation != null ? designation : "Constable (PC)";
        this.thanaName = thanaName != null && !thanaName.isBlank() ? thanaName : "Buxar Town Thana";
        this.status = status;
        this.createdAt = OffsetDateTime.now();
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getBadgeNumber() { return badgeNumber; }
    public void setBadgeNumber(String badgeNumber) { this.badgeNumber = badgeNumber; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getThanaName() { return thanaName; }
    public void setThanaName(String thanaName) { this.thanaName = thanaName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isSuperintendentOfPolice() {
        if ("Admin".equalsIgnoreCase(role) || "Patrol Duty Monitor".equalsIgnoreCase(role)) return true;
        if (designation == null) return false;
        String d = designation.toLowerCase().trim();
        return d.contains("superintendent of police") || d.equals("sp") || d.contains("(sp)");
    }

    public boolean isStationHouseOfficer() {
        if (isSuperintendentOfPolice() || "Supervisor".equalsIgnoreCase(role)) return true;
        if (designation == null) return false;
        String d = designation.toLowerCase().trim();
        return d.contains("sho") || d.contains("station house officer") || d.contains("c.i.") || d.contains("circle inspector") || d.contains("dysp") || d.contains("inspector");
    }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
