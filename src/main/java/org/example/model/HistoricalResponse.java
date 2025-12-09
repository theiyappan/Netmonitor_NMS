package org.example.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalResponse {
    private String systemName;
    private Integer count;
    private List<SnapshotData> snapshots;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SnapshotData {
        private Instant snapshotTime;
        private SystemInfo systemInfo;
        private List<InterfaceData> interfaces;
    }
}
