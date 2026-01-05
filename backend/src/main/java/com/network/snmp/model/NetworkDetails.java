package com.network.snmp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "network_details")
public class NetworkDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String systemName;

    @Column(nullable = false)
    private String ipAddress;

    private Integer snmpPort;

    private Instant lastScanTime;
    private Instant firstScanTime;

    private Integer totalInterfaces;
    private Integer totalScans;


    @Builder.Default
    private Double thresholdUtil = 90.0;

    @Builder.Default
    private Double thresholdError = 0.5;
}