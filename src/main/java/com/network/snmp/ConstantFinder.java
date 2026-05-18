package com.network.snmp;

// ✅ ALL IMPORTS INCLUDED
import org.snmp4j.Snmp;
import org.snmp4j.PDU;
import org.snmp4j.ScopedPDU;
import org.snmp4j.UserTarget;
import org.snmp4j.event.ResponseEvent; // <--- Crucial Import
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.*;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.TransportMapping;

public class ConstantFinder {

    public static void main(String[] args) {
        System.out.println("=== SNMPv3 Level 2 FIX (SNMP4J) ===");
        System.out.println("User: authuser");
        System.out.println("Auth: MD5");

        Snmp snmp = null;

        try {
            // 1. CRITICAL FIX: Register Protocols
            // This ensures MD5 is loaded so 'AUTH_NOPRIV' works.
            SecurityProtocols.getInstance().addDefaultProtocols();
            SecurityProtocols.getInstance().addAuthenticationProtocol(new AuthMD5());

            // 2. Initialize Transport
            TransportMapping<?> transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);

            // 3. Setup USM
            USM usm = new USM(SecurityProtocols.getInstance(), new OctetString(MPv3.createLocalEngineID()), 0);
            SecurityModels.getInstance().addSecurityModel(usm);
            transport.listen();

            // 4. Add User (AuthNoPriv)
            // 'authuser' with MD5, but NULL privacy
            UsmUser user = new UsmUser(
                    new OctetString("authuser"),
                    AuthMD5.ID,
                    new OctetString("authpassword"),
                    null, null // No Priv
            );
            snmp.getUSM().addUser(new OctetString("authuser"), user);

            // 5. Setup Target
            UserTarget target = new UserTarget();
            target.setAddress(new UdpAddress("127.0.0.1/1024"));
            target.setRetries(2);
            target.setTimeout(3000);
            target.setVersion(SnmpConstants.version3);
            target.setSecurityLevel(SecurityLevel.AUTH_NOPRIV);
            target.setSecurityName(new OctetString("authuser"));

            // 6. Send Request
            System.out.println("Sending GET Request...");
            ScopedPDU pdu = new ScopedPDU();
            pdu.setType(PDU.GET);
            pdu.add(new VariableBinding(new OID("1.3.6.1.2.1.1.1.0")));

            ResponseEvent response = snmp.send(pdu, target);

            // 7. Check Response
            if (response != null && response.getResponse() != null) {
                PDU resp = response.getResponse();
                if (resp.getErrorStatus() == PDU.noError) {
                    System.out.println("✅ SUCCESS! Response: " + resp.getVariableBindings().get(0).getVariable());
                } else {
                    System.out.println("❌ SNMP Error: " + resp.getErrorStatusText());
                }
            } else {
                System.out.println("❌ Timeout. (Check if snmpd is running)");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}