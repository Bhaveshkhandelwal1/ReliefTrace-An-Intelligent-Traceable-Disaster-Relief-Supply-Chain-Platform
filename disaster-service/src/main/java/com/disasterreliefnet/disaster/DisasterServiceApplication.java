package com.disasterreliefnet.disaster;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Microservice managing disaster incidents and severity alerts.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class DisasterServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DisasterServiceApplication.class, args);
    }
}
