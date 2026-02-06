///**
// * Author: Eric Huang
// * User:eric.huang
// * Date:4/2/2025
// * Time:6:05 PM
// */
//
//package com.fps.svmes.component;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.data.redis.connection.RedisConnection;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Component;
//
//import java.util.Objects;
//
//@Component
//@RequiredArgsConstructor
//public class RedisConnectionCheck implements CommandLineRunner {
//    private final RedisTemplate<String, Object> redisTemplate;
//
//    @Value("${spring.data.redis.host}")
//    private String redisHost;
//
//    @Value("${spring.data.redis.port}")
//    private int redisPort;
//
//    @Override
//    public void run(String... args) {
//        RedisConnection connection = Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection();
//        System.out.println("➡️ Redis host: " + redisHost + ":" + redisPort);
//        System.out.println("✅ Redis connect test:  PING - " + connection.ping());
//    }
//}
