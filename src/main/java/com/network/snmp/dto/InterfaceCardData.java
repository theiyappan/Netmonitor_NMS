package com.network.snmp.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InterfaceCardData {
    private int interfaceIndex;
    private String interfaceName;
    private String status;
    private String interfaceType;
    private String speedFormatted;

    private String rxRate;
    private String txRate;
    private Double rxUtilization;
    private Double txUtilization;

    private String rxPps;
    private String txPps;
    private long errorCount;
    private long discardCount;
    private String totalVolume;
}