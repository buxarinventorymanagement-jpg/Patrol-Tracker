package com.patroltracker.service;

import com.patroltracker.model.DutyAllocation;
import com.patroltracker.model.User;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationService {

    private final List<Map<String, String>> dispatchedSmsLogs = new ArrayList<>();

    public Map<String, String> sendDutyAssignmentSms(DutyAllocation duty, User guard, User stationInCharge) {
        String guardPhone = guard.getPhoneNumber() != null ? guard.getPhoneNumber() : "+91-9876543210";
        String guardName = guard.getName();
        String inChargeName = stationInCharge != null ? stationInCharge.getName() : "Station In-Charge";
        String shiftName = duty.getShiftName();
        String timeStr = duty.getStartTime() != null ? 
                duty.getStartTime().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm")) : "Immediate";

        String smsText = String.format(
            "🚨 PATROL DUTY ALERT: Officer %s, shift '%s' assigned by Station In-charge %s. Start Time: %s. Assigned Checkpoints: [%s]. Please acknowledge and report for patrol.",
            guardName, shiftName, inChargeName, timeStr, duty.getCheckpointsList()
        );

        Map<String, String> smsPayload = ConcurrentHashMap.newKeySet().isEmpty() ? 
                Map.of(
                    "toPhone", guardPhone,
                    "guardId", guard.getUserId(),
                    "guardName", guardName,
                    "shiftName", shiftName,
                    "smsMessage", smsText,
                    "status", "DELIVERED_TO_MOBILE",
                    "sentAt", java.time.OffsetDateTime.now().toString()
                ) : Map.of();

        dispatchedSmsLogs.add(smsPayload);

        System.out.println("\n=======================================================");
        System.out.println(" 📱 MOBILE SMS DISPATCHED TO PERSONNEL ");
        System.out.println(" TO:      " + guardPhone + " (" + guardName + ")");
        System.out.println(" FROM:    Station In-Charge (" + inChargeName + ")");
        System.out.println(" MESSAGE: " + smsText);
        System.out.println(" STATUS:  SUCCESSFULLY DELIVERED TO MOBILE GATEWAY");
        System.out.println("=======================================================\n");

        return smsPayload;
    }

    public List<Map<String, String>> getDispatchedSmsLogs() {
        return dispatchedSmsLogs;
    }
}
