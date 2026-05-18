package com.network.snmp.service;

import com.network.snmp.model.NetworkDetails;
import com.network.snmp.repository.mysql.NetworkDetailsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.util.SubnetUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscoveryService {

    private final SnmpService snmpService;
    private final NetworkDetailsRepository networkRepo;
    private final DashboardService dashboardService;

    public List<String> scanSubnet(String cidr, String community) {
        log.info("Starting discovery for subnet: {}", cidr);
        List<String> foundDevices = new ArrayList<>();

        try {
            SubnetUtils utils = new SubnetUtils(cidr);
            utils.setInclusiveHostCount(true);
            String[] allIps = utils.getInfo().getAllAddresses();

            dashboardService.logToDb("INFO", "Discovery", "System",
                    "Scanning " + allIps.length + " IPs in " + cidr);

            List<NetworkDetails> newDevices = Arrays.stream(allIps)
                    .parallel() // <--- Fast Parallel Processing
                    .map(ip -> {
                        String sysName = snmpService.discover(ip, community);
                        if (sysName != null && !sysName.isEmpty() && !sysName.contains("noSuchObject")) {
                            return createDeviceObject(ip, sysName);
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            for (NetworkDetails device : newDevices) {
                if (networkRepo.findBySystemName(device.getSystemName()).isEmpty()) {
                    networkRepo.save(device);
                    foundDevices.add(device.getSystemName() + " (" + device.getIpAddress() + ")");

                    dashboardService.logToDb("STARTUP", "Discovery", "System",
                            "Auto-discovered: " + device.getSystemName());
                }
            }

            dashboardService.logToDb("INFO", "Discovery", "System",
                    "Discovery complete. Found " + newDevices.size() + " devices.");

        } catch (Exception e) {
            log.error("Discovery failed: {}", e.getMessage());
            dashboardService.logToDb("ERROR", "Discovery", "System", "Failed: " + e.getMessage());
            throw new RuntimeException("Invalid CIDR (e.g. 192.168.1.0/24)");
        }

        return foundDevices;
    }

    private NetworkDetails createDeviceObject(String ip, String sysName) {
        return NetworkDetails.builder()
                .systemName(sysName)
                .ipAddress(ip)
                .snmpPort(161)
                .firstScanTime(Instant.now())
                .thresholdUtil(90.0)
                .thresholdError(0.5)
                .totalInterfaces(0)
                .totalScans(0)
                .build();
    }
}