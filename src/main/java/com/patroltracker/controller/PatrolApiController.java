package com.patroltracker.controller;

import com.patroltracker.model.*;
import com.patroltracker.service.PatrolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api")
public class PatrolApiController {

    @Autowired
    private PatrolService patrolService;

    // Login API
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String userId = body.get("userId");
        String password = body.get("password");

        Optional<User> userOpt = patrolService.authenticate(userId, password);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Authentication successful",
                "user", user
            ));
        } else {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "Invalid User ID or Password"
            ));
        }
    }

    // Scan Endpoint
    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> processScan(@RequestBody Map<String, Object> request) {
        String qrCodeData = (String) request.get("qrCodeData");
        String userId = (String) request.getOrDefault("userId", "usr-001");
        String dutyId = (String) request.getOrDefault("dutyId", "duty-801");
        String notes = (String) request.get("notes");

        BigDecimal lat = null;
        BigDecimal lng = null;
        if (request.containsKey("latitude") && request.get("latitude") != null) {
            lat = new BigDecimal(request.get("latitude").toString());
        }
        if (request.containsKey("longitude") && request.get("longitude") != null) {
            lng = new BigDecimal(request.get("longitude").toString());
        }

        Map<String, Object> result = patrolService.registerScan(qrCodeData, userId, dutyId, lat, lng, notes);
        return ResponseEntity.ok(result);
    }

    // Get checkpoints
    @GetMapping("/checkpoints")
    public ResponseEntity<List<Checkpoint>> getCheckpoints() {
        return ResponseEntity.ok(patrolService.getAllCheckpoints());
    }

    // Add new checkpoint
    @PostMapping("/checkpoints")
    public ResponseEntity<Checkpoint> createCheckpoint(@RequestBody Checkpoint checkpoint) {
        return ResponseEntity.ok(patrolService.saveCheckpoint(checkpoint));
    }

    // Get scan logs
    @GetMapping("/scan-logs")
    public ResponseEntity<List<ScanLog>> getScanLogs(@RequestParam(name = "userId", required = false) String userId) {
        if (userId != null && !userId.isBlank()) {
            return ResponseEntity.ok(patrolService.getScanLogsForUser(userId));
        }
        return ResponseEntity.ok(patrolService.getAllScanLogs());
    }

    // Get duties
    @GetMapping("/duties")
    public ResponseEntity<List<DutyAllocation>> getDuties(@RequestParam(name = "userId", required = false) String userId) {
        if (userId != null && !userId.isBlank()) {
            return ResponseEntity.ok(patrolService.getDutiesForUser(userId));
        }
        return ResponseEntity.ok(patrolService.getAllDuties());
    }

    // Create duty
    @PostMapping("/duties")
    public ResponseEntity<DutyAllocation> createDuty(@RequestBody DutyAllocation duty) {
        return ResponseEntity.ok(patrolService.saveDuty(duty));
    }

    // Get users
    @GetMapping("/users")
    public ResponseEntity<List<User>> getUsers(@RequestParam(name = "userId", required = false) String userId) {
        if (userId != null && !userId.isBlank()) {
            return ResponseEntity.ok(patrolService.getUsersForUser(userId));
        }
        return ResponseEntity.ok(patrolService.getAllUsers());
    }

    // Create user
    @PostMapping("/users")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        return ResponseEntity.ok(patrolService.saveUser(user));
    }

    // Analytics summary
    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics(@RequestParam(name = "userId", required = false) String userId) {
        if (userId != null && !userId.isBlank()) {
            return ResponseEntity.ok(patrolService.getPatrolAnalyticsForUser(userId));
        }
        return ResponseEntity.ok(patrolService.getPatrolAnalytics());
    }
}
