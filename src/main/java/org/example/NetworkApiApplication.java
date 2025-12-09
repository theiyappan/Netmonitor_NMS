package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NetworkApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(NetworkApiApplication.class, args);

        System.out.println("NetworkApiApplication started");
    }
}
