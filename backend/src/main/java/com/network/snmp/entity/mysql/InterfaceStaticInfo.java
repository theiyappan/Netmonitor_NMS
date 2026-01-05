package com.network.snmp.entity.mysql;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "interface_static_info",
        uniqueConstraints = { @UniqueConstraint(columnNames = { "system_name", "interface_index" }) })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterfaceStaticInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "system_name", nullable = false)
    private String systemName;

    @Column(name = "interface_index", nullable = false)
    private Integer interfaceIndex;

    @Column(name = "interface_name")
    private String interfaceName;

    @Column(name = "interface_type")
    private String interfaceType;

    @Column(name = "mac_address")
    private String macAddress;

    @Column(name = "admin_status")
    private String adminStatus;

    @Column(name = "oper_status")
    private String operStatus;

    @Column(name = "speed_bps")
    private Long speedBps;

    @Column(name = "last_updated")
    private Instant lastUpdated;
}