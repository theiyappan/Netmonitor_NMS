package com.network.snmp.controller;

import com.network.snmp.dto.DashboardData;
import com.network.snmp.dto.InterfaceGraphData;
import com.network.snmp.entity.mysql.SystemLog;
import com.network.snmp.model.NetworkDetails;
import com.network.snmp.repository.mysql.NetworkDetailsRepository;
import com.network.snmp.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService dashboardService;
    private final ArchivingService archivingService;
    private final AlertingService alertingService;
    private final NetworkDetailsRepository networkDetailsRepository;
    private final MonitoringService monitoringService;
    private final DiscoveryService discoveryService;

    @PostMapping("/discovery")
    public ResponseEntity<List<String>> discoverDevices(
            @RequestParam String cidr,
            @RequestParam(defaultValue = "public") String community) {
        return ResponseEntity.ok(discoveryService.scanSubnet(cidr, community));
    }

    @GetMapping("/{systemName}/report")
    public ResponseEntity<byte[]> generateReport(@PathVariable String systemName) {
        // Return a dummy PDF byte array to verify connectivity
        // In real impl, use iText or JasperReports here.
        byte[] pdfContent = ("Report for " + systemName + "\n\n(PDF Generation Stub)").getBytes();
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=report.pdf")
                .body(pdfContent);
    }

    @PostMapping("/{systemName}/test-email")
    public ResponseEntity<String> sendTestEmail(@PathVariable String systemName) {
        try {
            alertingService.triggerAlert(systemName, 999, "TEST", "ManualTest", "This is a manual test notification.");
            return ResponseEntity.ok("Test email sent successfully!");
        } catch (Exception e) {
            // Return 200 with warning so frontend doesn't crash
            return ResponseEntity.ok("Alert triggered locally, but Email failed: " + e.getMessage());
        }
    }

    @PostMapping("/{systemName}/scan")
    public ResponseEntity<String> triggerManualScan(@PathVariable String systemName) {
        CompletableFuture.runAsync(() -> monitoringService.scanSpecificSystem(systemName));
        return ResponseEntity.ok("Scan started in background for " + systemName);
    }

    @GetMapping("/{systemName}/logs/all")
    public ResponseEntity<List<SystemLog>> getDeviceAllLogs(@PathVariable String systemName, @RequestParam(defaultValue = "24") int hours) {
        return ResponseEntity.ok(dashboardService.getDeviceAllLogs(systemName, hours));
    }

    @GetMapping("/{systemName}/logs/errors")
    public ResponseEntity<List<SystemLog>> getDeviceErrors(@PathVariable String systemName, @RequestParam(defaultValue = "24") int hours) {
        return ResponseEntity.ok(dashboardService.getDeviceErrors(systemName, hours));
    }

    @GetMapping("/devices")
    public ResponseEntity<List<NetworkDetails>> getAllDevices() {
        return ResponseEntity.ok(dashboardService.getAllDevices());
    }

    @PostMapping("/devices")
    public ResponseEntity<NetworkDetails> addDevice(@RequestBody NetworkDetails device) {
        return ResponseEntity.ok(dashboardService.addDevice(device));
    }

    @PutMapping("/{systemName}/ip")
    public ResponseEntity<?> updateDeviceIp(@PathVariable String systemName, @RequestBody Map<String, String> payload) {
        String newIp = payload.get("ipAddress");
        if (newIp == null || newIp.isEmpty()) return ResponseEntity.badRequest().body("IP Required");
        dashboardService.updateDeviceIp(systemName, newIp);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{systemName}/settings")
    public ResponseEntity<NetworkDetails> updateSettings(@PathVariable String systemName, @RequestBody NetworkDetails settings) {
        return networkDetailsRepository.findBySystemName(systemName)
                .map(device -> {
                    if (settings.getThresholdUtil() != null) device.setThresholdUtil(settings.getThresholdUtil());
                    if (settings.getThresholdError() != null) device.setThresholdError(settings.getThresholdError());
                    NetworkDetails updated = networkDetailsRepository.save(device);
                    dashboardService.logToDb("INFO", "Settings", systemName,
                            "Updated thresholds: Util=" + updated.getThresholdUtil() + "%, Err=" + updated.getThresholdError() + "%");
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{systemName}")
    public ResponseEntity<DashboardData> getDashboard(@PathVariable String systemName, @RequestParam(defaultValue = "0") int hours) {
        return ResponseEntity.ok(dashboardService.getDashboardData(systemName, hours));
    }

    @GetMapping("/{systemName}/interface/{interfaceIndex}/graph")
    public ResponseEntity<InterfaceGraphData> getInterfaceGraph(@PathVariable String systemName, @PathVariable int interfaceIndex, @RequestParam(defaultValue = "24") int hours) {
        return ResponseEntity.ok(dashboardService.getInterfaceGraphData(systemName, interfaceIndex, hours));
    }

    @PostMapping("/{systemName}/archive")
    public ResponseEntity<String> forceArchive(@PathVariable String systemName) {
        try {
            archivingService.forceArchiveAll(systemName);
            return ResponseEntity.ok("Force archiving triggered for " + systemName);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Archive failed: " + e.getMessage());
        }
    }
}