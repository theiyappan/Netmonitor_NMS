package org.example.model;

import  com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InterfaceData {
    private Integer index;
    private String name;
    private String type;
    private String status;

    @JsonProperty("speedbps")
    private Integer speedbps;

    @JsonProperty("macAddress")
    private String macAddress;

    private TrafficMetrics traffic;
    private UtilizationMetrics utilization;
    private PacketMetrics packets;
    private ErrorMetrics errors;

    private String timestamp;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrafficMetrics {
        @JsonProperty("rxBytes")
        private Long rxBytes;

        @JsonProperty("txBytes")
        private Long txBytes;

        @JsonProperty("totalBytes")
        private Long totalBytes;

        @JsonProperty("rxBitsPerSec")
        private Double rxBitsPerSec;

        @JsonProperty("txBitsPerSec")
        private Double txBitsPerSec;

        @JsonProperty("totalBitsPerSec")
        private Double totalBitsPerSec;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UtilizationMetrics {
        @JsonProperty("rxPercent")
        private Double rxPercent;

        @JsonProperty("txPercent")
        private Double txPercent;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PacketMetrics {
        @JsonProperty("rxPackets")
        private Long rxPackets;

        @JsonProperty("txPackets")
        private Long txPackets;

        @JsonProperty("rxPacketsPerSec")
        private Double rxPacketsPerSec;

        @JsonProperty("txPacketsPerSec")
        private Double txPacketsPerSec;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorMetrics {
        @JsonProperty("inErrors")
        private Long inErrors;

        @JsonProperty("outErrors")
        private Long outErrors;

        @JsonProperty("inDiscards")
        private Long inDiscards;

        @JsonProperty("outDiscards")
        private Long outDiscards;

        @JsonProperty("errorRatePercent")
        private Double errorRatePercent;
    }
}
