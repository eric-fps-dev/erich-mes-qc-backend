package com.fps.svmes.controllers;

import com.fps.svmes.dto.dtos.subscription.WeeklyReportSubscriptionDTO;
import com.fps.svmes.dto.responses.ResponseResult;
import com.fps.svmes.services.WeeklyReportSubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/weekly-report-subscription")
@RequiredArgsConstructor
@Tag(name = "Weekly Report Subscription API", description = "API for managing weekly AI report email subscriptions")
public class WeeklyReportSubscriptionController {

    private final WeeklyReportSubscriptionService subscriptionService;
    private final com.fps.svmes.schedulers.WeeklyReportScheduler weeklyReportScheduler;

    @GetMapping("")
    @Operation(summary = "Get all subscriptions", description = "Returns all weekly report subscriptions")
    public ResponseResult<List<WeeklyReportSubscriptionDTO>> getAllSubscriptions() {
        try {
            List<WeeklyReportSubscriptionDTO> subscriptions = subscriptionService.getAllSubscriptions();
            log.info("Subscriptions retrieved, count: {}", subscriptions.size());
            return ResponseResult.success(subscriptions);
        } catch (Exception e) {
            log.error("Error retrieving subscriptions", e);
            return ResponseResult.fail("Error retrieving subscriptions", e);
        }
    }

    @PostMapping("/trigger-test")
    @Operation(summary = "Trigger test send", description = "Manually trigger the weekly report email job for all active subscribers")
    public ResponseResult<String> triggerTestSend(@RequestParam(required = false, defaultValue = "en") String lang) {
        try {
            log.info("Manually triggering weekly report test send with language: {}...", lang);
            weeklyReportScheduler.sendWeeklyReportsManual(lang);
            return ResponseResult.success("Weekly report job triggered successfully with language " + lang + ". Check logs for details.");
        } catch (Exception e) {
            log.error("Error triggering test send", e);
            return ResponseResult.fail("Error triggering test send: " + e.getMessage(), e);
        }
    }

    @PostMapping("/{id}/send-now")
    @Operation(summary = "Send report immediately", description = "Trigger sending the weekly report to a specific subscriber immediately")
    public ResponseResult<String> sendNow(@PathVariable Integer id) {
        try {
            log.info("Manually sending report to subscriber id: {}...", id);
            weeklyReportScheduler.sendReportToSubscriber(id);
            return ResponseResult.success("Report sent successfully.");
        } catch (Exception e) {
            log.error("Error sending report to subscriber", e);
            return ResponseResult.fail("Error sending report: " + e.getMessage(), e);
        }
    }

    @PostMapping("")
    @Operation(summary = "Add subscription", description = "Add a new email subscription")
    public ResponseResult<WeeklyReportSubscriptionDTO> addSubscription(@RequestBody WeeklyReportSubscriptionDTO dto) {
        try {
            WeeklyReportSubscriptionDTO created = subscriptionService.addSubscription(dto);
            log.info("Subscription created for email: {}", dto.getEmail());
            return ResponseResult.success(created);
        } catch (Exception e) {
            log.error("Error adding subscription", e);
            return ResponseResult.fail("Error adding subscription: " + e.getMessage(), e);
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update subscription", description = "Update email or language for a subscription")
    public ResponseResult<WeeklyReportSubscriptionDTO> updateSubscription(@PathVariable Integer id, @RequestBody WeeklyReportSubscriptionDTO dto) {
        try {
            WeeklyReportSubscriptionDTO updated = subscriptionService.updateSubscription(id, dto);
            log.info("Subscription updated, id: {}", id);
            return ResponseResult.success(updated);
        } catch (Exception e) {
            log.error("Error updating subscription", e);
            return ResponseResult.fail("Error updating subscription: " + e.getMessage(), e);
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove subscription", description = "Remove a subscription by ID (soft delete)")
    public ResponseResult<Void> removeSubscription(@PathVariable Integer id) {
        try {
            subscriptionService.removeSubscription(id);
            log.info("Subscription removed, id: {}", id);
            return ResponseResult.success(null);
        } catch (Exception e) {
            log.error("Error removing subscription", e);
            return ResponseResult.fail("Error removing subscription", e);
        }
    }

    @PutMapping("/{id}/toggle")
    @Operation(summary = "Toggle subscription", description = "Enable/disable a subscription")
    public ResponseResult<Void> toggleSubscription(
            @PathVariable Integer id,
            @RequestParam Boolean isActive) {
        try {
            subscriptionService.toggleSubscriptionStatus(id, isActive);
            log.info("Subscription toggled, id: {}, isActive: {}", id, isActive);
            return ResponseResult.success(null);
        } catch (Exception e) {
            log.error("Error toggling subscription", e);
            return ResponseResult.fail("Error toggling subscription", e);
        }
    }
}
