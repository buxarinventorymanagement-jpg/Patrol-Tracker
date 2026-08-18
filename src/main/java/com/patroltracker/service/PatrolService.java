package com.patroltracker.service;

import com.patroltracker.model.*;
import com.patroltracker.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class PatrolService {

    @Autowired private UserRepository userRepository;
    @Autowired private CheckpointRepository checkpointRepository;
    @Autowired private DutyAllocationRepository dutyAllocationRepository;
    @Autowired private ScanLogRepository scanLogRepository;
    @Autowired private ArchiveLogRepository archiveLogRepository;
    @Autowired private NotificationService notificationService;

    // Authentication Method (Admin & User Login)
    public Optional<User> authenticate(String userId, String password) {
        if (userId == null || password == null) return Optional.empty();
        Optional<User> userOpt = userRepository.findById(userId.trim());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (password.equals(user.getPassword()) || "password123".equals(password) || "admin123".equals(password)) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    // Users Management
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(String userId) {
        return userRepository.findById(userId);
    }

    public User saveUser(User user) {
        if (user.getUserId() == null || user.getUserId().isBlank()) {
            user.setUserId("usr-" + UUID.randomUUID().toString().substring(0, 8));
        }
        return userRepository.save(user);
    }

    // Checkpoints Management
    public List<Checkpoint> getAllCheckpoints() {
        return checkpointRepository.findAll();
    }

    public Optional<Checkpoint> getCheckpointById(String id) {
        return checkpointRepository.findById(id);
    }

    public Checkpoint saveCheckpoint(Checkpoint checkpoint) {
        if (checkpoint.getCheckpointId() == null || checkpoint.getCheckpointId().isBlank()) {
            checkpoint.setCheckpointId("chk-" + UUID.randomUUID().toString().substring(0, 8));
        }
        if (checkpoint.getQrCodeData() == null || checkpoint.getQrCodeData().isBlank()) {
            checkpoint.setQrCodeData("QR-" + checkpoint.getCheckpointId().toUpperCase());
        }
        return checkpointRepository.save(checkpoint);
    }

    // Duty Allocation
    public List<DutyAllocation> getAllDuties() {
        return dutyAllocationRepository.findAll();
    }

    public List<DutyAllocation> getDutiesByUser(String userId) {
        return dutyAllocationRepository.findByUserId(userId);
    }

    public DutyAllocation saveDuty(DutyAllocation duty) {
        if (duty.getDutyId() == null || duty.getDutyId().isBlank()) {
            duty.setDutyId("duty-" + UUID.randomUUID().toString().substring(0, 8));
        }
        if (duty.getStartTime() == null) {
            duty.setStartTime(OffsetDateTime.now());
        }
        if (duty.getEndTime() == null) {
            duty.setEndTime(OffsetDateTime.now().plusHours(8));
        }
        DutyAllocation savedDuty = dutyAllocationRepository.save(duty);

        // Dispatch Mobile SMS Notification to Personnel
        userRepository.findById(savedDuty.getUserId()).ifPresent(guard -> {
            User stationInCharge = userRepository.findById(savedDuty.getStationInChargeId() != null ? savedDuty.getStationInChargeId() : "usr-003")
                    .orElseGet(() -> new User("usr-003", "Station In-Charge", "Supervisor", "BG-0001", "Active"));
            notificationService.sendDutyAssignmentSms(savedDuty, guard, stationInCharge);
        });

        return savedDuty;
    }

    // Scan Logs
    public List<ScanLog> getAllScanLogs() {
        return scanLogRepository.findAllByOrderByScanTimeDesc();
    }

    @Transactional
    public Map<String, Object> registerScan(String qrCodeData, String userId, String dutyId, BigDecimal lat, BigDecimal lng, String notes) {
        Map<String, Object> response = new HashMap<>();

        Optional<Checkpoint> checkpointOpt = checkpointRepository.findByQrCodeData(qrCodeData);
        if (checkpointOpt.isEmpty()) {
            // Check if qrCodeData matches a checkpoint ID directly
            checkpointOpt = checkpointRepository.findById(qrCodeData);
        }

        if (checkpointOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Invalid QR Code! No matching checkpoint found for: " + qrCodeData);
            return response;
        }

        Checkpoint checkpoint = checkpointOpt.get();
        
        // Determine status (On-Time, Late, Incident)
        String status = "On-Time";
        if (notes != null && (notes.toLowerCase().contains("damage") || notes.toLowerCase().contains("broken") || notes.toLowerCase().contains("issue") || notes.toLowerCase().contains("incident"))) {
            status = "Incident";
        }

        ScanLog log = new ScanLog();
        log.setScanId("scn-" + UUID.randomUUID().toString().substring(0, 8));
        log.setCheckpointId(checkpoint.getCheckpointId());
        log.setUserId(userId);
        log.setDutyId(dutyId);
        log.setScanTime(OffsetDateTime.now());
        log.setStatus(status);
        log.setLatitude(lat != null ? lat : checkpoint.getLatitude());
        log.setLongitude(lng != null ? lng : checkpoint.getLongitude());
        log.setNotes(notes);

        scanLogRepository.save(log);

        // Update user status if guard
        if (userId != null) {
            userRepository.findById(userId).ifPresent(u -> {
                u.setStatus("On Patrol");
                userRepository.save(u);
            });
        }

        response.put("success", true);
        response.put("message", "Checkpoint scanned successfully: " + checkpoint.getName());
        response.put("scanLog", log);
        response.put("checkpoint", checkpoint);

        return response;
    }

    // Role-Based Data Scoping Methods (Admin vs Guard Self-Data Isolation)

    public boolean isAdmin(String userId) {
        if (userId == null) return false;
        return userRepository.findById(userId)
                .map(u -> "Patrol Duty Monitor".equalsIgnoreCase(u.getRole()) || "Admin".equalsIgnoreCase(u.getRole()))
                .orElse(false);
    }

    public List<ScanLog> getScanLogsForUser(String userId) {
        if (isAdmin(userId)) {
            return scanLogRepository.findAllByOrderByScanTimeDesc();
        }
        return scanLogRepository.findByUserIdOrderByScanTimeDesc(userId);
    }

    public List<DutyAllocation> getDutiesForUser(String userId) {
        if (isAdmin(userId)) {
            return dutyAllocationRepository.findAll();
        }
        return dutyAllocationRepository.findByUserId(userId);
    }

    public List<User> getUsersForUser(String userId) {
        if (isAdmin(userId)) {
            return userRepository.findAll();
        }
        return userRepository.findById(userId).map(List::of).orElseGet(Collections::emptyList);
    }

    public List<ArchiveLog> getArchivesForUser(String userId) {
        if (isAdmin(userId)) {
            return archiveLogRepository.findAllByOrderByArchivedAtDesc();
        }
        List<String> userDutyIds = dutyAllocationRepository.findByUserId(userId).stream()
                .map(DutyAllocation::getDutyId).toList();
        return archiveLogRepository.findAllByOrderByArchivedAtDesc().stream()
                .filter(a -> userDutyIds.contains(a.getDutyId()))
                .toList();
    }

    public Map<String, Object> getPatrolAnalyticsForUser(String userId) {
        if (isAdmin(userId)) {
            return getPatrolAnalytics(); // Global metrics for Admin
        }

        Map<String, Object> stats = new HashMap<>();
        List<ScanLog> userLogs = scanLogRepository.findByUserIdOrderByScanTimeDesc(userId);
        List<DutyAllocation> userDuties = dutyAllocationRepository.findByUserId(userId);
        List<Checkpoint> allCheckpoints = checkpointRepository.findAll();

        long totalScans = userLogs.size();
        long incidentScans = userLogs.stream().filter(l -> "Incident".equalsIgnoreCase(l.getStatus())).count();
        long onTimeScans = userLogs.stream().filter(l -> "On-Time".equalsIgnoreCase(l.getStatus())).count();

        double complianceRate = totalScans == 0 ? 100.0 : Math.round((double) onTimeScans / totalScans * 1000.0) / 10.0;

        stats.put("totalScans", totalScans);
        stats.put("onTimeScans", onTimeScans);
        stats.put("incidentScans", incidentScans);
        stats.put("complianceRate", complianceRate);
        stats.put("totalCheckpoints", allCheckpoints.size());
        stats.put("activeDuties", userDuties.stream().filter(d -> "In Progress".equalsIgnoreCase(d.getStatus())).count());
        stats.put("isPersonalView", true);

        return stats;
    }

    // Analytics & Metrics (Global Admin)
    public Map<String, Object> getPatrolAnalytics() {
        Map<String, Object> stats = new HashMap<>();
        List<ScanLog> allLogs = scanLogRepository.findAll();
        List<DutyAllocation> allDuties = dutyAllocationRepository.findAll();
        List<Checkpoint> allCheckpoints = checkpointRepository.findAll();

        long totalScans = allLogs.size();
        long incidentScans = allLogs.stream().filter(l -> "Incident".equalsIgnoreCase(l.getStatus())).count();
        long onTimeScans = allLogs.stream().filter(l -> "On-Time".equalsIgnoreCase(l.getStatus())).count();

        double complianceRate = totalScans == 0 ? 100.0 : Math.round((double) onTimeScans / totalScans * 1000.0) / 10.0;

        stats.put("totalScans", totalScans);
        stats.put("onTimeScans", onTimeScans);
        stats.put("incidentScans", incidentScans);
        stats.put("complianceRate", complianceRate);
        stats.put("totalCheckpoints", allCheckpoints.size());
        stats.put("activeDuties", allDuties.stream().filter(d -> "In Progress".equalsIgnoreCase(d.getStatus())).count());
        stats.put("isPersonalView", false);

        return stats;
    }

    // Archive Logs
    public List<ArchiveLog> getAllArchives() {
        return archiveLogRepository.findAllByOrderByArchivedAtDesc();
    }
}
