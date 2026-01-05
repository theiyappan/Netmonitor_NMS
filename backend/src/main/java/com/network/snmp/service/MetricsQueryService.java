package com.network.snmp.service;

import com.network.snmp.model.InterfaceMetric;
import com.network.snmp.model.InterfaceMetricDaily;
import com.network.snmp.model.InterfaceMetricHourly;
import com.network.snmp.repository.cassandra.InterfaceMetricDailyRepository;
import com.network.snmp.repository.cassandra.InterfaceMetricHourlyRepository;
import com.network.snmp.repository.cassandra.InterfaceMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MetricsQueryService {

    private final InterfaceMetricRepository rawRepo;
    private final InterfaceMetricHourlyRepository hourlyRepo;
    private final InterfaceMetricDailyRepository dailyRepo;

    public List<InterfaceMetric> getLatestMetrics(String systemName) {
        Instant now = Instant.now();
        Instant cutoff = now.minus(10, ChronoUnit.MINUTES);

        List<InterfaceMetric> recentData = rawRepo.findBySystemNameRecent(systemName, cutoff);

        if (recentData.isEmpty()) return List.of();

        Instant latestTime = recentData.stream()
                .map(InterfaceMetric::getSnapTime)
                .max(Comparator.naturalOrder())
                .orElse(null);

        if (latestTime == null) return List.of();

        return recentData.stream()
                .filter(m -> m.getSnapTime().equals(latestTime))
                .collect(Collectors.toList());
    }

    public List<InterfaceMetric> getLatestMetrics(String systemName, Instant cutoff) {
        return rawRepo.findBySystemNameRecent(systemName, cutoff);
    }

    public List<InterfaceMetric> getRawMetricsByTime(String systemName, int hours) {
        Instant end = Instant.now();
        Instant start = end.minus(hours, ChronoUnit.HOURS);
        return rawRepo.findBySystemNameAndTimeRange(systemName, start, end);
    }

    public List<InterfaceMetric> getRawInterfaceHistory(String systemName, int interfaceIndex, int hours) {
        Instant end = Instant.now();
        Instant start = end.minus(hours, ChronoUnit.HOURS);
        return rawRepo.findBySystemNameAndInterfaceIndexAndTimeRange(systemName, interfaceIndex, start, end);
    }

    public List<InterfaceMetricHourly> getHourlyMetrics(String systemName, int hours) {
        Instant end = Instant.now();
        Instant start = end.minus(hours, ChronoUnit.HOURS);
        return hourlyRepo.findBySystemAndTime(systemName, start, end);
    }

    public List<InterfaceMetricDaily> getDailyMetrics(String systemName, int days) {
        Instant end = Instant.now();
        Instant start = end.minus(days, ChronoUnit.DAYS);
        return dailyRepo.findBySystemAndTime(systemName, start, end);
    }
    public List<InterfaceMetric> getAllMetrics(String systemName) {
        return rawRepo.findBySystemName(systemName);
    }
}