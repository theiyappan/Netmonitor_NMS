package com.network.snmp.controller;

import com.network.snmp.model.InterfaceMetric;
import com.network.snmp.service.MetricsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final MetricsQueryService metricsQueryService;

    @GetMapping("/{systemName}/latest")
    public ResponseEntity<List<InterfaceMetric>> getLatestMetrics(@PathVariable String systemName) {
        return ResponseEntity.ok(metricsQueryService.getLatestMetrics(systemName));
    }

    @GetMapping("/{systemName}/history")
    public ResponseEntity<List<InterfaceMetric>> getMetricsByTimeRange(
            @PathVariable String systemName,
            @RequestParam(defaultValue = "6") int hours) {
        return ResponseEntity.ok(metricsQueryService.getRawMetricsByTime(systemName, hours));
    }

    @GetMapping("/{systemName}/all")
    public ResponseEntity<List<InterfaceMetric>> getAllMetrics(@PathVariable String systemName) {
        return ResponseEntity.ok(metricsQueryService.getAllMetrics(systemName));
    }

    @GetMapping("/{systemName}/interface/{interfaceIndex}")
    public ResponseEntity<List<InterfaceMetric>> getInterfaceHistory(
            @PathVariable String systemName,
            @PathVariable int interfaceIndex) {
        return ResponseEntity.ok(metricsQueryService.getRawInterfaceHistory(systemName, interfaceIndex, 24));
    }

    @GetMapping("/{systemName}/interface/{interfaceIndex}/history")
    public ResponseEntity<List<InterfaceMetric>> getInterfaceHistoryByTimeRange(
            @PathVariable String systemName,
            @PathVariable int interfaceIndex,
            @RequestParam(defaultValue = "6") int hours) {
        return ResponseEntity.ok(metricsQueryService.getRawInterfaceHistory(systemName, interfaceIndex, hours));
    }
}