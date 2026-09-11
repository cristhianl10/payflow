package com.payflow;

import org.springframework.boot.SpringApplication;
import org.testcontainers.containers.PostgreSQLContainer;

/** Isolated browser-test server: the database is disposed of when this process exits. */
public class BrowserTestApplication {
    public static void main(String[] args) {
        PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
        postgres.start();
        Runtime.getRuntime().addShutdownHook(new Thread(postgres::stop));
        SpringApplication.run(PayflowApplication.class,
                "--spring.profiles.active=test",
                "--server.port=18080",
                "--spring.datasource.url=" + postgres.getJdbcUrl(),
                "--spring.datasource.username=" + postgres.getUsername(),
                "--spring.datasource.password=" + postgres.getPassword(),
                "--payflow.allowed-origins=http://127.0.0.1:5174",
                "--payflow.auth.requests-per-minute=1000");
    }
}
