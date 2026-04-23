/**
 * Author: Eric Huang
 * User:eric.huang
 * Date:4/3/2025
 * Time:1:52 PM
 */

package com.fps.svmes.component;

import com.mongodb.client.MongoClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MongoConnectionCheck implements CommandLineRunner {

    private final MongoClient mongoClient;

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database}")
    private String mongoDb;

    @Override
    public void run(String... args) {
        try {
            mongoClient.getDatabase(mongoDb).listCollectionNames().first();

            log.info("=====================================================");
            log.info("➡️ MongoDB URI: {}", mongoUri);
            log.info("➡️ MongoDB Database: {}", mongoDb);
            log.info("✅ MongoDB connection established.");
            log.info("=====================================================");
        } catch (Exception e) {
            log.error("❌ Failed to connect to MongoDB: {}", e.getMessage());
            throw new RuntimeException("MongoDB connection check failed", e);
        }
    }
}