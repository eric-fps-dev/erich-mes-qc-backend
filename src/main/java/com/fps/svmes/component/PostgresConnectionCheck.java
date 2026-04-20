/**
 * Author: Eric Huang
 * User:eric.huang
 * Date:4/3/2025
 * Time:10:10 AM
 */

package com.fps.svmes.component;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
@RequiredArgsConstructor
public class PostgresConnectionCheck implements CommandLineRunner {

    private final DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            String dbHost = conn.getMetaData().getURL();
            System.out.println("➡️ PostgreSQL is up. host: " + dbHost);
        } catch (SQLException e) {
            System.err.println("❌ Failed to connect to PostgreSQL: " + e.getMessage());
            throw e; // optionally fail the app startup
        }
    }

}
