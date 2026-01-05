package com.network.snmp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("interface_metrics_hourly")
public class InterfaceMetricHourly {
    @PrimaryKeyColumn(name = "system_name", type = PrimaryKeyType.PARTITIONED) private String systemName;
    @PrimaryKeyColumn(name = "hour_bucket", type = PrimaryKeyType.CLUSTERED, ordinal = 0) private Instant hourBucket;
    @PrimaryKeyColumn(name = "interface_index", type = PrimaryKeyType.CLUSTERED, ordinal = 1) private Integer interfaceIndex;

    @Column("interface_name") private String interfaceName;
    @Column("interface_type") private String interfaceType;
    @Column("speed_bps") private Long speedBps;

    @Column("avg_rx_bps") private Double avgRxBps;
    @Column("max_rx_bps") private Double maxRxBps;
    @Column("min_rx_bps") private Double minRxBps;
    @Column("avg_tx_bps") private Double avgTxBps;
    @Column("max_tx_bps") private Double maxTxBps;
    @Column("min_tx_bps") private Double minTxBps;

    @Column("avg_rx_percent") private Double avgRxPercent;
    @Column("avg_tx_percent") private Double avgTxPercent;

    @Column("total_rx_bytes") private Long totalRxBytes;
    @Column("total_tx_bytes") private Long totalTxBytes;
    @Column("total_total_bytes") private Long totalTotalBytes;

    @Column("avg_rx_pps") private Double avgRxPps;
    @Column("avg_tx_pps") private Double avgTxPps;

    @Column("total_in_errors") private Long totalInErrors;
    @Column("total_out_errors") private Long totalOutErrors;
    @Column("total_in_discards") private Long totalInDiscards;
    @Column("total_out_discards") private Long totalOutDiscards;
    @Column("avg_error_rate_percent") private Double avgErrorRatePercent;

    @Column("total_iterations") private Integer totalIterations;
}