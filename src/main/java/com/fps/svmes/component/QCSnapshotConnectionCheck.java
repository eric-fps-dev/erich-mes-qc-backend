/**
 * Author: Eric Huang
 * User:eric.huang
 * Date:4/30/2026
 * Time:6:15 PM
 */

package com.fps.svmes.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URL;

@Slf4j
@Component
public class QCSnapshotConnectionCheck implements CommandLineRunner {

    @Value("${qc.snapshot.api.url}")
    private String qcApiUrl;

    @Override
    public void run(String... args) {
        String healthUrl = qcApiUrl.endsWith("/") ? qcApiUrl + "health" : qcApiUrl + "/health";

        log.info("=====================================================");
        log.info("➡️ Checking QC Snapshot API: {}", qcApiUrl);

        try {
            URL url = new URL(healthUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                log.info("✅ QC Snapshot API is reachable and healthy (HTTP 200).");
            } else {
                log.error("❌ QC Snapshot API Health Check Failed! HTTP Status: {}", responseCode);
                throw new RuntimeException("Mandatory service [QC Snapshot API] returned status " + responseCode);
            }

        } catch (Exception e) {
            log.error("❌ Critical Error: Could not connect to QC Snapshot API: {}", e.getMessage());
            throw new RuntimeException("Failed to establish connection to QC Snapshot API at " + healthUrl, e);
        }
        log.info("=====================================================");
    }
}
