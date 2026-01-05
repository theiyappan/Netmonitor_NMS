package com.network.snmp.service;

import com.network.snmp.model.InterfaceMetric;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TreeEvent;
import org.snmp4j.util.TreeUtils;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SnmpService {

    private final Snmp snmp;

    public SnmpService() throws Exception {
        TransportMapping<?> transport = new DefaultUdpTransportMapping();
        this.snmp = new Snmp(transport);
        transport.listen();
    }

    public boolean isReachable(String ip) {
        try {
            return InetAddress.getByName(ip).isReachable(2000); // 2-second timeout
        } catch (Exception e) {
            return false;
        }
    }

    public String discover(String ip, String community) {
        try {
            CommunityTarget target = new CommunityTarget();
            target.setCommunity(new OctetString(community));
            target.setAddress(new UdpAddress(ip + "/161"));
            target.setRetries(0);
            target.setTimeout(500);
            target.setVersion(SnmpConstants.version2c);

            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(".1.3.6.1.2.1.1.5.0")));
            pdu.setType(PDU.GET);

            ResponseEvent<?> response = snmp.send(pdu, target);
            if (response != null && response.getResponse() != null) {
                return response.getResponse().get(0).getVariable().toString();
            }
        } catch (Exception ignored) {
            // Device offline or not SNMP enabled - ignore
        }
        return null;
    }

    public List<InterfaceMetric> getInterfaceMetrics(String host, int port, String community) {
        List<InterfaceMetric> metrics = new ArrayList<>();
        String[] oids = {
                ".1.3.6.1.2.1.2.2.1.1", ".1.3.6.1.2.1.2.2.1.2",
                ".1.3.6.1.2.1.2.2.1.8", ".1.3.6.1.2.1.2.2.1.5",
                ".1.3.6.1.2.1.2.2.1.10", ".1.3.6.1.2.1.2.2.1.16",
                ".1.3.6.1.2.1.2.2.1.14", ".1.3.6.1.2.1.2.2.1.20"
        };

        try {
            return new ArrayList<>();

        } catch (Exception e) {
            log.error("Error fetching metrics: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public Map<String, String> walk(String host, int port, String community, String oid) {
        Map<String, String> result = new HashMap<>();
        try {
            CommunityTarget target = createTarget(host, port, community);
            TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());
            List<TreeEvent> events = treeUtils.getSubtree(target, new OID(oid));

            for (TreeEvent event : events) {
                if (event != null && !event.isError()) {
                    VariableBinding[] varBindings = event.getVariableBindings();
                    if (varBindings != null) {
                        for (VariableBinding vb : varBindings) {
                            result.put(vb.getOid().toString(), vb.getVariable().toString());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("SNMP WALK error: {}", e.getMessage());
        }
        return result;
    }

    public String get(String host, int port, String community, String oid) {
        try {
            CommunityTarget target = createTarget(host, port, community);
            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(oid)));
            pdu.setType(PDU.GET);

            ResponseEvent<?> response = snmp.send(pdu, target);
            if (response != null && response.getResponse() != null) {
                return response.getResponse().get(0).getVariable().toString();
            }
            return "Error";
        } catch (Exception e) {
            log.error("SNMP GET error: {}", e.getMessage());
            return "Error";
        }
    }

    // --- 6. DECODE HEX STRING ---
    public String decodeHexString(String hexString) {
        if (hexString == null || hexString.isEmpty() || !hexString.contains(":")) {
            return hexString;
        }
        try {
            String[] hexParts = hexString.split(":");
            StringBuilder decoded = new StringBuilder();
            for (String hex : hexParts) {
                if (!hex.isEmpty()) {
                    int value = Integer.parseInt(hex, 16);
                    if (value > 0 && value < 128) decoded.append((char) value);
                }
            }
            return decoded.toString().trim();
        } catch (Exception e) {
            return hexString;
        }
    }

    private CommunityTarget createTarget(String host, int port, String community) {
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString(community));
        target.setAddress(new UdpAddress(host + "/" + port));
        target.setRetries(2);
        target.setTimeout(3000);
        target.setVersion(SnmpConstants.version2c);
        return target;
    }
}