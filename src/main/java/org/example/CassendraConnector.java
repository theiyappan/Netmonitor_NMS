package org.example;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class CassendraConnector {
    private CqlSession session;
    public void main(String node, Integer Port,String datacenter){
        session= CqlSession.builder()
                .addContactPoint(new InetSocketAddress(node,Port))
                .withLocalDatacenter(datacenter)
                .build();
    }
}
