package com.disasterreliefnet.receiver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Receiver Service – manages disaster victim/beneficiary profiles
 * and aid requests throughout their full lifecycle.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
public class ReceiverServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReceiverServiceApplication.class, args);
    }
}
