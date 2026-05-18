package com.network.snmp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.network.snmp.repository.mysql")
public class SnmpMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SnmpMonitorApplication.class, args);
    }
}