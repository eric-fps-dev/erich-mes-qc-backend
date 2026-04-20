package com.fps.svmes.services;

import com.fps.svmes.dto.dtos.subscription.WeeklyReportSubscriptionDTO;

import java.util.List;

public interface WeeklyReportSubscriptionService {

    List<WeeklyReportSubscriptionDTO> getAllSubscriptions();

    List<WeeklyReportSubscriptionDTO> getActiveSubscriptions();

    WeeklyReportSubscriptionDTO addSubscription(WeeklyReportSubscriptionDTO dto);

    void removeSubscription(Integer id);

    void toggleSubscriptionStatus(Integer id, Boolean isActive);

    WeeklyReportSubscriptionDTO updateSubscription(Integer id, WeeklyReportSubscriptionDTO dto);

    boolean isEmailSubscribed(String email);
}
