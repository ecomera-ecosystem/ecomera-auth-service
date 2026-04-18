package com.ecomera.auth;

import org.springframework.boot.SpringApplication;

public class TestEcomeraAuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(EcomeraAuthServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
