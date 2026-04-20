package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.subscription.WeeklyReportSubscriptionDTO;
import com.fps.svmes.models.sql.subscription.WeeklyReportSubscription;
import com.fps.svmes.repositories.jpaRepo.subscription.WeeklyReportSubscriptionRepository;
import com.fps.svmes.repositories.jpaRepo.user.UserRepository;
import com.fps.svmes.services.WeeklyReportSubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeeklyReportSubscriptionServiceImpl implements WeeklyReportSubscriptionService {

    private final WeeklyReportSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional(readOnly = true)
    public List<WeeklyReportSubscriptionDTO> getAllSubscriptions() {
        return subscriptionRepository.findAll()
                .stream()
                .filter(sub -> sub.getStatus().equals(1))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WeeklyReportSubscriptionDTO> getActiveSubscriptions() {
        return subscriptionRepository.findAllActiveSubscriptions()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public WeeklyReportSubscriptionDTO addSubscription(WeeklyReportSubscriptionDTO dto) {
        // Check if email already subscribed
        if (subscriptionRepository.existsByEmailAndStatus(dto.getEmail(), 1)) {
            throw new RuntimeException("Email already subscribed: " + dto.getEmail());
        }

        WeeklyReportSubscription subscription = new WeeklyReportSubscription();
        subscription.setUserId(dto.getUserId());
        subscription.setEmail(dto.getEmail());
        subscription.setIsActive(true);
        subscription.setCreationDetails(dto.getCreatedBy(), 1);

        WeeklyReportSubscription saved = subscriptionRepository.save(subscription);
        log.info("Added subscription for email: {}", dto.getEmail());
        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public void removeSubscription(Integer id) {
        Optional<WeeklyReportSubscription> optional = subscriptionRepository.findById(id);
        if (optional.isPresent()) {
            WeeklyReportSubscription sub = optional.get();
            sub.setStatus(0); // Soft delete
            subscriptionRepository.save(sub);
            log.info("Removed subscription id: {}", id);
        } else {
            throw new RuntimeException("Subscription not found: " + id);
        }
    }

    @Override
    @Transactional
    public void toggleSubscriptionStatus(Integer id, Boolean isActive) {
        Optional<WeeklyReportSubscription> optional = subscriptionRepository.findById(id);
        if (optional.isPresent()) {
            WeeklyReportSubscription sub = optional.get();
            sub.setIsActive(isActive);
            subscriptionRepository.save(sub);
            log.info("Toggled subscription id: {} to isActive: {}", id, isActive);
        } else {
            throw new RuntimeException("Subscription not found: " + id);
        }
    }

    @Override
    @Transactional
    public WeeklyReportSubscriptionDTO updateSubscription(Integer id, WeeklyReportSubscriptionDTO dto) {
        Optional<WeeklyReportSubscription> optional = subscriptionRepository.findById(id);
        if (optional.isEmpty()) {
            throw new RuntimeException("Subscription not found: " + id);
        }

        WeeklyReportSubscription sub = optional.get();

        // If email is changing, check uniqueness
        if (dto.getEmail() != null && !dto.getEmail().equals(sub.getEmail())) {
            if (subscriptionRepository.existsByEmailAndStatus(dto.getEmail(), 1)) {
                throw new RuntimeException("Email already subscribed: " + dto.getEmail());
            }
            sub.setEmail(dto.getEmail());
        }

        if (dto.getLanguage() != null) {
            sub.setLanguage(dto.getLanguage());
        }
        
        if (dto.getUserId() != null) {
            sub.setUserId(dto.getUserId());
        }

        sub.setUpdateDetails(dto.getUpdatedBy(), 1);
        WeeklyReportSubscription saved = subscriptionRepository.save(sub);
        log.info("Updated subscription id: {}", id);
        return mapToDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailSubscribed(String email) {
        return subscriptionRepository.existsByEmailAndStatus(email, 1);
    }

    private WeeklyReportSubscriptionDTO mapToDTO(WeeklyReportSubscription subscription) {
        WeeklyReportSubscriptionDTO dto = modelMapper.map(subscription, WeeklyReportSubscriptionDTO.class);
        // Enrich with user name if userId is present
        if (subscription.getUserId() != null) {
            String userName = userRepository.findNameById(subscription.getUserId());
            dto.setUserName(userName);
        }
        return dto;
    }
}
