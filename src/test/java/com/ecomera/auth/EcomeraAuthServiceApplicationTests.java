package com.ecomera.auth;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requires Docker (PostgreSQL, Redis, Zipkin containers). Use controller/repository unit tests instead.")
class EcomeraAuthServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
