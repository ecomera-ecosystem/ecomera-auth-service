package com.youssef.ecomeraauthservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class EcomeraAuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EcomeraAuthServiceApplication.class, args);
    }
}
