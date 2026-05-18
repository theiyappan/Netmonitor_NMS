package com.network.snmp.service;

import com.network.snmp.model.*;
import com.network.snmp.entity.mysql.SystemLog;
import com.network.snmp.repository.cassandra.InterfaceMetricDailyRepository;
import com.network.snmp.repository.cassandra.InterfaceMetricHourlyRepository;
import com.network.snmp.repository.cassandra.InterfaceMetricRepository;
import com.network.snmp.repository.mysql.NetworkDetailsRepository;
import com.network.snmp.repository.mysql.SystemLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchivingService {

    private final InterfaceMetricRepository rawRepo;
    private final InterfaceMetricHourlyRepository hourlyRepo;
    private final InterfaceMetricDailyRepository dailyRepo;
    private final NetworkDetailsRepository networkRepo;
    private final SystemLogRepository logRepo;

    @Scheduled(cron = "0 0 2 * * ?")
    public void runNightlyArchiving() {
        log.info("Starting Nightly Archiving Process...");
        try {
            List<NetworkDetails> devices = networkRepo.findAll();
            for (NetworkDetails device : devices) {
                String sysName = device.getSystemName();
                saveLog("INFO", "Archiver", sysName, "Starting nightly data aggregation...");

                // 1. Hourly: Older than 7 days
                Instant hourlyStart = Instant.now().minus(8, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
                Instant hourlyEnd = hourlyStart.plus(1, ChronoUnit.DAYS);
                archiveRawToHourlyForRange(sysName, hourlyStart, hourlyEnd);

                // 2. Daily: Older than 30 days
                Instant dailyStart = Instant.now().minus(31, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
                Instant dailyEnd = dailyStart.plus(1, ChronoUnit.DAYS);
                archiveRawToDailyForRange(sysName, dailyStart, dailyEnd);

                saveLog("INFO", "Archiver", sysName, "Archiving completed successfully.");
            }
            logRepo.deleteByTimestampBefore(Instant.now().minus(30, ChronoUnit.DAYS));
        } catch (Exception e) {
            log.error("Archiving failed", e);
        }
    }

    public void forceArchiveAll(String systemName) {
        log.info("Force archiving for {}...", systemName);
        Instant now = Instant.now();

        saveLog("INFO", "Archiver", systemName, "Manual archiving triggered.");
        for (int i = 35; i >= 0; i--) {
            Instant targetStart = now.minus(i, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
            Instant targetEnd = targetStart.plus(1, ChronoUnit.DAYS);
            archiveRawToHourlyForRange(systemName, targetStart, targetEnd);
            archiveRawToDailyForRange(systemName, targetStart, targetEnd);
        }

        saveLog("INFO", "Archiver", systemName, "Manual archiving completed.");
    }


    private void archiveRawToHourlyForRange(String systemName, Instant start, Instant end) {
        List<InterfaceMetric> rawMetrics = rawRepo.findBySystemNameAndTimeRange(systemName, start, end);
        if (rawMetrics.isEmpty()) return;

        Map<String, List<InterfaceMetric>> grouped = rawMetrics.stream()
                .collect(Collectors.groupingBy(m -> m.getInterfaceIndex() + "_" + m.getSnapTime().truncatedTo(ChronoUnit.HOURS)));

        List<InterfaceMetricHourly> hourlyData = new ArrayList<>();
        for (List<InterfaceMetric> group : grouped.values()) {
            hourlyData.add(aggregateHourly(group, systemName));
        }
        hourlyRepo.saveAll(hourlyData);
    }

    private void archiveRawToDailyForRange(String systemName, Instant start, Instant end) {
        List<InterfaceMetric> rawMetrics = rawRepo.findBySystemNameAndTimeRange(systemName, start, end);
        if (rawMetrics.isEmpty()) return;

        Map<String, List<InterfaceMetric>> grouped = rawMetrics.stream()
                .collect(Collectors.groupingBy(m -> m.getInterfaceIndex() + "_" + m.getSnapTime().truncatedTo(ChronoUnit.DAYS)));

        List<InterfaceMetricDaily> dailyData = new ArrayList<>();
        for (List<InterfaceMetric> group : grouped.values()) {
            dailyData.add(aggregateDaily(group, systemName));
        }
        dailyRepo.saveAll(dailyData);
    }


    private InterfaceMetricHourly aggregateHourly(List<InterfaceMetric> group, String systemName) {
        DoubleSummaryStatistics rxStats = group.stream().mapToDouble(m -> m.getRxBitsPerSec() != null ? m.getRxBitsPerSec() : 0).summaryStatistics();
        DoubleSummaryStatistics txStats = group.stream().mapToDouble(m -> m.getTxBitsPerSec() != null ? m.getTxBitsPerSec() : 0).summaryStatistics();
        InterfaceMetric s = group.get(0);

        return InterfaceMetricHourly.builder()
                .systemName(systemName)
                .hourBucket(s.getSnapTime().truncatedTo(ChronoUnit.HOURS))
                .interfaceIndex(s.getInterfaceIndex())
                .interfaceName(s.getInterfaceName()).interfaceType(s.getInterfaceType()).speedBps(s.getSpeedBps())
                .avgRxBps(rxStats.getAverage()).maxRxBps(rxStats.getMax()).minRxBps(rxStats.getMin())
                .avgTxBps(txStats.getAverage()).maxTxBps(txStats.getMax()).minTxBps(txStats.getMin())
                .avgRxPercent(group.stream().mapToDouble(m -> m.getRxPercent() != null ? m.getRxPercent() : 0).average().orElse(0))
                .avgTxPercent(group.stream().mapToDouble(m -> m.getTxPercent() != null ? m.getTxPercent() : 0).average().orElse(0))
                .totalRxBytes(group.stream().mapToLong(m -> m.getRxBytes() != null ? m.getRxBytes() : 0).sum())
                .totalTxBytes(group.stream().mapToLong(m -> m.getTxBytes() != null ? m.getTxBytes() : 0).sum())
                .totalTotalBytes(group.stream().mapToLong(m -> m.getTotalBytes() != null ? m.getTotalBytes() : 0).sum())
                .avgRxPps(group.stream().mapToDouble(m -> m.getRxPacketsPerSec() != null ? m.getRxPacketsPerSec() : 0).average().orElse(0))
                .avgTxPps(group.stream().mapToDouble(m -> m.getTxPacketsPerSec() != null ? m.getTxPacketsPerSec() : 0).average().orElse(0))
                .totalInErrors(group.stream().mapToLong(m -> m.getInErrors() != null ? m.getInErrors() : 0).sum())
                .totalOutErrors(group.stream().mapToLong(m -> m.getOutErrors() != null ? m.getOutErrors() : 0).sum())
                .totalInDiscards(group.stream().mapToLong(m -> m.getInDiscards() != null ? m.getInDiscards() : 0).sum())
                .totalOutDiscards(group.stream().mapToLong(m -> m.getOutDiscards() != null ? m.getOutDiscards() : 0).sum())
                .avgErrorRatePercent(group.stream().mapToDouble(m -> m.getErrorRatePercent() != null ? m.getErrorRatePercent() : 0).average().orElse(0))
                .totalIterations((int) rxStats.getCount())
                .build();
    }

    private InterfaceMetricDaily aggregateDaily(List<InterfaceMetric> group, String systemName) {
        DoubleSummaryStatistics rxStats = group.stream().mapToDouble(m -> m.getRxBitsPerSec() != null ? m.getRxBitsPerSec() : 0).summaryStatistics();
        DoubleSummaryStatistics txStats = group.stream().mapToDouble(m -> m.getTxBitsPerSec() != null ? m.getTxBitsPerSec() : 0).summaryStatistics();
        InterfaceMetric s = group.get(0);

        return InterfaceMetricDaily.builder()
                .systemName(systemName)
                .dayBucket(s.getSnapTime().truncatedTo(ChronoUnit.DAYS))
                .interfaceIndex(s.getInterfaceIndex())
                .interfaceName(s.getInterfaceName()).interfaceType(s.getInterfaceType()).speedBps(s.getSpeedBps())
                .avgRxBps(rxStats.getAverage()).maxRxBps(rxStats.getMax())
                .avgTxBps(txStats.getAverage()).maxTxBps(txStats.getMax())
                .avgRxPercent(group.stream().mapToDouble(m -> m.getRxPercent() != null ? m.getRxPercent() : 0).average().orElse(0))
                .avgTxPercent(group.stream().mapToDouble(m -> m.getTxPercent() != null ? m.getTxPercent() : 0).average().orElse(0))
                .totalRxBytes(group.stream().mapToLong(m -> m.getRxBytes() != null ? m.getRxBytes() : 0).sum())
                .totalTxBytes(group.stream().mapToLong(m -> m.getTxBytes() != null ? m.getTxBytes() : 0).sum())
                .totalTotalBytes(group.stream().mapToLong(m -> m.getTotalBytes() != null ? m.getTotalBytes() : 0).sum())
                .avgRxPps(group.stream().mapToDouble(m -> m.getRxPacketsPerSec() != null ? m.getRxPacketsPerSec() : 0).average().orElse(0))
                .avgTxPps(group.stream().mapToDouble(m -> m.getTxPacketsPerSec() != null ? m.getTxPacketsPerSec() : 0).average().orElse(0))
                .totalInErrors(group.stream().mapToLong(m -> m.getInErrors() != null ? m.getInErrors() : 0).sum())
                .totalOutErrors(group.stream().mapToLong(m -> m.getOutErrors() != null ? m.getOutErrors() : 0).sum())
                .totalInDiscards(group.stream().mapToLong(m -> m.getInDiscards() != null ? m.getInDiscards() : 0).sum())
                .totalOutDiscards(group.stream().mapToLong(m -> m.getOutDiscards() != null ? m.getOutDiscards() : 0).sum())
                .avgErrorRatePercent(group.stream().mapToDouble(m -> m.getErrorRatePercent() != null ? m.getErrorRatePercent() : 0).average().orElse(0))
                .totalScans((int) rxStats.getCount())
                .build();
    }

    private void saveLog(String level, String source, String systemName, String message) {
        try {
            SystemLog log = SystemLog.builder()
                    .timestamp(Instant.now()).level(level).source(source)
                    .systemName(systemName).message(message).details("")
                    .build();
            logRepo.save(log);
        } catch (Exception ignored) {}
    }
}