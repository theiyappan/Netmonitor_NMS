package org.example.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NetworkSnap {
    private String SystemName;
    private Instant Snaptime;

    private String systemDesc;
    private String uptime;
    private Integer totalInterface;
    private Integer activeInterface;

    private String Interfacejson;
}
