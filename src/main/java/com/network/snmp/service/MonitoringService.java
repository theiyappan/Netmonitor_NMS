package com.network.snmp.service;

import com.network.snmp.entity.mysql.InterfaceStaticInfo;
import com.network.snmp.entity.mysql.SystemLog;
import com.network.snmp.model.InterfaceMetric;
import com.network.snmp.model.NetworkDetails;
import com.network.snmp.repository.cassandra.InterfaceMetricRepository;
import com.network.snmp.repository.mysql.InterfaceStaticInfoRepository;
import com.network.snmp.repository.mysql.NetworkDetailsRepository;
import com.network.snmp.repository.mysql.SystemLogRepository;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringService {

    private final SnmpService snmpService;
    private final InterfaceMetricRepository metricRepository;
    private final NetworkDetailsRepository networkDetailsRepository;
    private final InterfaceStaticInfoRepository staticInfoRepository;
    private final SystemLogRepository logRepository;
    private final AlertingService alertingService;

    private final Map<String, InterfaceState> previousStates = new ConcurrentHashMap<>();

    @Value("${snmp.community}")
    private String community;

    private static final double THRESHOLD_HIGH_UTILIZATION = 90.0;
    private static final double THRESHOLD_HIGH_ERRORS = 0.5;

    @PostConstruct
    public void restoreStateFromDatabase() {
        log.info("Restoring monitoring state...");
        try {
            List<NetworkDetails> devices = networkDetailsRepository.findAll();
            for (NetworkDetails device : devices) {
                String systemName = device.getSystemName();
                Instant cutoff = Instant.now().minus(Duration.ofHours(1));
                List<InterfaceMetric> metrics = metricRepository.findBySystemNameRecent(systemName, cutoff);

                if (metrics.isEmpty()) continue;

                Instant latestTime = metrics.stream()
                        .map(InterfaceMetric::getSnapTime)
                        .max(Comparator.naturalOrder()).orElse(null);

                if (latestTime != null) {
                    metrics.stream().filter(m -> m.getSnapTime().equals(latestTime))
                            .forEach(m -> {
                                String key = systemName + ":" + m.getInterfaceIndex();
                                previousStates.put(key, new InterfaceState(
                                        m.getRxBytes() != null ? m.getRxBytes() : 0,
                                        m.getTxBytes() != null ? m.getTxBytes() : 0,
                                        m.getRxPackets() != null ? m.getRxPackets() : 0,
                                        m.getTxPackets() != null ? m.getTxPackets() : 0,
                                        m.getInErrors() != null ? m.getInErrors() : 0,
                                        m.getOutErrors() != null ? m.getOutErrors() : 0,
                                        m.getInDiscards() != null ? m.getInDiscards() : 0,
                                        m.getOutDiscards() != null ? m.getOutDiscards() : 0,
                                        m.getSnapTime()
                                ));
                            });
                }
            }
            logToDb("STARTUP", "MonitoringService", "SYSTEM", "State restored successfully.", null);
        } catch (Exception e) {
            logToDb("ERROR", "MonitoringService", "SYSTEM", "Failed to restore state", e);
        }
    }

    @Scheduled(fixedDelayString = "${snmp.scan-interval}")
    public void scanInterfaces() {
        log.info(">>> STARTING SCHEDULED SCAN <<<");
        List<NetworkDetails> devices = networkDetailsRepository.findAll();
        if (devices.isEmpty()) return;

        for (NetworkDetails device : devices) {
            scanDevice(device);
        }
    }

    private void scanDevice(NetworkDetails device) {
        String host = device.getIpAddress();
        int port = device.getSnmpPort() != null ? device.getSnmpPort() : 161;
        String dbSystemName = device.getSystemName();

        double utilLimit = device.getThresholdUtil() != null ? device.getThresholdUtil() : THRESHOLD_HIGH_UTILIZATION;
        double errorLimit = device.getThresholdError() != null ? device.getThresholdError() : THRESHOLD_HIGH_ERRORS;

        logToDb("INFO", "Scanner", dbSystemName, "Starting SNMP scan for " + host, null);

        try {
            Instant snapTime = Instant.now();
            String realSystemName = snmpService.get(host, port, community, "1.3.6.1.2.1.1.5.0");

            if (realSystemName.contains("Error") || realSystemName.contains("Timeout")) {
                logToDb("ERROR", "Scanner", dbSystemName, "Device unreachable: " + host, null);
                alertingService.triggerAlert(dbSystemName, 0, "DEVICE", "CriticalDeviceDown",
                        "Device " + host + " is unreachable via SNMP.");
                return;
            } else {
                alertingService.resolveAlert(dbSystemName, 0, "CriticalDeviceDown");
            }

            List<InterfaceStaticInfo> existingInfos = staticInfoRepository.findAllBySystemName(dbSystemName);
            Map<Integer, InterfaceStaticInfo> staticInfoMap = existingInfos.stream()
                    .collect(Collectors.toMap(InterfaceStaticInfo::getInterfaceIndex, Function.identity()));
            List<InterfaceStaticInfo> staticInfosToSave = new ArrayList<>();

            String ifNumber = snmpService.get(host, port, community, "1.3.6.1.2.1.2.1.0");
            int totalInterfaces = parseInt(ifNumber, 0);

            List<InterfaceMetric> metrics = new ArrayList<>();

            for (int ifIndex = 1; ifIndex <= totalInterfaces; ifIndex++) {
                InterfaceMetric metric = processInterface(host, port, dbSystemName, ifIndex, snapTime, staticInfoMap, staticInfosToSave);

                if (metric != null) {
                    metrics.add(metric);
                    checkAlerts(metric, utilLimit, errorLimit, dbSystemName, ifIndex);
                }
            }

            if (!staticInfosToSave.isEmpty()) {
                staticInfoRepository.saveAll(staticInfosToSave);
            }

            if (!metrics.isEmpty()) {
                metricRepository.saveAll(metrics);
            }

            device.setLastScanTime(snapTime);
            device.setTotalInterfaces(totalInterfaces);
            device.setTotalScans(device.getTotalScans() == null ? 1 : device.getTotalScans() + 1);
            networkDetailsRepository.save(device);

            logToDb("INFO", "Scanner", dbSystemName,
                    String.format("Scan completed. Processed %d interfaces.", totalInterfaces), null);

        } catch (Exception e) {
            logToDb("ERROR", "Scanner", dbSystemName, "Error scanning " + host, e);
        }
    }

    private InterfaceMetric processInterface(String host, int port, String systemName, int ifIndex, Instant snapTime,
                                             Map<Integer, InterfaceStaticInfo> staticInfoMap,
                                             List<InterfaceStaticInfo> staticInfosToSave) {
        try {
            String oidBase = "1.3.6.1.2.1.2.2.1.";
            String ifDescr = snmpService.get(host, port, community, oidBase + "2." + ifIndex);

            if (ifDescr.contains("Error") || ifDescr.contains("noSuchInstance")) return null;

            String ifOperStatus = snmpService.get(host, port, community, oidBase + "8." + ifIndex);
            String ifSpeedStr = snmpService.get(host, port, community, oidBase + "5." + ifIndex);
            String ifType = snmpService.get(host, port, community, oidBase + "3." + ifIndex);
            String ifMac = snmpService.get(host, port, community, oidBase + "6." + ifIndex);
            long speedBps = parseLong(ifSpeedStr, 0);
            String cleanName = snmpService.decodeHexString(ifDescr);

            InterfaceStaticInfo info = staticInfoMap.getOrDefault(ifIndex, InterfaceStaticInfo.builder()
                    .systemName(systemName)
                    .interfaceIndex(ifIndex)
                    .build());

            info.setInterfaceName(cleanName);
            info.setInterfaceType(ifType);
            info.setMacAddress(ifMac);
            info.setSpeedBps(speedBps);
            info.setLastUpdated(Instant.now());

            staticInfosToSave.add(info);

            String statusStr = getStatusString(ifOperStatus);

            InterfaceMetric.InterfaceMetricBuilder builder = InterfaceMetric.builder()
                    .systemName(systemName)
                    .snapTime(snapTime)
                    .interfaceIndex(ifIndex)
                    .interfaceName(cleanName)
                    .interfaceType(ifType)
                    .macAddress(ifMac)
                    .speedBps(speedBps)
                    .status(statusStr);

            if ("1".equals(ifOperStatus)) {
                long inOctets = parseLong(snmpService.get(host, port, community, oidBase + "10." + ifIndex), 0);
                long outOctets = parseLong(snmpService.get(host, port, community, oidBase + "16." + ifIndex), 0);
                long inPkts = parseLong(snmpService.get(host, port, community, oidBase + "11." + ifIndex), 0);
                long outPkts = parseLong(snmpService.get(host, port, community, oidBase + "17." + ifIndex), 0);
                long inErrors = parseLong(snmpService.get(host, port, community, oidBase + "14." + ifIndex), 0);
                long outErrors = parseLong(snmpService.get(host, port, community, oidBase + "20." + ifIndex), 0);
                long inDiscards = parseLong(snmpService.get(host, port, community, oidBase + "13." + ifIndex), 0);
                long outDiscards = parseLong(snmpService.get(host, port, community, oidBase + "19." + ifIndex), 0);

                InterfaceState currentState = new InterfaceState(
                        inOctets, outOctets, inPkts, outPkts,
                        inErrors, outErrors, inDiscards, outDiscards, snapTime
                );

                String key = systemName + ":" + ifIndex;
                InterfaceState previousState = previousStates.put(key, currentState);

                if (previousState != null) {
                    calculateDeltas(builder, currentState, previousState, speedBps);
                } else {
                    populateZeroRates(builder);
                    builder.rxBytes(inOctets).txBytes(outOctets).totalBytes(inOctets + outOctets);
                }
            } else {
                previousStates.remove(systemName + ":" + ifIndex);
                populateZeroRates(builder);
            }

            return builder.build();

        } catch (Exception e) {
            log.error("Error processing interface index {}", ifIndex, e);
            return null;
        }
    }

    private void checkAlerts(InterfaceMetric metric, double utilLimit, double errorLimit, String systemName, int ifIndex) {
        if (metric.getRxPercent() > utilLimit || metric.getTxPercent() > utilLimit) {
            String msg = String.format("High Traffic: RX: %.2f%%, TX: %.2f%%", metric.getRxPercent(), metric.getTxPercent());
            alertingService.triggerAlert(systemName, ifIndex, metric.getInterfaceName(), "HighTrafficOverhead", msg);
        } else {
            alertingService.resolveAlert(systemName, ifIndex, "HighTrafficOverhead");
        }

        if (metric.getErrorRatePercent() != null && metric.getErrorRatePercent() > errorLimit) {
            String msg = String.format("High Error Rate: %.2f%%", metric.getErrorRatePercent());
            alertingService.triggerAlert(systemName, ifIndex, metric.getInterfaceName(), "CriticalErrorRate", msg);
        } else {
            alertingService.resolveAlert(systemName, ifIndex, "CriticalErrorRate");
        }
    }

    private void calculateDeltas(InterfaceMetric.InterfaceMetricBuilder builder, InterfaceState current, InterfaceState prev, long speedBps) {
        long timeDiffSeconds = Duration.between(prev.getTimestamp(), current.getTimestamp()).getSeconds();
        if (timeDiffSeconds <= 0) timeDiffSeconds = 1;

        long rxBytes = Math.max(0, current.rxBytes - prev.rxBytes);
        long txBytes = Math.max(0, current.txBytes - prev.txBytes);
        long rxPkts = Math.max(0, current.rxPackets - prev.rxPackets);
        long txPkts = Math.max(0, current.txPackets - prev.txPackets);
        long inErrDelta = Math.max(0, current.inErrors - prev.inErrors);
        long outErrDelta = Math.max(0, current.outErrors - prev.outErrors);
        long inDiscDelta = Math.max(0, current.inDiscards - prev.inDiscards);
        long outDiscDelta = Math.max(0, current.outDiscards - prev.outDiscards);

        double rxBitsPerSec = (rxBytes * 8.0) / timeDiffSeconds;
        double txBitsPerSec = (txBytes * 8.0) / timeDiffSeconds;
        long effectiveSpeed = (speedBps > 0) ? speedBps : 10_000_000L;

        builder.rxBytes(rxBytes).txBytes(txBytes).totalBytes(rxBytes + txBytes)
                .rxBitsPerSec(rxBitsPerSec).txBitsPerSec(txBitsPerSec)
                .totalBitsPerSec(rxBitsPerSec + txBitsPerSec)
                .rxPercent((rxBitsPerSec / effectiveSpeed) * 100)
                .txPercent((txBitsPerSec / effectiveSpeed) * 100)
                .rxPackets(rxPkts).txPackets(txPkts)
                .rxPacketsPerSec(rxPkts / (double) timeDiffSeconds)
                .txPacketsPerSec(txPkts / (double) timeDiffSeconds)
                .inErrors(inErrDelta).outErrors(outErrDelta)
                .inDiscards(inDiscDelta).outDiscards(outDiscDelta);

        long totalPackets = rxPkts + txPkts;
        builder.errorRatePercent(totalPackets > 0 ? ((inErrDelta + outErrDelta) * 100.0) / totalPackets : 0.0);
    }

    private void populateZeroRates(InterfaceMetric.InterfaceMetricBuilder builder) {
        builder.rxBitsPerSec(0.0).txBitsPerSec(0.0).totalBitsPerSec(0.0)
                .rxPercent(0.0).txPercent(0.0)
                .rxPackets(0L).txPackets(0L).rxPacketsPerSec(0.0).txPacketsPerSec(0.0)
                .inErrors(0L).outErrors(0L).inDiscards(0L).outDiscards(0L)
                .errorRatePercent(0.0);
    }

    private void logToDb(String level, String source, String systemName, String message, Exception e) {
        try {
            SystemLog logEntry = SystemLog.builder()
                    .timestamp(Instant.now())
                    .level(level)
                    .source(source)
                    .systemName(systemName)
                    .message(message)
                    .details(e != null ? e.getMessage() : "")
                    .build();
            logRepository.save(logEntry);
        } catch (Exception ex) { }
    }

    private String getStatusString(String operStatus) {
        return switch (operStatus) {
            case "1" -> "up";
            case "2" -> "down";
            default -> "unknown";
        };
    }

    private long parseLong(String value, long defaultValue) {
        try { return Long.parseLong(value); } catch (Exception e) { return defaultValue; }
    }

    private int parseInt(String value, int defaultValue) {
        try { return Integer.parseInt(value); } catch (Exception e) { return defaultValue; }
    }

    public void scanSpecificSystem(String systemName) {
        log.info("Manual scan requested for: {}", systemName);
        networkDetailsRepository.findBySystemName(systemName)
                .ifPresent(this::scanDevice);
    }

    @Data
    @AllArgsConstructor
    private static class InterfaceState {
        long rxBytes; long txBytes; long rxPackets; long txPackets;
        long inErrors; long outErrors; long inDiscards; long outDiscards;
        Instant timestamp;
    }
}