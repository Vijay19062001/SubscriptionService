package com.sms.SubscriptionService.service.servicesImpl;

import com.sms.SubscriptionService.enums.Status;
import com.sms.SubscriptionService.exception.custom.*;
import com.sms.SubscriptionService.mapper.SubscriptionMapper;
import com.sms.SubscriptionService.model.SubscriptionModel;
import com.sms.SubscriptionService.entity.Subscription;
import com.sms.SubscriptionService.repository.ServiceRepository;
import com.sms.SubscriptionService.repository.SubscriptionRepository;
import com.sms.SubscriptionService.repository.UserRepository;
import com.sms.SubscriptionService.service.SubscriptionService;
import com.sms.SubscriptionService.utils.DateUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionServiceImpl.class);

    @Autowired
    private final SubscriptionRepository subscriptionRepository;
    @Autowired
    private final SubscriptionMapper subscriptionMapper;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Transactional
    @Override
    public SubscriptionModel createSubscription(SubscriptionModel subscriptionModel) {
        logger.info("Starting subscription creation for user ID {}", subscriptionModel.getUserId());

        Integer serviceId = Integer.valueOf(subscriptionModel.getServiceId());


        if (subscriptionModel.getDbstatus() == Status.ACTIVE) {
            if (subscriptionRepository.existsByUserIdAndServiceIdAndDbstatus(
                    Integer.valueOf(subscriptionModel.getUserId()),
                    serviceId,
                    Status.ACTIVE)) {
                throw new DuplicateSubscriptionException("User already has an active subscription for this service.");
            }
        } else if (subscriptionModel.getDbstatus() == Status.INACTIVE) {
            throw new BusinessValidationException("Cannot create a subscription with an inactive status.");
        }

        if (!isValidDbStatus(subscriptionModel.getDbstatus())) {
            throw new BusinessValidationException("Invalid subscription status. Must be 'ACTIVE'.");
        }

        validateSubscriptionDates(subscriptionModel.getStartDate());

        Subscription subscription = subscriptionMapper.toEntity(subscriptionModel);
        LocalDateTime now = LocalDateTime.now();
        subscription.setEndDate(now);
        subscription.setCreatedDate(now);
        subscription.setUpdatedDate(now);
        subscription.setCreatedBy(subscriptionModel.getCreatedBy() != null && !subscriptionModel.getCreatedBy().isEmpty()
                ? subscriptionModel.getCreatedBy()
                : "system");
        subscription.setUpdatedBy("system");

        Subscription savedSubscription = subscriptionRepository.save(subscription);

        logger.info("Subscription created successfully for user ID {}", subscriptionModel.getUserId());

        return subscriptionMapper.toModel(savedSubscription);
    }

    private boolean isValidDbStatus(Status dbstatus) {
        return dbstatus == Status.ACTIVE;
    }

    @Override
    public boolean checkActiveSubscription(Integer userId, String serviceId) {
        return subscriptionRepository.existsByUserIdAndServiceIdAndDbstatus(userId, Integer.valueOf(serviceId), Status.ACTIVE);
    }

   @Override
    public List<SubscriptionModel> getSubscriptionUserId(Integer userId, Status status) {
        if (status == Status.INACTIVE) {
            throw new BusinessValidationException(" subscriptions as the status is inactive.");
        }
        List<Subscription> subscriptions = subscriptionRepository.findByUserIdAndDbstatus(userId, status);
        return subscriptions.stream()
                .map(subscriptionMapper::toModel)
                .collect (Collectors.toList());
    }

    private void validateSubscriptionDates(String startDate) {
        if (startDate == null) {
            throw new InvalidDateFormatException("Start date must not be null.");
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        try {
            LocalDate start = LocalDate.parse(startDate, formatter);
            LocalDate now = LocalDate.now();

            if (start.isBefore(now)) {
                throw new InvalidDateFormatException("Start date must not be in the past.");
            }

        } catch (DateTimeParseException e) {
            throw new InvalidDateFormatException("Invalid date format provided. Please use 'yyyyMMdd'.");
        }
    }

    @Override
    public SubscriptionModel updateSubscription(Integer subscriptionId, SubscriptionModel subscriptionModel) {
        logger.info("Updating subscription with ID {}", subscriptionId);

        Subscription existingSubscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription with ID '" + subscriptionId + "' not found."));

        updateSubscriptionDetails(existingSubscription, subscriptionModel);

        Subscription updatedSubscription = subscriptionRepository.save(existingSubscription);
        logger.info("Subscription update for ID {} completed", subscriptionId);
        return subscriptionMapper.toModel(updatedSubscription);
    }

    private void updateSubscriptionDetails(Subscription existingSubscription, SubscriptionModel subscriptionModel) {
        if (subscriptionModel.getServiceId() != null) {
            existingSubscription.setServiceId(Integer.parseInt(subscriptionModel.getServiceId()));
        }
        if (subscriptionModel.getUserId() != null) {
            existingSubscription.setUserId(Integer.parseInt(subscriptionModel.getUserId()));
        }

        if (subscriptionModel.getDbstatus() != null) {
            existingSubscription.setDbstatus(Status.fromString(String.valueOf(subscriptionModel.getDbstatus())));
        }
        if (subscriptionModel.getStartDate() != null) {
            try {
                existingSubscription.setStartDate(LocalDateTime.from(DateUtils.convertToLocalDateTime(subscriptionModel.getStartDate())));
            } catch (DateTimeParseException e) {
                logger.error("Invalid start date format for subscription ID {}", existingSubscription.getId());
                throw new InvalidDateFormatException("Invalid start date format. Please use the correct format.");
            }
        }
        if (subscriptionModel.getEndDate() != null) {
            try {
                existingSubscription.setEndDate(LocalDateTime.from(DateUtils.convertToLocalDateTime(subscriptionModel.getEndDate())));
            } catch (DateTimeParseException e) {
                logger.error("Invalid end date format for subscription ID {}", existingSubscription.getId());
                throw new InvalidDateFormatException("Invalid end date format. Please use the correct format.");
            }
        }
        existingSubscription.setUpdatedBy("Admin");
        existingSubscription.setUpdatedDate(LocalDateTime.now());
    }


    @Override
    public void cancelSubscription(Integer subscriptionId, Integer userId) {
        logger.info("Attempting to cancel subscription with ID {} for user ID {}", subscriptionId, userId);

        // Validate if the user exists
        boolean userExists = userRepository.existsById(userId);
        if (!userExists) {
            logger.error("User with ID {} not found. Cancellation failed.", userId);
            throw new SubscriptionNotFoundException("User with ID " + userId + " not found.");
        }

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription with ID '" + subscriptionId + "' not found."));

        subscription.setDbstatus(Status.INACTIVE);
        subscription.setUpdatedBy("Admin");
        subscription.setUpdatedDate(LocalDateTime.now());
        subscriptionRepository.save(subscription);

        logger.info("Subscription with ID {} successfully cancelled for user ID {}", subscriptionId, userId);
    }



    @Override
    public List<Subscription> getSubscriptionDetails(Integer subscriptionId) {
        logger.info("Fetching subscription details for user ID {}", subscriptionId);
        if (subscriptionId == null || subscriptionId <= 0) {
            logger.error("Invalid user ID format for fetching subscription details");
            throw new SubscriptionNotFoundException("Invalid userId format.");
        }

        boolean userExists = userRepository.existsById(subscriptionId);
        if (!userExists) {
            logger.error("User with ID {} not found", subscriptionId);
            throw new SubscriptionNotFoundException("User with userId " + subscriptionId + " not found.");
        }

        List<Subscription> subscriptions = subscriptionRepository.findByUserId(subscriptionId);

        if (subscriptions.isEmpty()) {
            logger.warn("No subscriptions found for user ID {}", subscriptionId);
            throw new SubscriptionNotFoundException("No subscriptions found for userId: " + subscriptionId);
        }
        logger.info("Subscription details fetched successfully for user ID {}", subscriptionId);
        return subscriptions;
    }

    @Override
    public List<Subscription> getAllSubscriptions() {
        logger.info("Fetching all subscriptions");
        return subscriptionRepository.findAll();
    }
}
