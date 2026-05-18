package com.network.snmp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterfaceGraphData {
    private String systemName;
    private int interfaceIndex;
    private String interfaceName;
    private List<DataPoint> rxData;
    private List<DataPoint> txData;
    private List<DataPoint> utilizationData;
    private InterfaceSummary summary;
}