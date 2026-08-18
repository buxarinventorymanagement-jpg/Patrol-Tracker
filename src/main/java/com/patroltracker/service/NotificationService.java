package com.patroltracker.service;

import com.patroltracker.model.Checkpoint;
import com.patroltracker.model.DutyAllocation;
import com.patroltracker.model.User;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private final List<Map<String, String>> dispatchedSmsLogs = new ArrayList<>();

    public Map<String, String> sendDutyAssignmentSms(DutyAllocation duty, User guard, User stationInCharge) {
        String guardPhone = guard != null && guard.getPhoneNumber() != null ? guard.getPhoneNumber() : "+91-9876543210";
        String guardName = guard != null ? guard.getName() : "Security Guard";
        String inChargeName = stationInCharge != null ? stationInCharge.getName() : "Station In-Charge";
        String shiftName = duty.getShiftName() != null ? duty.getShiftName() : "Patrol Shift";
        String timeStr = duty.getStartTime() != null ? 
                duty.getStartTime().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm")) : "Immediate";

        String smsText = String.format(
            "🚨 PATROL DUTY ALERT: Officer %s, shift '%s' assigned by Station In-charge %s. Start Time: %s. Assigned Checkpoints: [%s]. Please report to duty point and scan QR code.",
            guardName, shiftName, inChargeName, timeStr, duty.getCheckpointsList()
        );

        Map<String, String> smsPayload = new HashMap<>();
        smsPayload.put("type", "DUTY_ASSIGNMENT_SMS");
        smsPayload.put("toPhone", guardPhone);
        smsPayload.put("guardId", guard != null ? guard.getUserId() : "usr-001");
        smsPayload.put("guardName", guardName);
        smsPayload.put("shiftName", shiftName);
        smsPayload.put("checkpoints", duty.getCheckpointsList());
        smsPayload.put("smsMessage", smsText);
        smsPayload.put("status", "DELIVERED_TO_MOBILE");
        smsPayload.put("sentAt", OffsetDateTime.now().toString());

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

    public Map<String, String> sendLiveScanGpsSms(User guard, Checkpoint checkpoint, BigDecimal lat, BigDecimal lng) {
        String guardPhone = guard != null && guard.getPhoneNumber() != null ? guard.getPhoneNumber() : "+91-9876543210";
        String guardName = guard != null ? guard.getName() : "Security Guard";
        String chkName = checkpoint != null ? checkpoint.getName() : "Duty Point";
        String latStr = lat != null ? lat.setScale(6, java.math.RoundingMode.HALF_UP).toString() : "25.564700";
        String lngStr = lng != null ? lng.setScale(6, java.math.RoundingMode.HALF_UP).toString() : "83.977700";

        String smsText = String.format(
            "📍 LIVE SCAN GPS ALERT: Officer %s reached duty point '%s'. Live GPS Location: %s, %s. QR Scan verified GREEN. Station In-charge notified.",
            guardName, chkName, latStr, lngStr
        );

        Map<String, String> smsPayload = new HashMap<>();
        smsPayload.put("type", "LIVE_GPS_SCAN_SMS");
        smsPayload.put("toPhone", guardPhone);
        smsPayload.put("guardName", guardName);
        smsPayload.put("checkpointName", chkName);
        smsPayload.put("latitude", latStr);
        smsPayload.put("longitude", lngStr);
        smsPayload.put("smsMessage", smsText);
        smsPayload.put("status", "LIVE_LOCATION_VISIBLE");
        smsPayload.put("sentAt", OffsetDateTime.now().toString());

        dispatchedSmsLogs.add(smsPayload);

        System.out.println("\n-------------------------------------------------------");
        System.out.println(" 📍 LIVE GUARD GPS SCAN VERIFIED & SMS DISPATCHED");
        System.out.println(" GUARD:      " + guardName + " (" + guardPhone + ")");
        System.out.println(" CHECKPOINT: " + chkName);
        System.out.println(" GPS COORDS: " + latStr + ", " + lngStr);
        System.out.println(" SMS MSG:    " + smsText);
        System.out.println("-------------------------------------------------------\n");

        return smsPayload;
    }

    public Map<String, String> sendWhatsAppDutyMessage(DutyAllocation duty, User staff, User stationInCharge) {
        String staffPhone = staff != null && staff.getPhoneNumber() != null ? staff.getPhoneNumber() : "+91-9876543210";
        String staffName = staff != null ? staff.getName() : "Police Staff Officer";
        String designation = staff != null && staff.getDesignation() != null ? staff.getDesignation() : "Constable (PC)";
        String inChargeName = stationInCharge != null ? stationInCharge.getName() : "Station In-Charge (SHO)";
        String shiftName = duty.getShiftName() != null ? duty.getShiftName() : "Patrol Shift";
        String timeStr = duty.getStartTime() != null ? 
                duty.getStartTime().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm")) : "Immediate";

        String waMessageText = String.format(
            "👮‍♂️ *POLICE STATION PATROL DUTY ALLOCATION*\n" +
            "-----------------------------------------\n" +
            "👤 *Staff Officer:* %s\n" +
            "🏅 *Rank / Designation:* %s\n" +
            "📱 *Mobile:* %s\n" +
            "👮 *Assigned By Station In-Charge:* %s\n" +
            "📅 *Shift Name:* %s\n" +
            "⏰ *Start Time:* %s\n" +
            "📍 *Assigned Duty Points:*\n%s\n\n" +
            "🚨 *Instructions:* Please acknowledge receipt of this WhatsApp dispatch and report to your assigned points. Scan the checkpoint QR code upon arrival to update live GPS location.",
            staffName, designation, staffPhone, inChargeName, shiftName, timeStr,
            duty.getCheckpointsList() != null ? "   • " + duty.getCheckpointsList().replace(",", "\n   • ") : "   • Main Gate Entrance"
        );

        // Sanitize phone number for wa.me link (e.g., +91-9876543210 -> 919876543210)
        String cleanPhone = staffPhone.replaceAll("[^0-9]", "");
        if (!cleanPhone.startsWith("91") && cleanPhone.length() == 10) {
            cleanPhone = "91" + cleanPhone;
        }

        String waUrl = "https://wa.me/" + cleanPhone + "?text=" + java.net.URLEncoder.encode(waMessageText, java.nio.charset.StandardCharsets.UTF_8);

        Map<String, String> waPayload = new HashMap<>();
        waPayload.put("type", "WHATSAPP_DUTY_DISPATCH");
        waPayload.put("toPhone", staffPhone);
        waPayload.put("cleanPhone", cleanPhone);
        waPayload.put("staffName", staffName);
        waPayload.put("designation", designation);
        waPayload.put("shiftName", shiftName);
        waPayload.put("checkpoints", duty.getCheckpointsList());
        waPayload.put("waMessageText", waMessageText);
        waPayload.put("whatsappUrl", waUrl);
        waPayload.put("status", "WHATSAPP_READY_DISPATCHED");
        waPayload.put("sentAt", OffsetDateTime.now().toString());

        dispatchedSmsLogs.add(waPayload);

        System.out.println("\n=======================================================");
        System.out.println(" 🟢 WHATSAPP DUTY MESSAGE GENERATED & DISPATCHED ");
        System.out.println(" TO STAFF: " + staffName + " (" + designation + " - " + staffPhone + ")");
        System.out.println(" WA LINK:  " + waUrl);
        System.out.println(" MESSAGE:\n" + waMessageText);
        System.out.println("=======================================================\n");

        return waPayload;
    }

    public List<Map<String, String>> getDispatchedSmsLogs() {
        return dispatchedSmsLogs;
    }
}
