package org.example;

import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.*;
import java.net.InetAddress;

import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class InterfaceProgram {

    static class InterfaceData {
        public int index;
        public String name;
        public String type;
        public String status;
        public long speedBps;
        public String macAddress;
        public String ipAddress;

        public long rxBytes;
        public long txBytes;
        public long totalBytes;
        public double rxBitsPerSec;
        public double txBitsPerSec;
        public double totalBitsPerSec;

        public double rxUtilizationPercent;
        public double txUtilizationPercent;

        public long rxPackets;
        public long txPackets;
        public double rxPacketsPerSec;
        public double txPacketsPerSec;

        public long inErrors;
        public long outErrors;
        public long inDiscards;
        public long outDiscards;
        public double errorRatePercent;

        public String timestamp;
    }

    static class SystemInfo {
        public String description;
        public String uptime;
        public String contact;
        public String name;
        public String location;
        public int totalInterfaces;
        public int activeInterfaces;
        public String timestamp;
    }
    private static CqlSession session;
    public static void connect(String node, Integer Port,String datacenter,String keyspace){
        session= CqlSession.builder()
                .addContactPoint(new InetSocketAddress(node,Port))
                .withLocalDatacenter(datacenter)
                .withKeyspace(keyspace)
                .build();
    }
    public static void initialize(){
        if(session == null){
            connect("127.0.0.1",9042,"datacenter1","new_app");
        }
    }
    public static void main(String[] args) throws Exception {
        String host = "127.0.0.1";
        int port = 161;
        String community = "public";
        int monitoringDuration = 10;
        initialize();

        System.out.println("Starting SNMP Network Monitor...");
        System.out.println("Host: " + host);
        System.out.println("Monitoring Duration: " + monitoringDuration + " seconds");
        System.out.println("=" .repeat(60));
        SystemInfo systemInfo = new SystemInfo();
        systemInfo.description = infoget(host, port, community, "1.3.6.1.2.1.1.1.0");
        systemInfo.uptime = infoget(host, port, community, "1.3.6.1.2.1.1.3.0");
        systemInfo.contact = infoget(host, port, community, "1.3.6.1.2.1.1.4.0");
        systemInfo.name = infoget(host, port, community, "1.3.6.1.2.1.1.5.0");
        systemInfo.location = infoget(host, port, community, "1.3.6.1.2.1.1.6.0");
        systemInfo.timestamp = getCurrentTimestamp();
        String ifNumber = infoget(host, port, community, "1.3.6.1.2.1.2.1.0");
        systemInfo.totalInterfaces = parseIntSafe(ifNumber, 37);
        System.out.println("\n=== System Information ===");
        System.out.println("System: " + systemInfo.name);
        System.out.println("Description: " + systemInfo.description);
        System.out.println("Uptime: " + systemInfo.uptime);
        System.out.println("Total Interfaces: " + systemInfo.totalInterfaces);

        List<InterfaceData> allInterfaces = new ArrayList<>();
        System.out.println("\n=== Scanning Interfaces ===");

        for (int ifIndex = 1; ifIndex <= systemInfo.totalInterfaces; ifIndex++) {
            String ifDescr = infoget(host, port, community, "1.3.6.1.2.1.2.2.1.2." + ifIndex);

            if (ifDescr.contains("Error") || ifDescr.contains("Timeout") || ifDescr.contains("Exception")) {
                continue;
            }
            InterfaceData iface = new InterfaceData();
            iface.index = ifIndex;
            iface.name = decodeHexString(ifDescr);
            iface.type = infoget(host, port, community, "1.3.6.1.2.1.2.2.1.3." + ifIndex);
            iface.macAddress = infoget(host, port, community, "1.3.6.1.2.1.2.2.1.6." + ifIndex);
            iface.timestamp = getCurrentTimestamp();

            String ifSpeed = infoget(host, port, community, "1.3.6.1.2.1.2.2.1.5." + ifIndex);
            iface.speedBps = parseLongSafe(ifSpeed, 0);

            String ifOperStatus = infoget(host, port, community, "1.3.6.1.2.1.2.2.1.8." + ifIndex);
            iface.status = getStatusString(ifOperStatus);

            if (ifOperStatus.equals("1") && iface.speedBps > 0) {
                System.out.println("Monitoring Interface " + ifIndex + ": " + iface.name + " (" + iface.status + ")");

                String ifInOctets_before = infoget(host, port, community, "1.3.6.1.2.1.2.2.1.10." + ifIndex);
                String ifOutOctets_before = infoget(host, port, community, "1.3.6.1.2.1.2.2.1.16." + ifIndex);
                String ifInUcastPkts_before = infoget(host, port, community, "1.3.6.1.2.1.2.2.1.11." + ifIndex);
                String ifOutUcastPkts_before = infoget(host, port, community, "1.3.6.1.2.1.2.2.1.17." + ifIndex);
                String ifInErrors_before = infoget(host, port, community, "1.3.6.1.2.1.2.2.1.14." + ifIndex);
                String ifOutErrors_before = infoget(host, port, community, "1.3.6.1.2.1.2.2.1.20." + ifIndex);
                String ifInDiscards_before = infoget(host, port, community, "1.3.6.1.2.1.2.2.1.13." + ifIndex);
                String ifOutDiscards_before = infoget(host, port, community, "1.3.6.1.2.1.2.2.1.19." + ifIndex);
                Thread.sleep(monitoringDuration * 1000);

                String ifInOctets_after = infoget(host, port, community, "1.3.6.1.2.1.2.2.1.10." + ifIndex);
                String ifOutOctets_after = infoget(host, port, community, "1.3.6.1.2.1.2.2.1.16." + ifIndex);
                String ifInUcastPkts_after = infoget(host, port, community, "1.3.6.1.2.1.2.2.1.11." + ifIndex);
                String ifOutUcastPkts_after = infoget(host, port, community, "1.3.6.1.2.1.2.2.1.17." + ifIndex);
                String ifInErrors_after = infoget(host, port, community, "1.3.6.1.2.1.2.2.1.14." + ifIndex);
                String ifOutErrors_after = infoget(host, port, community, "1.3.6.1.2.1.2.2.1.20." + ifIndex);
                String ifInDiscards_after = infoget(host, port, community, "1.3.6.1.2.1.2.2.1.13." + ifIndex);
                String ifOutDiscards_after = infoget(host, port, community, "1.3.6.1.2.1.2.2.1.19." + ifIndex);

                try {
                    iface.rxBytes = parseLongSafe(ifInOctets_after, 0) - parseLongSafe(ifInOctets_before, 0);
                    iface.txBytes = parseLongSafe(ifOutOctets_after, 0) - parseLongSafe(ifOutOctets_before, 0);
                    iface.totalBytes = iface.rxBytes + iface.txBytes;
                    iface.rxBitsPerSec = (iface.rxBytes * 8.0) / monitoringDuration;
                    iface.txBitsPerSec = (iface.txBytes * 8.0) / monitoringDuration;
                    iface.totalBitsPerSec = iface.rxBitsPerSec + iface.txBitsPerSec;
                    if (iface.speedBps > 0) {
                        iface.rxUtilizationPercent = (iface.rxBitsPerSec / iface.speedBps) * 100;
                        iface.txUtilizationPercent = (iface.txBitsPerSec / iface.speedBps) * 100;
                    }
                    iface.rxPackets = parseLongSafe(ifInUcastPkts_after, 0) - parseLongSafe(ifInUcastPkts_before, 0);
                    iface.txPackets = parseLongSafe(ifOutUcastPkts_after, 0) - parseLongSafe(ifOutUcastPkts_before, 0);
                    iface.rxPacketsPerSec = iface.rxPackets / (double) monitoringDuration;
                    iface.txPacketsPerSec = iface.txPackets / (double) monitoringDuration;
                    iface.inErrors = parseLongSafe(ifInErrors_after, 0) - parseLongSafe(ifInErrors_before, 0);
                    iface.outErrors = parseLongSafe(ifOutErrors_after, 0) - parseLongSafe(ifOutErrors_before, 0);
                    iface.inDiscards = parseLongSafe(ifInDiscards_after, 0) - parseLongSafe(ifInDiscards_before, 0);
                    iface.outDiscards = parseLongSafe(ifOutDiscards_after, 0) - parseLongSafe(ifOutDiscards_before, 0);
                    long totalPackets = iface.rxPackets + iface.txPackets;
                    if (totalPackets > 0) {
                        iface.errorRatePercent = ((iface.inErrors + iface.outErrors) * 100.0) / totalPackets;
                    }
                    System.out.println("  → Rx: " + formatBytes(iface.rxBytes) + " | Tx: " + formatBytes(iface.txBytes));
                } catch (Exception e) {
                    System.out.println("  → Error calculating metrics: " + e.getMessage());
                }
            } else {
                System.out.println("Skipping Interface " + ifIndex + ": " + iface.name + " (" + iface.status + ")");
            }
            allInterfaces.add(iface);
        }
        systemInfo.activeInterfaces = (int) allInterfaces.stream()
                .filter(i -> i.status.equals("up") && i.speedBps > 0)
                .count();

        System.out.println("\n=== Exporting Data ===");
        String jsonOutput = generateJSON(systemInfo, allInterfaces);

        System.out.println("✓ Total interfaces: " + allInterfaces.size());
        System.out.println("✓ Active interfaces: " + systemInfo.activeInterfaces);
        SimpleStatement stmt = SimpleStatement.builder(
                        "INSERT INTO network_details " +
                                "(system_name, snap_time, uptime, system_desc,total_interface, active_interface, interfaces_json) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?)"
                )
                .addPositionalValues(
                        systemInfo.name,
                        Instant.now(),
                        systemInfo.uptime,
                        systemInfo.description,
                        systemInfo.totalInterfaces,
                        systemInfo.activeInterfaces,
                        jsonOutput
                )
                .build();

        session.execute(stmt);
        System.out.println(jsonOutput);
    }

    private static String generateJSON(SystemInfo sysInfo, List<InterfaceData> interfaces) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"system\": {\n");
        json.append("    \"name\": ").append(jsonString(sysInfo.name)).append(",\n");
        json.append("    \"description\": ").append(jsonString(sysInfo.description)).append(",\n");
        json.append("    \"uptime\": ").append(jsonString(sysInfo.uptime)).append(",\n");
        json.append("    \"contact\": ").append(jsonString(sysInfo.contact)).append(",\n");
        json.append("    \"location\": ").append(jsonString(sysInfo.location)).append(",\n");
        json.append("    \"totalInterfaces\": ").append(sysInfo.totalInterfaces).append(",\n");
        json.append("    \"activeInterfaces\": ").append(sysInfo.activeInterfaces).append(",\n");
        json.append("    \"timestamp\": ").append(jsonString(sysInfo.timestamp)).append("\n");
        json.append("  },\n");

        json.append("  \"interfaces\": [\n");

        for (int i = 0; i < interfaces.size(); i++) {
            InterfaceData iface = interfaces.get(i);
            json.append("    {\n");
            json.append("      \"index\": ").append(iface.index).append(",\n");
            json.append("      \"name\": ").append(jsonString(iface.name)).append(",\n");
            json.append("      \"type\": ").append(jsonString(iface.type)).append(",\n");
            json.append("      \"status\": ").append(jsonString(iface.status)).append(",\n");
            json.append("      \"speedBps\": ").append(iface.speedBps).append(",\n");
            json.append("      \"macAddress\": ").append(jsonString(iface.macAddress)).append(",\n");

            json.append("      \"traffic\": {\n");
            json.append("        \"rxBytes\": ").append(iface.rxBytes).append(",\n");
            json.append("        \"txBytes\": ").append(iface.txBytes).append(",\n");
            json.append("        \"totalBytes\": ").append(iface.totalBytes).append(",\n");
            json.append("        \"rxBitsPerSec\": ").append(roundTo2(iface.rxBitsPerSec)).append(",\n");
            json.append("        \"txBitsPerSec\": ").append(roundTo2(iface.txBitsPerSec)).append(",\n");
            json.append("        \"totalBitsPerSec\": ").append(roundTo2(iface.totalBitsPerSec)).append("\n");
            json.append("      },\n");

            json.append("      \"utilization\": {\n");
            json.append("        \"rxPercent\": ").append(roundTo2(iface.rxUtilizationPercent)).append(",\n");
            json.append("        \"txPercent\": ").append(roundTo2(iface.txUtilizationPercent)).append("\n");
            json.append("      },\n");

            json.append("      \"packets\": {\n");
            json.append("        \"rxPackets\": ").append(iface.rxPackets).append(",\n");
            json.append("        \"txPackets\": ").append(iface.txPackets).append(",\n");
            json.append("        \"rxPacketsPerSec\": ").append(roundTo2(iface.rxPacketsPerSec)).append(",\n");
            json.append("        \"txPacketsPerSec\": ").append(roundTo2(iface.txPacketsPerSec)).append("\n");
            json.append("      },\n");

            json.append("      \"errors\": {\n");
            json.append("        \"inErrors\": ").append(iface.inErrors).append(",\n");
            json.append("        \"outErrors\": ").append(iface.outErrors).append(",\n");
            json.append("        \"inDiscards\": ").append(iface.inDiscards).append(",\n");
            json.append("        \"outDiscards\": ").append(iface.outDiscards).append(",\n");
            json.append("        \"errorRatePercent\": ").append(roundTo2(iface.errorRatePercent)).append("\n");
            json.append("      },\n");

            json.append("      \"timestamp\": ").append(jsonString(iface.timestamp)).append("\n");

            json.append("    }");
            if (i < interfaces.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("  ]\n");
        json.append("}\n");
        return json.toString();
    }
    private static String jsonString(String value) {
        if (value == null) value = "";
        value = value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
        return "\"" + value + "\"";
    }
    private static double roundTo2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
    private static void saveToFile(String content, String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(content);
        } catch (IOException e) {
            System.err.println("Error saving file: " + e.getMessage());
        }
    }
    private static String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    private static String getStatusString(String operStatus) {
        switch (operStatus) {
            case "1": return "up";
            case "2": return "down";
            case "3": return "testing";
            case "4": return "unknown";
            case "5": return "dormant";
            case "6": return "notPresent";
            case "7": return "lowerLayerDown";
            default: return "unknown";
        }
    }
    private static long parseLongSafe(String value, long defaultValue) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }
    private static int parseIntSafe(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
    public static String decodeHexString(String hexString) {
        if (hexString == null || hexString.isEmpty()) {
            return hexString;
        }
        if (!hexString.contains(":") || hexString.length() < 5) {
            return hexString;
        }
        try {
            String[] hexParts = hexString.split(":");
            StringBuilder decoded = new StringBuilder();
            for (String hex : hexParts) {
                if (hex.isEmpty()) continue;
                int value = Integer.parseInt(hex, 16);
                if (value > 0 && value < 128) {
                    decoded.append((char) value);
                }
            }
            String result = decoded.toString().trim();
            return result.isEmpty() ? hexString : result;
        } catch (Exception e) {
            return hexString;
        }
    }
    public static String infoget(String host, int port, String community, String oid) {
        Snmp snmp = null;
        TransportMapping<?> transport = null;
        try {
            transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            transport.listen();
            CommunityTarget target = new CommunityTarget();
            target.setCommunity(new OctetString(community));
            target.setAddress(new UdpAddress(host + "/" + port));
            target.setRetries(2);
            target.setTimeout(3000);
            target.setVersion(SnmpConstants.version2c);
            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(oid)));
            pdu.setType(PDU.GET);
            ResponseEvent<?> response = snmp.send(pdu, target);
            if (response != null && response.getResponse() != null) {
                PDU responsePDU = response.getResponse();
                if (responsePDU.getErrorStatus() == PDU.noError) {
                    return responsePDU.get(0).getVariable().toString();
                } else {
                    return "Error: " + responsePDU.getErrorStatusText();
                }
            }
            return "Timeout/Error";
        } catch (Exception e) {
            return "Exception: " + e.getMessage();
        } finally {
            try {
                if (snmp != null) snmp.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}