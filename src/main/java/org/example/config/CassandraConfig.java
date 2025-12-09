package org.example.config;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.shaded.guava.common.net.InetAddresses;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.InetSocketAddress;

@Configuration
public class CassandraConfig {

    @Value("${spring.data.cassandra.contact-points}")
    private String contactPoints;

    @Value("${spring.data.cassandra.port}")
    private int port;

    @Value("${spring.data.cassandra.keyspace-name}")
    private String keyspace;

    @Value("${spring.data.cassandra.local-datacenter}")
    private String localDatacenter;

    @Bean
    public CqlSession cqlSession() {
        System.out.println("Connecting to Cassandra...");
        System.out.println("  Contact Point: " + contactPoints + ":" + port);
        System.out.println("  Keyspace: " + keyspace);
        System.out.println("  Datacenter: " + localDatacenter);

        CqlSession Session=CqlSession.builder()
                .addContactPoint(new InetSocketAddress(contactPoints,port))
                .withLocalDatacenter(localDatacenter)
                .withKeyspace(keyspace).build();

        return Session;
    }
}
