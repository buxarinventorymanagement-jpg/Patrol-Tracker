package com.patroltracker.config;

import com.patroltracker.model.*;
import com.patroltracker.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private UserRepository userRepository;
    @Autowired private CheckpointRepository checkpointRepository;
    @Autowired private DutyAllocationRepository dutyAllocationRepository;
    @Autowired private ScanLogRepository scanLogRepository;
    @Autowired private ArchiveLogRepository archiveLogRepository;

    @Override
    @SuppressWarnings("null")
    public void run(String... args) throws Exception {
        // Migration: Ensure thanaName is set for existing database users
        userRepository.findAll().forEach(u -> {
            if (u.getName() != null && u.getName().toLowerCase().contains("anshuman")) {
                u.setName("Inspector Vikram Singh");
                userRepository.save(u);
            }
            if (u.getThanaName() == null || u.getThanaName().isBlank()) {
                u.setThanaName("Buxar Town Thana");
                userRepository.save(u);
            }
        });

        if (userRepository.count() == 0) {
            System.out.println(">>> Initializing Patrol Tracker Demo Seed Data with Login Credentials & SMS Contact Info...");

            // Seed Initial Clean Command Accounts (SP Admin, SHO, & Police Staff)
            userRepository.saveAll(List.of(
                new User("sp-admin", "Dr. Rajesh Kumar, IPS", "Admin", "SP-0001", "sp123", "+91-9990001112", "Superintendent of Police (SP)", "District Police HQ", "Active"),
                new User("usr-003", "Inspector Vikram Singh", "Supervisor", "SHO-1001", "super123", "+91-9998887770", "Station House Officer (SHO)", "Buxar Town Thana", "Active"),
                new User("usr-001", "Constable Ramesh Sharma", "Guard", "PC-1001", "guard123", "+91-9876543210", "Constable (PC)", "Buxar Town Thana", "Active"),
                new User("usr-002", "Constable Amit Verma", "Guard", "PC-1002", "guard123", "+91-9876543211", "Constable (PC)", "Buxar Industrial Thana", "Active"),
                new User("Patrol Tracker", "District Police Monitor", "Admin", "ADM-8800", "BXRadmin123", "+91-9990001112", "Superintendent of Police (SP)", "District Police HQ", "Active")
            ));

            // Seed Checkpoints
            checkpointRepository.saveAll(List.of(
                new Checkpoint("chk-101", "Main Gate Entrance", "QR-GATE-MAIN-101", new BigDecimal("25.564700"), new BigDecimal("83.977700"), 30, "Inspect barrier gate lock, check visitor logbook"),
                new Checkpoint("chk-102", "North Perimeter Fence", "QR-PERIM-NORTH-102", new BigDecimal("25.565800"), new BigDecimal("83.978500"), 45, "Check fence integrity and perimeter floodlights"),
                new Checkpoint("chk-103", "Server & Control Room", "QR-SERVER-CTRL-103", new BigDecimal("25.564100"), new BigDecimal("83.976900"), 15, "Verify AC temperature and access authorization"),
                new Checkpoint("chk-104", "Warehouse Building B", "QR-WH-BLDG-B-104", new BigDecimal("25.563500"), new BigDecimal("83.979100"), 60, "Inspect rear loading dock doors and fire extinguishers"),
                new Checkpoint("chk-105", "Emergency South Exit", "QR-EMERG-SOUTH-105", new BigDecimal("25.562900"), new BigDecimal("83.977200"), 30, "Ensure exit path is unobstructed and panic bar functions")
            ));

            // Seed Duty Allocations
            dutyAllocationRepository.saveAll(List.of(
                new DutyAllocation("duty-801", "usr-001", "usr-003", "Day Shift - Sector Alpha", OffsetDateTime.now().minusHours(2), OffsetDateTime.now().plusHours(6), "chk-101,chk-102,chk-103,chk-104", "Delivered", "In Progress"),
                new DutyAllocation("duty-802", "usr-002", "usr-003", "Night Watch - Sector Bravo", OffsetDateTime.now().plusHours(8), OffsetDateTime.now().plusHours(16), "chk-103,chk-104,chk-105", "Dispatched", "Assigned")
            ));

            // Seed Scan Logs
            ScanLog s1 = new ScanLog("scn-9001", "chk-101", "usr-001", "duty-801", "On-Time", new BigDecimal("25.564700"), new BigDecimal("83.977700"), "Gate clear, all visitor entries recorded");
            s1.setQrId("QR-GATE-MAIN-101");
            s1.setThanaName("Buxar Town Thana");
            s1.setPatrolStatus("Active Patrol");

            ScanLog s2 = new ScanLog("scn-9002", "chk-102", "usr-001", "duty-801", "Out of Range", new BigDecimal("25.565807"), new BigDecimal("83.983709"), "Perimeter lights checked. All normal.");
            s2.setQrId("QR-PERIM-NORTH-102");
            s2.setThanaName("Buxar Industrial Thana");
            s2.setPatrolStatus("Out of Range Warning");

            ScanLog s3 = new ScanLog("scn-9003", "chk-103", "usr-001", "duty-801", "On-Time", new BigDecimal("25.564100"), new BigDecimal("83.976900"), "Server room AC running fine at 20C");
            s3.setQrId("QR-SERVER-CTRL-103");
            s3.setThanaName("Buxar Central Thana");
            s3.setPatrolStatus("Normal Patrol");

            scanLogRepository.saveAll(List.of(s1, s2, s3));

            // Seed Archive Logs
            archiveLogRepository.saveAll(List.of(
                new ArchiveLog("arc-501", "duty-790", "{\"complianceRate\": 100, \"totalScans\": 8, \"missedScans\": 0, \"incidents\": 0}", "[{\"scanId\": \"scn-8801\", \"checkpoint\": \"Main Gate\", \"time\": \"Yesterday 18:00\"}]")
            ));

            System.out.println(">>> Demo Seed Data Initialized Successfully!");
        }
    }
}
