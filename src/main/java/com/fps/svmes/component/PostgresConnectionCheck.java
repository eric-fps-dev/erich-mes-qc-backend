/**
 * Author: Eric Huang
 * User:eric.huang
 * Date:4/3/2025
 * Time:10:10 AM
 */

package com.fps.svmes.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostgresConnectionCheck implements CommandLineRunner {

    private final DataSource dataSource;

    @Override
    public void run(String... args) {
        try (Connection conn = dataSource.getConnection()) {
            String dbUrl = conn.getMetaData().getURL();

            log.info("=====================================================");
            log.info("➡️ Postgres URL: {}", dbUrl);
            log.info("✅ Postgres connection established.");
            log.info("=====================================================");
        } catch (SQLException e) {
            log.error("❌ Failed to connect to PostgreSQL: {}", e.getMessage());
            throw new RuntimeException("PostgreSQL connection check failed", e);
        }
    }

}
