package com.example.demo;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.ConfluentKafkaContainer;

@TestConfiguration(proxyBeanMethods = false)
public class ContainerConfig {
    @Bean
    @ServiceConnection(name = "redis")
    public GenericContainer redis() {
        return new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
    }

    @Bean
    @ServiceConnection
    public PostgreSQLContainer postgreSQLContainer() {
        return new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("workshop");
    }

    @Bean
    @ServiceConnection
    public ConfluentKafkaContainer confluentKafkaContainer() {
        return new ConfluentKafkaContainer("confluentinc/cp-kafka:7.5.0");
    }

}
