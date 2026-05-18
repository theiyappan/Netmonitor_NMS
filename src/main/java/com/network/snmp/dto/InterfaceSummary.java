package com.network.snmp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterfaceSummary {
    private String interfaceName;
    private String status;
    private double avgRxBitsPerSec;
    private double avgTxBitsPerSec;
    private double maxRxBitsPerSec;
    private double maxTxBitsPerSec;
    private long totalErrors;
    private long speedBps;
}