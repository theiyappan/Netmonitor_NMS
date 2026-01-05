package com.network.snmp.service;

import com.network.snmp.dto.*;
import com.network.snmp.model.*;
import com.network.snmp.entity.mysql.SystemLog;
import com.network.snmp.repository.mysql.NetworkDetailsRepository;
import com.network.snmp.repository.mysql.SystemLogRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final NetworkDetailsRepository networkDetailsRepository;
    private final MetricsQueryService queryService;
    private final SystemLogRepository logRepository;

    public DashboardData getDashboardData(String systemName, int hours) {
        NetworkDetails device = networkDetailsRepository.findBySystemName(systemName).orElse(null);
        if (device == null) return null;

        List<UnifiedMetric> unifiedData = new ArrayList<>();

        if (hours == 0) {
            Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
            List<InterfaceMetric> raw = queryService.getLatestMetrics(systemName, cutoff);

            Map<Integer, InterfaceMetric> uniqueMap = raw.stream()
                    .collect(Collectors.toMap(
                            InterfaceMetric::getInterfaceIndex,
                            metric -> metric,
                            (existing, replacement) -> existing.getSnapTime().isAfter(replacement.getSnapTime()) ? existing : replacement
                    ));
            unifiedData = uniqueMap.values().stream().map(this::convertToUnified).collect(Collectors.toList());

        } else if (hours <= 168) {
            List<InterfaceMetric> raw = queryService.getRawMetricsByTime(systemName, hours);
            unifiedData = aggregateRawData(raw);

        } else {
            // 30 DAYS + (Use Daily Archives for performance)
            int days = hours / 24;
            List<InterfaceMetricDaily> daily = queryService.getDailyMetrics(systemName, days);
            unifiedData = aggregateDailyData(daily);
        }

        List<InterfaceCardData> cards = unifiedData.stream()
                .sorted(Comparator.comparingInt(UnifiedMetric::getInterfaceIndex))
                .map(this::buildCardFromUnified)
                .collect(Collectors.toList());

        int activeInterfaces = (int) cards.stream().filter(c -> "up".equalsIgnoreCase(c.getStatus())).count();

        return DashboardData.builder()
                .systemName(systemName)
                .ipAddress(device.getIpAddress())
                .totalInterfaces(cards.size())
                .activeInterfaces(activeInterfaces)
                .lastScanTime(device.getLastScanTime())
                .interfaces(cards)
                .build();
    }

    public InterfaceGraphData getInterfaceGraphData(String systemName, int interfaceIndex, int hours) {
        List<DataPoint> rxData = new ArrayList<>();
        List<DataPoint> txData = new ArrayList<>();
        List<DataPoint> utilData = new ArrayList<>();

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("MM/dd").withZone(ZoneId.systemDefault());

        Instant now = Instant.now();
        Instant start = now.minus(hours, ChronoUnit.HOURS);

        List<UnifiedMetric> history;

        if (hours <= 168) {
            history = queryService.getRawInterfaceHistory(systemName, interfaceIndex, hours)
                    .stream().map(this::convertToUnified).collect(Collectors.toList());
        } else {
            int days = hours / 24;
            history = queryService.getDailyMetrics(systemName, days).stream()
                    .filter(d -> d.getInterfaceIndex() == interfaceIndex)
                    .map(this::convertToUnified).collect(Collectors.toList());
        }

        if (history.isEmpty()) {
            return InterfaceGraphData.builder()
                    .systemName(systemName).interfaceIndex(interfaceIndex).interfaceName("Unknown")
                    .rxData(List.of()).txData(List.of()).utilizationData(List.of())
                    .summary(InterfaceSummary.builder().build())
                    .build();
        }

        history.sort(Comparator.comparing(UnifiedMetric::getTimestamp));
        String ifName = history.get(0).getName();
        long currentSpeed = history.get(history.size() - 1).getSpeed();

        DoubleSummaryStatistics rxStats = history.stream().mapToDouble(UnifiedMetric::getRxBps).summaryStatistics();
        DoubleSummaryStatistics txStats = history.stream().mapToDouble(UnifiedMetric::getTxBps).summaryStatistics();

        long stepSeconds = (hours <= 48) ? 300 : 3600;

        Map<Long, UnifiedMetric> dataMap = history.stream()
                .collect(Collectors.toMap(m -> roundToStep(m.getTimestamp(), stepSeconds), m -> m, (a, b) -> b));

        for (Instant t = roundToInstant(start, stepSeconds); !t.isAfter(now); t = t.plusSeconds(stepSeconds)) {
            UnifiedMetric m = dataMap.get(t.toEpochMilli());

            String label;
            if (hours <= 24) label = timeFmt.format(t); // HH:mm
            else label = dateFmt.format(t) + " " + timeFmt.format(t); // MM/dd HH:mm

            long ts = t.toEpochMilli();
            rxData.add(new DataPoint(ts, m != null ? m.getRxBps() : 0.0, label));
            txData.add(new DataPoint(ts, m != null ? m.getTxBps() : 0.0, label));
            utilData.add(new DataPoint(ts, m != null ? m.getRxUtil() : 0.0, label));
        }

        InterfaceSummary summary = InterfaceSummary.builder()
                .interfaceName(ifName)
                .status("N/A")
                .avgRxBitsPerSec(rxStats.getAverage())
                .avgTxBitsPerSec(txStats.getAverage())
                .maxRxBitsPerSec(rxStats.getMax())
                .maxTxBitsPerSec(txStats.getMax())
                .totalErrors(0L)
                .speedBps(currentSpeed)
                .build();

        return InterfaceGraphData.builder()
                .systemName(systemName).interfaceIndex(interfaceIndex).interfaceName(ifName)
                .rxData(rxData).txData(txData).utilizationData(utilData)
                .summary(summary)
                .build();
    }


    public List<SystemLog> getDeviceAllLogs(String name, int hours) {
        return logRepository.findBySystemNameAndTimestampAfterOrderByTimestampDesc(name, calculateStartTime(hours));
    }

    public List<SystemLog> getDeviceErrors(String name, int hours) {
        return logRepository.findBySystemNameAndLevelAndTimestampAfterOrderByTimestampDesc(name, "ERROR", calculateStartTime(hours));
    }

    public List<SystemLog> getDeviceSystemLogs(String name, int hours) {
        return logRepository.findBySystemNameAndLevelNotAndTimestampAfterOrderByTimestampDesc(name, "ERROR", calculateStartTime(hours));
    }

    public void logToDb(String level, String source, String systemName, String message) {
        try {
            SystemLog log = SystemLog.builder()
                    .timestamp(Instant.now()).level(level).source(source)
                    .systemName(systemName).message(message).details("")
                    .build();
            logRepository.save(log);
        } catch (Exception ignored) {}
    }

    public List<NetworkDetails> getAllDevices() { return networkDetailsRepository.findAll(); }

    public NetworkDetails addDevice(NetworkDetails device) {
        if(device.getSnmpPort()==null) device.setSnmpPort(161);
        if(device.getFirstScanTime()==null) device.setFirstScanTime(Instant.now());
        NetworkDetails saved = networkDetailsRepository.save(device);
        logToDb("STARTUP", "UserAction", saved.getSystemName(), "Device added to monitoring.");
        return saved;
    }

    public void updateDeviceIp(String name, String ip) {
        NetworkDetails d = networkDetailsRepository.findBySystemName(name).orElseThrow();
        String oldIp = d.getIpAddress();
        d.setIpAddress(ip);
        networkDetailsRepository.save(d);
        logToDb("INFO", "UserAction", name, "IP updated from " + oldIp + " to " + ip);
    }

    private Instant calculateStartTime(int hours) {
        // If hours is 0 (Latest), default to 24h for logs.
        return Instant.now().minus(hours <= 0 ? 24 : hours, ChronoUnit.HOURS);
    }


    @Data @Builder
    private static class UnifiedMetric {
        int interfaceIndex; String name; String type; String status;
        long speed; double rxBps; double txBps; double rxUtil; double txUtil; Instant timestamp;
    }

    private UnifiedMetric convertToUnified(InterfaceMetric m) {
        long speed = (m.getSpeedBps() != null) ? m.getSpeedBps() : 0L;
        return UnifiedMetric.builder()
                .interfaceIndex(m.getInterfaceIndex()).name(m.getInterfaceName())
                .type(m.getInterfaceType()).status(m.getStatus()).speed(speed)
                .rxBps(m.getRxBitsPerSec() != null ? m.getRxBitsPerSec() : 0)
                .txBps(m.getTxBitsPerSec() != null ? m.getTxBitsPerSec() : 0)
                .rxUtil(m.getRxPercent() != null ? m.getRxPercent() : 0)
                .txUtil(m.getTxPercent() != null ? m.getTxPercent() : 0)
                .timestamp(m.getSnapTime())
                .build();
    }

    private UnifiedMetric convertToUnified(InterfaceMetricHourly h) {
        long speed = (h.getSpeedBps() != null) ? h.getSpeedBps() : 0L;
        return UnifiedMetric.builder()
                .interfaceIndex(h.getInterfaceIndex()).name(h.getInterfaceName())
                .type(h.getInterfaceType()).status("up").speed(speed)
                .rxBps(h.getAvgRxBps()).txBps(h.getAvgTxBps())
                .rxUtil(h.getAvgRxPercent()).txUtil(h.getAvgTxPercent())
                .timestamp(h.getHourBucket())
                .build();
    }

    private UnifiedMetric convertToUnified(InterfaceMetricDaily d) {
        long speed = (d.getSpeedBps() != null) ? d.getSpeedBps() : 0L;
        return UnifiedMetric.builder()
                .interfaceIndex(d.getInterfaceIndex()).name(d.getInterfaceName())
                .type(d.getInterfaceType()).status("up").speed(speed)
                .rxBps(d.getAvgRxBps()).txBps(d.getAvgTxBps())
                .rxUtil(d.getAvgRxPercent()).txUtil(d.getAvgTxPercent())
                .timestamp(d.getDayBucket())
                .build();
    }

    private List<UnifiedMetric> aggregateRawData(List<InterfaceMetric> raw) {
        return raw.stream().collect(Collectors.groupingBy(InterfaceMetric::getInterfaceIndex))
                .values().stream().map(list -> convertToUnified(list.get(list.size() - 1))).collect(Collectors.toList());
    }

    private List<UnifiedMetric> aggregateHourlyData(List<InterfaceMetricHourly> list) {
        return list.stream().collect(Collectors.groupingBy(InterfaceMetricHourly::getInterfaceIndex))
                .values().stream().map(group -> convertToUnified(group.get(0))).collect(Collectors.toList());
    }

    private List<UnifiedMetric> aggregateDailyData(List<InterfaceMetricDaily> list) {
        return list.stream().collect(Collectors.groupingBy(InterfaceMetricDaily::getInterfaceIndex))
                .values().stream().map(group -> convertToUnified(group.get(0))).collect(Collectors.toList());
    }

    private InterfaceCardData buildCardFromUnified(UnifiedMetric u) {
        return InterfaceCardData.builder()
                .interfaceIndex(u.getInterfaceIndex())
                .interfaceName(u.getName())
                .status(u.getStatus())
                .interfaceType(u.getType())
                .speedFormatted(formatSpeed(u.getSpeed()))
                .rxRate(formatBitsPerSec(u.getRxBps()))
                .txRate(formatBitsPerSec(u.getTxBps()))
                .rxUtilization(u.getRxUtil())
                .txUtilization(u.getTxUtil())
                .build();
    }

    private long roundToStep(Instant in, long seconds) { return (in.getEpochSecond() - (in.getEpochSecond() % seconds)) * 1000; }
    private Instant roundToInstant(Instant in, long seconds) { return Instant.ofEpochMilli(roundToStep(in, seconds)); }

    private String formatSpeed(Long speed) {
        if (speed == null || speed == 0) return "N/A";
        if (speed >= 1_000_000_000) return (speed / 1_000_000_000) + " Gbps";
        if (speed >= 1_000_000) return (speed / 1_000_000) + " Mbps";
        return speed + " bps";
    }

    private String formatBitsPerSec(Double bps) {
        if (bps == null) return "0 bps";
        if (bps >= 1_000_000_000) return String.format("%.2f Gbps", bps / 1_000_000_000);
        if (bps >= 1_000_000) return String.format("%.2f Mbps", bps / 1_000_000);
        if (bps >= 1_000) return String.format("%.2f Kbps", bps / 1_000);
        return String.format("%.2f bps", bps);
    }
}