package com.disasterreliefnet.transport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Transport Service.
 *
 * <p>Manages driver profiles, vehicles and shipments, and emits lifecycle
 * tracking events (assigned → picked-up → delivered / disputed).</p>
 */
@SpringBootApplication
@EnableScheduling
@EnableFeignClients
public class TransportServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransportServiceApplication.class, args);
    }
}
