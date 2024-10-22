package com.sms.SubscriptionService.service.servicesImpl;

import com.sms.SubscriptionService.enums.Status;
import com.sms.SubscriptionService.exception.custom.DuplicateSubscriptionException;
import com.sms.SubscriptionService.exception.custom.InvalidDateFormatException;
import com.sms.SubscriptionService.exception.custom.InvalidSubscriptionException;
import com.sms.SubscriptionService.exception.custom.SubscriptionNotFoundException;
import com.sms.SubscriptionService.mapper.SubscriptionMapper;
import com.sms.SubscriptionService.model.SubscriptionModel;
import com.sms.SubscriptionService.entity.Subscription;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;

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

    @Transactional
    @Override
    public SubscriptionModel createSubscription(SubscriptionModel subscriptionModel) {
        logger.info("Starting subscription creation for user ID {}", subscriptionModel.getUserId());

        if (subscriptionModel.getUserId() == null || subscriptionModel.getServiceId() == null) {
            throw new InvalidSubscriptionException("User ID and Service ID must not be null.");
        }


        if (!isValidDbStatus(String.valueOf(subscriptionModel.getDbstatus()))) {
            throw new InvalidSubscriptionException("Invalid subscription status. Must be 'ACTIVE' or 'INACTIVE'.");
        }

        Subscription subscription = subscriptionMapper.toEntity(subscriptionModel);

        if ("ACTIVE".equalsIgnoreCase(String.valueOf(subscriptionModel.getDbstatus()))) {
            boolean hasActiveSubscription = checkActiveSubscription(subscriptionModel.getUserId(), subscriptionModel.getServiceId());
            if (hasActiveSubscription) {
                throw new DuplicateSubscriptionException("User already has an active subscription for this service.");
            }
        }

        validateSubscriptionDates(subscription.getStartDate(), subscription.getEndDate());

        LocalDateTime now = LocalDateTime.now();
        subscription.setCreatedDate(now);
        subscription.setUpdatedDate(now);
        String createdBy = subscriptionModel.getCreatedBy();
        subscription.setCreatedBy((createdBy != null && !createdBy.isEmpty()) ? createdBy : "system");
        subscription.setUpdatedBy("system");

        Subscription savedSubscription = subscriptionRepository.save(subscription);
        logger.info("Subscription created successfully for user ID {}", subscriptionModel.getUserId());

        return subscriptionMapper.toModel(savedSubscription);
    }

    private boolean isValidDbStatus(String dbstatus) {
        return "ACTIVE".equalsIgnoreCase(dbstatus) || "INACTIVE".equalsIgnoreCase(dbstatus);
    }


    @Override
    public boolean checkActiveSubscription(String userId, String serviceId) {
        List<Subscription> activeSubscriptions = subscriptionRepository
                .findActiveSubscriptionsByUserId(Integer.valueOf(userId));

        return activeSubscriptions.stream()
                .anyMatch(subscription -> Objects.equals(subscription.getServiceId(), serviceId));
    }

    private void validateSubscriptionDates(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate != null && endDate != null) {
            long daysBetween = java.time.Duration.between(startDate, endDate).toDays();
            if (daysBetween <= 0) {
                logger.error("End date must be after start date.");
                throw new InvalidDateFormatException("End date must be after start date.");
            }
            if (daysBetween >= 30) {
                logger.error("The duration between start date and end date exceeds 30 days");
                throw new InvalidDateFormatException("The duration between start date and end date cannot exceed 30 days.");
            }
        } else {
            throw new InvalidDateFormatException("Start date and end date must not be null.");
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
    public void cancelSubscription(Integer subscriptionId) {
        logger.info("Cancelling subscription with ID {}", subscriptionId);
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription with ID '" + subscriptionId + "' not found."));

        subscription.setDbstatus(Status.INACTIVE);
        subscription.setUpdatedBy("Admin");
        subscription.setUpdatedDate(LocalDateTime.now());
        subscriptionRepository.save(subscription);
        logger.info("Subscription with ID {} successfully cancelled", subscriptionId);
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
