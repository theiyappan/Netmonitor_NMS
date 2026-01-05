package com.network.snmp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("interface_metrics")
public class InterfaceMetric {

    @PrimaryKeyColumn(name = "system_name", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private String systemName;

    @PrimaryKeyColumn(name = "snap_time", ordinal = 1, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    private Instant snapTime;

    @PrimaryKeyColumn(name = "interface_index", ordinal = 2, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.ASCENDING)
    private Integer interfaceIndex;

    @Column("interface_name")
    private String interfaceName;

    @Column("interface_type")
    private String interfaceType;

    @Column("status")
    private String status;

    @Column("speed_bps")
    private Long speedBps;

    @Column("mac_address")
    private String macAddress;

    @Column("rx_bytes")
    private Long rxBytes;

    @Column("tx_bytes")
    private Long txBytes;

    @Column("total_bytes")
    private Long totalBytes;

    @Column("rx_bits_per_sec")
    private Double rxBitsPerSec;

    @Column("tx_bits_per_sec")
    private Double txBitsPerSec;

    @Column("total_bits_per_sec")
    private Double totalBitsPerSec;

    @Column("rx_percent")
    private Double rxPercent;

    @Column("tx_percent")
    private Double txPercent;

    @Column("rx_packets")
    private Long rxPackets;

    @Column("tx_packets")
    private Long txPackets;

    @Column("rx_packets_per_sec")
    private Double rxPacketsPerSec;

    @Column("tx_packets_per_sec")
    private Double txPacketsPerSec;

    @Column("in_errors")
    private Long inErrors;

    @Column("out_errors")
    private Long outErrors;

    @Column("in_discards")
    private Long inDiscards;

    @Column("out_discards")
    private Long outDiscards;

    @Column("error_rate_percent")
    private Double errorRatePercent;
}