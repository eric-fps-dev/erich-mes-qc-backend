/**
 * Author: Eric Huang
 * User:eric.huang
 * Date:4/20/2026
 * Time:2:08 PM
 */

package com.fps.svmes.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisConnectionCheck implements CommandLineRunner {
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Override
    public void run(String... args) {
        try (RedisConnection connection = Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection()) {

            log.info("=====================================================");
            log.info("➡️ Redis host: {}:{}", redisHost, redisPort);
            log.info("✅ Redis connect test: PING - {}", connection.ping());
            log.info("=====================================================");

        } catch (Exception e) {
            log.error("❌ Failed to connect to Redis: {}", e.getMessage());
            throw new RuntimeException("Redis connection check failed", e);
        }
    }
}
