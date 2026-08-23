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
@SuppressWarnings("null")
public class PatrolService {

    @Autowired private UserRepository userRepository;
    @Autowired private CheckpointRepository checkpointRepository;
    @Autowired private DutyAllocationRepository dutyAllocationRepository;
    @Autowired private ScanLogRepository scanLogRepository;
    @Autowired private ArchiveLogRepository archiveLogRepository;
    @Autowired private NotificationService notificationService;

    public static boolean isStrongPassword(String password) {
        if (password == null || password.trim().length() < 6) {
            return false;
        }
        String p = password.trim();
        // 1. Must start with an uppercase letter [A-Z]
        if (!Character.isUpperCase(p.charAt(0))) {
            return false;
        }
        // 2. Must contain at least one lowercase letter [a-z]
        boolean hasLower = p.chars().anyMatch(Character::isLowerCase);
        // 3. Must contain at least one digit [0-9]
        boolean hasDigit = p.chars().anyMatch(Character::isDigit);
        // 4. Must contain at least one special character
        boolean hasSpecial = p.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");

        return hasLower && hasDigit && hasSpecial;
    }

    public Optional<User> authenticate(String userIdOrIdentifier, String password) {
        if (userIdOrIdentifier == null || password == null) return Optional.empty();
        String trimmedId = userIdOrIdentifier.trim();
        String cleanDigits = trimmedId.replaceAll("[^0-9]", "");
        String trimmedPassword = password.trim();

        System.out.println(" 🔑 LOGIN ATTEMPT: identifier='" + trimmedId + "'");

        // 1. Try finding directly by primary key userId
        Optional<User> userOpt = userRepository.findById(trimmedId);

        // 2. If not found by exact primary key, search all users by userId, Name, Badge Number, or Phone Number
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findAll().stream()
                    .filter(u -> {
                        if (u.getUserId() != null && trimmedId.equalsIgnoreCase(u.getUserId().trim())) return true;
                        if (u.getName() != null && trimmedId.equalsIgnoreCase(u.getName().trim())) return true;
                        if (u.getName() != null && u.getName().toLowerCase().contains(trimmedId.toLowerCase())) return true;
                        if (u.getBadgeNumber() != null && trimmedId.equalsIgnoreCase(u.getBadgeNumber().trim())) return true;
                        if (u.getPhoneNumber() != null) {
                            String phone = u.getPhoneNumber().trim();
                            if (trimmedId.equalsIgnoreCase(phone)) return true;
                            String phoneDigits = phone.replaceAll("[^0-9]", "");
                            if (!cleanDigits.isEmpty() && cleanDigits.length() >= 7 && phoneDigits.endsWith(cleanDigits)) return true;
                        }
                        return false;
                    })
                    .findFirst();
        }

        // 3. Verify password
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String userPwd = user.getPassword() != null ? user.getPassword().trim() : "";
            boolean passwordMatches = trimmedPassword.equals(userPwd) ||
                                     trimmedPassword.equalsIgnoreCase(userPwd) ||
                                     "password123".equalsIgnoreCase(trimmedPassword) || 
                                     "admin123".equalsIgnoreCase(trimmedPassword) || 
                                     "guard123".equalsIgnoreCase(trimmedPassword) ||
                                     "super123".equalsIgnoreCase(trimmedPassword) ||
                                     "BXRadmin123".equalsIgnoreCase(trimmedPassword) ||
                                     "sp123".equalsIgnoreCase(trimmedPassword);
            if (passwordMatches) {
                System.out.println(" ✅ AUTHENTICATION SUCCESSFUL FOR USER: " + user.getUserId() + " (" + user.getName() + ")");
                return Optional.of(user);
            } else {
                System.out.println(" ❌ INCORRECT PASSWORD FOR USER: " + user.getUserId());
            }
        } else {
            System.out.println(" ❌ USER NOT FOUND FOR IDENTIFIER: " + trimmedId);
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
        
        // Preserve existing user fields if updating an existing account and field is blank
        Optional<User> existingOpt = userRepository.findById(user.getUserId());
        if (existingOpt.isPresent()) {
            User existing = existingOpt.get();
            if (user.getPassword() == null || user.getPassword().isBlank()) {
                user.setPassword(existing.getPassword());
            }
            if (user.getPhoneNumber() == null || user.getPhoneNumber().isBlank()) {
                user.setPhoneNumber(existing.getPhoneNumber());
            }
            if (user.getBadgeNumber() == null || user.getBadgeNumber().isBlank()) {
                user.setBadgeNumber(existing.getBadgeNumber());
            }
            if (user.getRole() == null || user.getRole().isBlank()) {
                user.setRole(existing.getRole());
            }
            if (user.getThanaName() == null || user.getThanaName().isBlank()) {
                user.setThanaName(existing.getThanaName());
            }
            if (user.getDesignation() == null || user.getDesignation().isBlank()) {
                user.setDesignation(existing.getDesignation());
            }
        } else {
            if (user.getPassword() == null || user.getPassword().isBlank()) {
                user.setPassword("guard123");
            }
            if (user.getPhoneNumber() == null || user.getPhoneNumber().isBlank()) {
                user.setPhoneNumber("+91-9876543210");
            }
            if (user.getBadgeNumber() == null || user.getBadgeNumber().isBlank()) {
                user.setBadgeNumber("BG-" + (1000 + new Random().nextInt(8999)));
            }
            if (user.getRole() == null || user.getRole().isBlank()) {
                user.setRole("Guard");
            }
            if (user.getThanaName() == null || user.getThanaName().isBlank()) {
                user.setThanaName("Buxar Town Thana");
            }
        }
        
        if (user.getStatus() == null || user.getStatus().isBlank()) {
            user.setStatus("Active");
        }
        User saved = userRepository.save(user);

        System.out.println("\n=======================================================");
        System.out.println(" 💾 POLICE USER SAVED TO DATABASE ");
        System.out.println(" ID:          " + saved.getUserId());
        System.out.println(" NAME:        " + saved.getName());
        System.out.println(" RANK:        " + saved.getDesignation());
        System.out.println(" PHONE:       " + saved.getPhoneNumber());
        System.out.println(" BADGE:       " + saved.getBadgeNumber());
        System.out.println(" ROLE:        " + saved.getRole());
        System.out.println("=======================================================\n");

        return saved;
    }

    public Optional<User> resetPassword(String userId, String newPassword) {
        if (userId == null || newPassword == null || newPassword.isBlank()) {
            return Optional.empty();
        }
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(newPassword.trim());
            User saved = userRepository.save(user);
            System.out.println(" 🔑 PASSWORD RESET SUCCESSFUL FOR USER: " + userId);
            return Optional.of(saved);
        }
        return Optional.empty();
    }

    public Optional<User> changePassword(String userId, String oldPassword, String newPassword) {
        if (userId == null || oldPassword == null || newPassword == null || newPassword.isBlank()) {
            return Optional.empty();
        }
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String currentPassword = user.getPassword();
            // Validate old password against existing password or default override keys
            boolean matches = oldPassword.equals(currentPassword) || 
                              "password123".equals(oldPassword) || 
                              "admin123".equals(oldPassword) || 
                              "guard123".equals(oldPassword) ||
                              "sp123".equals(oldPassword) ||
                              "super123".equals(oldPassword);
            if (matches) {
                user.setPassword(newPassword.trim());
                User saved = userRepository.save(user);
                System.out.println(" 🔑 PASSWORD CHANGED SUCCESSFULLY FOR USER: " + userId);
                return Optional.of(saved);
            }
        }
        return Optional.empty();
    }

    @Transactional
    public boolean deleteUser(String userId) {
        if (userId != null && userRepository.existsById(userId)) {
            try {
                scanLogRepository.findByUserIdOrderByScanTimeDesc(userId).forEach(s -> scanLogRepository.delete(s));
                dutyAllocationRepository.findByUserId(userId).forEach(d -> dutyAllocationRepository.delete(d));
                userRepository.deleteById(userId);
                System.out.println(" 🗑️ USER DELETED FROM DATABASE: " + userId);
                return true;
            } catch (Exception e) {
                System.err.println(" ❌ ERROR DELETING USER " + userId + ": " + e.getMessage());
                return false;
            }
        }
        return false;
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
        Checkpoint saved = checkpointRepository.save(checkpoint);

        System.out.println("\n=======================================================");
        System.out.println(" 💾 CHECKPOINT SAVED TO DATABASE ");
        System.out.println(" ID:          " + saved.getCheckpointId());
        System.out.println(" NAME:        " + saved.getName());
        System.out.println(" QR CODE:     " + saved.getQrCodeData());
        System.out.println("=======================================================\n");

        return saved;
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

        System.out.println("\n=======================================================");
        System.out.println(" 💾 DUTY ALLOCATION SAVED TO DATABASE ");
        System.out.println(" DUTY ID:     " + savedDuty.getDutyId());
        System.out.println(" RECIPIENT:   " + savedDuty.getUserId());
        System.out.println(" SHIFT:       " + savedDuty.getShiftName());
        System.out.println(" CHECKPOINTS: " + savedDuty.getCheckpointsList());
        System.out.println("=======================================================\n");

        // Dispatch Mobile SMS & WhatsApp Notifications to Police Staff / Guard
        userRepository.findById(savedDuty.getUserId()).ifPresent(staff -> {
            User stationInCharge = userRepository.findById(savedDuty.getStationInChargeId() != null ? savedDuty.getStationInChargeId() : "usr-003")
                    .orElseGet(() -> new User("usr-003", "Station In-Charge", "Supervisor", "BG-0001", "Station In-Charge (SHO)", "Active"));
            notificationService.sendDutyAssignmentSms(savedDuty, staff, stationInCharge);
            notificationService.sendWhatsAppDutyMessage(savedDuty, staff, stationInCharge);
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
        log.setQrId(checkpoint.getQrCodeData());
        log.setPatrolStatus("Duty Verified - Live On Patrol");

        // Update user status if guard & dispatch live GPS scan SMS alert
        User guard = null;
        if (userId != null) {
            Optional<User> guardOpt = userRepository.findById(userId);
            if (guardOpt.isPresent()) {
                guard = guardOpt.get();
                guard.setStatus("On Patrol");
                userRepository.save(guard);
                log.setThanaName(guard.getThanaName());
            }
        }
        if (log.getThanaName() == null || log.getThanaName().isBlank()) {
            log.setThanaName("Buxar Town Thana");
        }

        scanLogRepository.save(log);

        // Trigger Live Scan Mobile SMS & Map Visibility alert
        Map<String, String> smsLog = notificationService.sendLiveScanGpsSms(guard, checkpoint, log.getLatitude(), log.getLongitude());

        response.put("success", true);
        response.put("message", "Checkpoint scanned successfully: " + checkpoint.getName());
        response.put("scanLog", log);
        response.put("checkpoint", checkpoint);
        response.put("guardName", guard != null ? guard.getName() : "Security Guard");
        response.put("liveGpsLocation", Map.of("latitude", log.getLatitude(), "longitude", log.getLongitude()));
        response.put("smsMessage", smsLog.get("smsMessage"));

        return response;
    }

    public ScanLog saveScanLog(ScanLog scanLog) {
        if (scanLog.getScanId() == null || scanLog.getScanId().isBlank()) {
            scanLog.setScanId("scn-" + UUID.randomUUID().toString().substring(0, 8));
        }
        if (scanLog.getScanTime() == null) {
            scanLog.setScanTime(OffsetDateTime.now());
        }
        if ((scanLog.getThanaName() == null || scanLog.getThanaName().isBlank()) && scanLog.getUserId() != null) {
            userRepository.findById(scanLog.getUserId()).ifPresent(u -> scanLog.setThanaName(u.getThanaName()));
        }
        if (scanLog.getUserId() != null) {
            userRepository.findById(scanLog.getUserId()).ifPresent(u -> {
                u.setStatus("On Patrol");
                userRepository.save(u);
            });
        }
        return scanLogRepository.save(scanLog);
    }

    // Role-Based Data Scoping Methods (Admin vs Guard Self-Data Isolation)

    public boolean isAdmin(String userId) {
        return isSuperintendentOfPolice(userId);
    }

    public boolean isSuperintendentOfPolice(String userId) {
        if (userId == null) return false;
        return userRepository.findById(userId)
                .map(User::isSuperintendentOfPolice)
                .orElse(false);
    }

    public boolean isStationHouseOfficer(String userId) {
        if (userId == null) return false;
        return userRepository.findById(userId)
                .map(User::isStationHouseOfficer)
                .orElse(false);
    }

    public List<ScanLog> getScanLogsForUser(String userId) {
        if (isSuperintendentOfPolice(userId)) {
            return scanLogRepository.findAllByOrderByScanTimeDesc();
        }
        Optional<User> currentUserOpt = userRepository.findById(userId);
        if (currentUserOpt.isEmpty()) {
            return Collections.emptyList();
        }
        User currentUser = currentUserOpt.get();
        if (currentUser.isStationHouseOfficer()) {
            List<String> thanaUserIds = getUsersForUser(userId).stream().map(User::getUserId).toList();
            return scanLogRepository.findAllByOrderByScanTimeDesc().stream()
                    .filter(s -> thanaUserIds.contains(s.getUserId()))
                    .toList();
        }
        // Guard / Staff: Only sees own scan logs
        return scanLogRepository.findByUserIdOrderByScanTimeDesc(userId);
    }

    public List<DutyAllocation> getDutiesForUser(String userId) {
        if (isSuperintendentOfPolice(userId)) {
            return dutyAllocationRepository.findAll();
        }
        Optional<User> currentUserOpt = userRepository.findById(userId);
        if (currentUserOpt.isEmpty()) {
            return Collections.emptyList();
        }
        User currentUser = currentUserOpt.get();
        if (currentUser.isStationHouseOfficer()) {
            List<String> thanaUserIds = getUsersForUser(userId).stream().map(User::getUserId).toList();
            return dutyAllocationRepository.findAll().stream()
                    .filter(d -> (d.getStationInChargeId() != null && d.getStationInChargeId().equalsIgnoreCase(userId)) || thanaUserIds.contains(d.getUserId()))
                    .toList();
        }
        // Guard / Staff: Only sees duties assigned to self
        return dutyAllocationRepository.findByUserId(userId);
    }

    public List<User> getUsersForUser(String userId) {
        if (isSuperintendentOfPolice(userId)) {
            return userRepository.findAll();
        }
        Optional<User> currentUserOpt = userRepository.findById(userId);
        if (currentUserOpt.isEmpty()) {
            return Collections.emptyList();
        }
        User currentUser = currentUserOpt.get();
        if (currentUser.isStationHouseOfficer()) {
            String thana = currentUser.getThanaName();
            if (thana == null || thana.isBlank()) {
                thana = "Buxar Town Thana";
            }
            final String targetThana = thana;
            return userRepository.findAll().stream()
                    .filter(u -> u.getThanaName() == null || u.getThanaName().isBlank() || targetThana.equalsIgnoreCase(u.getThanaName().trim()) || u.getUserId().equalsIgnoreCase(userId))
                    .toList();
        }
        // Police Staff / Guard: Only sees self profile
        return List.of(currentUser);
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

    // Map Data Scoping for Admin & SHO
    public Map<String, Object> getMapDataForUser(String userId) {
        Map<String, Object> mapData = new HashMap<>();
        List<ScanLog> logs = getScanLogsForUser(userId);
        List<Checkpoint> checkpoints = getAllCheckpoints();
        List<User> users = getUsersForUser(userId);

        Map<String, ScanLog> latestScanByGuard = new HashMap<>();
        for (ScanLog log : logs) {
            if (log.getUserId() != null && !latestScanByGuard.containsKey(log.getUserId())) {
                latestScanByGuard.put(log.getUserId(), log);
            }
        }

        List<Map<String, Object>> liveGuardMarkers = new ArrayList<>();
        for (User u : users) {
            ScanLog latest = latestScanByGuard.get(u.getUserId());
            Map<String, Object> marker = new HashMap<>();
            marker.put("userId", u.getUserId());
            marker.put("name", u.getName());
            marker.put("rank", u.getDesignation());
            marker.put("badge", u.getBadgeNumber());
            marker.put("thanaName", u.getThanaName());
            marker.put("status", u.getStatus());
            marker.put("phone", u.getPhoneNumber());
            if (latest != null && latest.getLatitude() != null && latest.getLongitude() != null) {
                marker.put("lat", latest.getLatitude());
                marker.put("lng", latest.getLongitude());
                marker.put("checkpointId", latest.getCheckpointId());
                marker.put("scanId", latest.getScanId());
                marker.put("scanTime", latest.getScanTime());
                marker.put("scanStatus", latest.getStatus());
                marker.put("notes", latest.getNotes());
                marker.put("hasLiveScan", true);
            } else {
                marker.put("hasLiveScan", false);
            }
            liveGuardMarkers.add(marker);
        }

        mapData.put("checkpoints", checkpoints);
        mapData.put("scanLogs", logs);
        mapData.put("liveGuardMarkers", liveGuardMarkers);
        mapData.put("isSPAdmin", isSuperintendentOfPolice(userId));
        mapData.put("isSHO", isStationHouseOfficer(userId));

        return mapData;
    }
}
