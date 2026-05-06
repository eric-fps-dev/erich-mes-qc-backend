/**
 * Author: Eric Huang
 * User:eric.huang
 * Date:4/28/2026
 * Time:10:08 AM
 */

package com.fps.svmes.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMQConnectionCheck implements CommandLineRunner {

    private final ConnectionFactory connectionFactory;

    @Value("${spring.rabbitmq.host}")
    private String rabbitHost;

    @Value("${spring.rabbitmq.port}")
    private int rabbitPort;

    @Override
    public void run(String... args) {
        try (Connection connection = connectionFactory.createConnection()) {

            log.info("=====================================================");
            log.info("➡️ RabbitMQ host: {}:{}", rabbitHost, rabbitPort);
            log.info("✅ RabbitMQ connect test: SUCCESS (Connected to {})", connection.getDelegate());
            log.info("=====================================================");

        } catch (Exception e) {
            log.error("❌ Failed to connect to RabbitMQ: {}", e.getMessage());
            throw new RuntimeException("RabbitMQ connection check failed", e);
        }
    }
}
