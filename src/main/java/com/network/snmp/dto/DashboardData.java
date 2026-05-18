package com.network.snmp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardData {
    private String systemName;
    private String ipAddress;
    private Integer totalInterfaces;
    private Integer activeInterfaces;
    private Instant lastScanTime;
    private List<InterfaceCardData> interfaces;
}