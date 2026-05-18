package com.network.snmp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String systemName;
    private Integer interfaceIndex;
    private String component;
    private String alertType;
    private String severity;

    @Column(length = 1000)
    private String message;

    private boolean active;
    private Instant startTime;
    private Instant lastActiveTime;
    private Instant endTime;
}