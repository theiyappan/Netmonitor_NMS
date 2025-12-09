package org.example.controller;

import org.example.repository.NetworkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/network")
public class HealthController {

    @Autowired
    private NetworkRepository networkRepository;

    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Network Monitor API is running");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("service", "Network Monitoring REST API");

        try {
            boolean hasData = networkRepository.FindLatest("theiyappan_loq").isPresent();
            response.put("database", "Connected");
            response.put("hasData", hasData);
        } catch (Exception e) {
            response.put("database", "Error: " + e.getMessage());
        }

        return response;
    }
}