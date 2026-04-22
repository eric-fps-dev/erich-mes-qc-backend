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

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    @Override
    public void run(String... args) {
        try {
            mongoClient.listDatabaseNames().first();
            System.out.println("--------------------------------------------------");
            System.out.println("➡️ MongoDB Server URI: " + mongoUri);
            System.out.println("➡️ Target Database:    " + databaseName);
            System.out.println("✅ MongoDB connection established successfully.");
            System.out.println("--------------------------------------------------");
        } catch (Exception e) {
            System.err.println("❌ Failed to connect to MongoDB at: " + mongoUri);
            System.err.println("❌ Error Detail: " + e.getMessage());
            throw new RuntimeException("MongoDB connection check failed", e);
        }
    }
}