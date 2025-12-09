package org.example.controller;

import org.example.model.HistoricalResponse;
import org.example.model.NetworkResponse;
import org.example.service.NetworkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.HashMap;


@RestController
@RequestMapping("/api/network")
@CrossOrigin(originPatterns = "*")
public class NetworkController {

    @Autowired
    private NetworkService networkService;

    @GetMapping("/latest")
    public ResponseEntity<?> getLatestNetwork(@RequestParam(required = false,defaultValue = "localhost.localdomain")String systemName) {
        try {
            NetworkResponse networkResponse=networkService.getLatestNetwork(systemName);
            return ResponseEntity.ok(networkResponse);
        }
        catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistoricalSnapshots(@RequestParam(required = false,defaultValue = "localhost.localdomain")String systemName,
                                                    @RequestParam(required = false,defaultValue = "10")int limit){
        try {
            HistoricalResponse historicalResponse=networkService.getHistoricalSnapshots(systemName,limit);
            return ResponseEntity.ok(historicalResponse);
        }
        catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @GetMapping("/range")
    public ResponseEntity<?> getSnapshotbytime(@RequestParam(required = false,defaultValue = "localhost.localdomain")String systemName,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)Instant startTime,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)Instant endTime){
        try{
            HistoricalResponse historicalResponse=networkService.getSnapshotsByTimeRange(systemName, startTime, endTime);
            return ResponseEntity.ok(historicalResponse);
        }
        catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getSnapshotsummary(@RequestParam(required = false,defaultValue = "localhost.localdomain")String systemName) {
        try {
            Map<String, Object> summary=networkService.getSnapshotSummary(systemName);
            return ResponseEntity.ok(summary);
        }
        catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @GetMapping("/systems")
    public ResponseEntity<?> listSystemNames() {
        try {
            List<String> systemNames = networkService.getAllSystemNames();

            Map<String, Object> response = new HashMap<>();
            response.put("systems", systemNames);
            response.put("count", systemNames.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
