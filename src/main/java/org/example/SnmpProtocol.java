import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;

public class SnmpProtocol {

    public static void main(String[] args) {
        String host = "127.0.0.1";
        String community = "public";
        int port = 161;
        String[][] oids = {
                {"System Description", "1.3.6.1.2.1.1.1.0"},
                {"System Uptime", "1.3.6.1.2.1.1.3.0"},
                {"System Contact", "1.3.6.1.2.1.1.4.0"},
                {"System Name", "1.3.6.1.2.1.1.5.0"},
                {"System Location", "1.3.6.1.2.1.1.6.0"}
        };

        for (String[] oidInfo : oids) {
            String name = oidInfo[0];
            String oid = oidInfo[1];
            String result = snmpGet(host, port, community, oid);
            System.out.println("\n" + name + ":");
            System.out.println("  OID: " + oid);
            System.out.println("  Value: " + result);
        }
    }

    public static String snmpGet(String host, int port, String community, String oid) {
        try {
            TransportMapping<?> transport = new DefaultUdpTransportMapping();
            transport.listen();
            CommunityTarget<Address> target = new CommunityTarget<>();
            target.setCommunity(new OctetString(community));
            target.setAddress(new UdpAddress(host + "/" + port));
            target.setRetries(2);
            target.setTimeout(1500);
            target.setVersion(SnmpConstants.version2c);
            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(oid)));
            pdu.setType(PDU.GET);
            Snmp snmp = new Snmp(transport);
            ResponseEvent<?> response = snmp.send(pdu, target);
            if (response != null) {
                PDU responsePDU = response.getResponse();

                if (responsePDU != null) {
                    if (responsePDU.getErrorStatus() == PDU.noError) {
                        Variable variable = responsePDU.get(0).getVariable();
                        snmp.close();
                        return variable.toString();
                    } else {
                        snmp.close();
                        return "Error: " + responsePDU.getErrorStatusText();
                    }
                } else {
                    snmp.close();
                    return "Error: No response from agent";
                }
            } else {
                snmp.close();
                return "Error: Request timeout";
            }
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        }
    }
}
