/**
 * Author: Eric Huang
 * User:eric.huang
 * Date:4/3/2025
 * Time:1:52 PM
 */

package com.fps.svmes.component;

import com.mongodb.client.MongoClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MongoConnectionCheck implements CommandLineRunner {

    private final MongoClient mongoClient;

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Override
    public void run(String... args) {
        try {
            mongoClient.listDatabaseNames().first();
            System.out.println("➡️ MongoDB URI: " + mongoUri);
            System.out.println("✅ MongoDB connection established.");
        } catch (Exception e) {
            System.err.println("❌ Failed to connect to MongoDB: " + e.getMessage());
            throw new RuntimeException("MongoDB connection check failed", e);
        }
    }
}

