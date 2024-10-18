package com.sms.SubscriptionService.service.servicesImpl;

import com.sms.SubscriptionService.enums.Status;
import com.sms.SubscriptionService.repository.UserRepository;
import com.sms.SubscriptionService.utils.DateUtils;
import com.sms.SubscriptionService.entity.Subscription;
import com.sms.SubscriptionService.enums.SubscriptionStatus;
import com.sms.SubscriptionService.exception.custom.DuplicateSubscriptionException;
import com.sms.SubscriptionService.exception.custom.InvalidDateFormatException;
import com.sms.SubscriptionService.exception.custom.InvalidRequestException;
import com.sms.SubscriptionService.exception.custom.SubscriptionNotFoundException;
import com.sms.SubscriptionService.mapper.SubscriptionMapper;
import com.sms.SubscriptionService.model.SubscriptionModel;
import com.sms.SubscriptionService.repository.SubscriptionRepository;
import com.sms.SubscriptionService.service.SubscriptionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionMapper subscriptionMapper;
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionServiceImpl.class);

    @Autowired
    private UserRepository userRepository; // Assuming there is a UserRepository to fetch user details


    @Transactional
    public SubscriptionModel createSubscription(SubscriptionModel subscriptionModel) {
        // Business validation: Check for duplicate active subscriptions
        Subscription subscription = subscriptionMapper.toEntity(subscriptionModel);

        // Use the enum directly instead of a string
        List<Subscription> activeSubscriptions = subscriptionRepository
                .findByUserIdAndStatus(subscription.getUserId(), SubscriptionStatus.ACTIVE);


        if (!activeSubscriptions.isEmpty()) {
            throw new DuplicateSubscriptionException("Duplicate active subscription found.");
        }

        LocalDateTime now = LocalDateTime.now();
        subscription.setCreatedDate(now);
        subscription.setUpdatedDate(now);

        // Assuming `createdBy` is obtained from the subscriptionModel or a logged-in user context
        String createdBy = subscriptionModel.getCreatedBy();
        subscription.setCreatedBy((createdBy != null && !createdBy.isEmpty()) ? createdBy : "system"); // Default to "system"
        subscription.setUpdatedBy("system");

        Subscription savedSubscription = subscriptionRepository.save(subscription);
        return subscriptionMapper.toModel(savedSubscription);
    }

    @Transactional
    public void renewSubscription(Integer subscriptionId, SubscriptionModel subscriptionModel) {
        // Validate subscription ID and check if it exists
        Subscription subscription = subscriptionRepository.findById(Integer.parseInt(String.valueOf(subscriptionId)))
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found."));

        // Check business validation (grace period eligibility, valid payment, etc.)
        if (!isValidRenewalRequest(subscription, subscriptionModel)) {
            throw new InvalidRequestException("Invalid renewal request.");
        }

        // Update subscription details
        LocalDateTime newEndDate = calculateNewEndDate(subscription.getEndDate());
        subscription.setEndDate(newEndDate);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setUpdatedDate(LocalDateTime.now());
        // Save the updated subscription
        Subscription updatedSubscription = subscriptionRepository.save(subscription);
        // Send confirmation email (placeholder for the actual implementation)
        subscriptionMapper.toModel(updatedSubscription);
    }

    private boolean isValidRenewalRequest(Subscription subscription, SubscriptionModel subscriptionModel) {
        return true;
    }

    private LocalDateTime calculateNewEndDate(LocalDateTime currentEndDate) {
        // Calculate the new end date based on the renewal period (e.g., add one month)
        return currentEndDate.plusMonths(1);
    }


    @Override
    public SubscriptionModel updateSubscription(Integer subscriptionId, SubscriptionModel subscriptionModel) {
        logger.info("Subscription update for ID {} started", subscriptionId);
        Subscription existingSubscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription with ID '" + subscriptionId + "' not found."));

        if (subscriptionModel.getServiceId() != null) {
            existingSubscription.setServiceId(Integer.parseInt(subscriptionModel.getServiceId()));
        }
        if (subscriptionModel.getUserId() != null) {
            existingSubscription.setUserId(Integer.parseInt(subscriptionModel.getUserId()));
        }
        if (subscriptionModel.getStatus() != null) {
            existingSubscription.setStatus(SubscriptionStatus.fromString(String.valueOf(subscriptionModel.getStatus())));
        }
        if (subscriptionModel.getDbstatus() != null) {
            existingSubscription.setDbstatus(Status.fromString(String.valueOf(subscriptionModel.getDbstatus())));
        }
        if (subscriptionModel.getStartDate() != null) {
            try {
                existingSubscription.setStartDate(LocalDateTime.from(DateUtils.convertToLocalDateTime(subscriptionModel.getStartDate())));
            } catch (DateTimeParseException e) {
                throw new InvalidDateFormatException("Invalid start date format. Please use the correct format.");
            }
        }
        if (subscriptionModel.getEndDate() != null) {
            try {
                existingSubscription.setEndDate(LocalDateTime.from(DateUtils.convertToLocalDateTime(subscriptionModel.getEndDate())));
            } catch (DateTimeParseException e) {
                throw new InvalidDateFormatException("Invalid end date format. Please use the correct format.");
            }
        }
        existingSubscription.setUpdatedBy("Admin");
        existingSubscription.setUpdatedDate(LocalDateTime.now());
        Subscription updatedSubscription = subscriptionRepository.save(existingSubscription);
        logger.info("Subscription update for ID {} completed", subscriptionId);
        return subscriptionMapper.toModel(updatedSubscription);
    }

    @Override
    public void deleteSubscription(Integer subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found."));

        subscription.setDbstatus(Status.INACTIVE);
        subscriptionRepository.save(subscription);
    }

    @Override
    public void cancelSubscription(Integer subscriptionId) {
        logger.info("Cancelling subscription with ID {}", subscriptionId);
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription with ID '" + subscriptionId + "' not found."));

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setUpdatedBy("Admin");
        subscription.setUpdatedDate(LocalDateTime.now());
        subscriptionRepository.save(subscription);
        logger.info("Subscription with ID {} successfully cancelled", subscriptionId);
    }

    @Override
    public List<Subscription> getSubscriptionDetails(Integer userId) {
        if (userId == null || userId <= 0) {
            throw new RuntimeException("Invalid userId format.");
        }

        boolean userExists = userRepository.existsById(userId);
        if (!userExists) {
            throw new RuntimeException("User with userId " + userId + " not found.");
        }

        List<Subscription> subscriptions = subscriptionRepository.findByUserId(userId);

        if (subscriptions.isEmpty()) {
            throw new RuntimeException("No subscriptions found for userId: " + userId);
        }
        return subscriptions;
    }

    @Override
    public List<Subscription> getAllSubscriptions() {
        return subscriptionRepository.findAll();
    }

}
