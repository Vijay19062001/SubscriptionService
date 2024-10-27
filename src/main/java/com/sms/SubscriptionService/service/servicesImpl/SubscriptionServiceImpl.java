package com.sms.SubscriptionService.service.servicesImpl;

import com.sms.SubscriptionService.entity.ServiceEntity;
import com.sms.SubscriptionService.entity.Users;
import com.sms.SubscriptionService.enums.Status;
import com.sms.SubscriptionService.exception.custom.*;
import com.sms.SubscriptionService.mapper.SubscriptionMapper;
import com.sms.SubscriptionService.model.SubscriptionModel;
import com.sms.SubscriptionService.entity.Subscription;
import com.sms.SubscriptionService.repository.ServiceRepository;
import com.sms.SubscriptionService.repository.SubscriptionRepository;
import com.sms.SubscriptionService.repository.UserRepository;
import com.sms.SubscriptionService.service.SubscriptionService;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    @PersistenceContext
    private final EntityManager entityManager;
    @Autowired
    private final JavaMailSender mailSender;
    @Autowired
    private final SpringTemplateEngine templateEngine;

    @Autowired
    private ServiceRepository serviceRepository;

    @Transactional
    @Override
    public SubscriptionModel createSubscription(SubscriptionModel subscriptionModel, Users users) {
        logger.info("Starting subscription creation for user ID {}", subscriptionModel.getUserId());

        Integer serviceId = Integer.valueOf(subscriptionModel.getServiceId());
        ServiceEntity service = serviceRepository.findByIdAndDbstatus(serviceId, Status.ACTIVE)
                .orElseThrow(() -> new BusinessValidationException("Service not found or inactive with ID: " + serviceId));

        validateSubscriptionDates(subscriptionModel.getStartDate());

        if (subscriptionRepository.existsByUserIdAndServiceIdAndDbstatus(
                Integer.valueOf(subscriptionModel.getUserId()),
                serviceId,
                Status.ACTIVE)) {
            throw new DuplicateSubscriptionException("User already has an active subscription for this service.");
        }

        Subscription subscription = subscriptionMapper.toEntity(subscriptionModel, users);

        Subscription savedSubscription = subscriptionRepository.save(subscription);
        List<Subscription> activeSubscriptions = subscriptionRepository.findByUserIdAndDbstatus(users.getId(), Status.ACTIVE);
        List<Subscription> deactivatedSubscriptions = subscriptionRepository.findByUserIdAndDbstatus(users.getId(), Status.INACTIVE);

        sendSubscriptionConfirmationEmail(users, activeSubscriptions, deactivatedSubscriptions);

        logger.info("Subscription created successfully for user ID {}", subscriptionModel.getUserId());

        return subscriptionMapper.toModel(savedSubscription);
    }


    @Override
    public boolean checkActiveSubscription(Integer userId, String serviceId) {
        return subscriptionRepository.existsByUserIdAndServiceIdAndDbstatus(userId, Integer.valueOf(serviceId), Status.ACTIVE);
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
    public void cancelSubscription(Integer subscriptionId, Integer userId) {
        logger.info("Attempting to cancel subscription with ID {}.", subscriptionId);

        boolean userExists = userRepository.existsById(userId);
        if (!userExists) {
            logger.error("User with ID {} not found. Cancellation failed.", userId);
            throw new SubscriptionNotFoundException("User with ID " + userId + " not found.");
        }

        Subscription subscription = subscriptionRepository
                .findByUserIdAndIdAndDbstatus(userId, subscriptionId, Status.ACTIVE)
                .orElse(null);

        if (subscription == null) {
            logger.error("No active subscription found for subscription ID {}.", subscriptionId);
            throw new SubscriptionNotFoundException("Active subscription with ID '" + subscriptionId + "' not found.");
        }

        subscription.setDbstatus(Status.INACTIVE);
        subscription.setUpdatedBy("Admin");
        subscription.setUpdatedDate(LocalDateTime.now());

        subscriptionRepository.save(subscription);

        logger.info("Subscription with ID {} successfully cancelled.", subscriptionId);
    }


    @Override
    public List<SubscriptionModel> getListSubscription(Integer userId) {
        logger.info("Fetching subscription list for user ID {}", userId);

        boolean userExists = userRepository.existsById(userId);
        if (!userExists) {
            logger.error("User with ID {} not found.", userId);
            throw new SubscriptionNotFoundException("User with ID " + userId + " not found.");
        }

        List<Subscription> subscriptions = subscriptionRepository.findAllByUserId(userId);

        if (subscriptions.isEmpty()) {
            logger.info("No subscriptions found for user ID {}", userId);
            throw new SubscriptionNotFoundException("No subscriptions found for user with ID " + userId);
        }

        List<SubscriptionModel> subscriptionModels = subscriptions.stream()
                .map(subscriptionMapper::toModel)
                .collect(Collectors.toList());

        logger.info("Successfully fetched subscription list for user ID {}", userId);

        return subscriptionModels;
    }

    @Override
    public List<Subscription> getAllSubscriptions() {
        return subscriptionRepository.findAll();
    }

    @Value("${scheduler.enabled}")
    private boolean isSchedulerEnabled;

    @Scheduled(cron = "0 11 23 * * ?")
    public void scheduleDailySubscriptionReminder() {
        logger.info("Scheduling daily subscription reminder emails.");

        if (!isSchedulerEnabled) {
            logger.info("Scheduler is disabled. Skipping daily subscription reminder emails.");
            return;
        }

        logger.info("Starting scheduled task to send daily subscription reminder emails.");

        try {
            // Fetch all users to send reminders
            List<Users> allUsers = userRepository.findAll();
            logger.info("Found {} users to send subscription reminders.", allUsers.size());

            for (Users user : allUsers) {
                sendEmailReminderForUser(user);
            }

        } catch (Exception e) {
            logger.error("Unexpected error in scheduleDailySubscriptionReminder", e);
        }
    }

    private void sendEmailReminderForUser(Users user) {
        try {
            logger.info("Preparing email for user: {}", user.getId());

            // Fetch subscriptions that are nearing expiration
            List<Subscription> subscriptions = subscriptionRepository
                    .findByUserIdAndEndDateBetween(
                            user.getId(),
                            LocalDate.now().plusDays(1).atStartOfDay(),
                            LocalDate.now().plusDays(3).atTime(LocalTime.MAX)
                    );

            // If subscriptions are found, send email
            if (!subscriptions.isEmpty()) {
                sendSubscriptionReminderEmail(user, subscriptions);
                logger.info("Successfully sent email to user: {}", user.getId());
            } else {
                logger.info("No subscriptions found nearing expiration for user: {}", user.getId());
            }

        } catch (Exception e) {
            logger.error("Failed to send subscription reminder to user: {}", user.getId(), e);
        }
    }

    private void sendSubscriptionReminderEmail(Users user, List<Subscription> subscriptions) {
        try {
            Context context = new Context();
            context.setVariable("Name", user.getName());

            // Fetch details for each subscription
            List<Map<String, String>> subscriptionDetails = subscriptions.stream()
                    .map(subscription -> {
                        ServiceEntity serviceEntity = serviceRepository.findById(subscription.getServiceId())
                                .orElseThrow(() -> new RuntimeException("Service not found for ID: " + subscription.getServiceId()));

                        Map<String, String> details = new HashMap<>();
                        details.put("serviceName", serviceEntity.getServiceName());
                        details.put("subscriptionId", String.valueOf(subscription.getId()));
                        details.put("startDate", subscription.getStartDate().toString());
                        details.put("expiryDate", subscription.getEndDate().toString());
                        details.put("status", subscription.getDbstatus().toString()); // Include status
                        return details;
                    }).collect(Collectors.toList());

            context.setVariable("allSubscriptions", subscriptionDetails);

            String emailContent = templateEngine.process("EmailTemplate", context);

            MimeMessage mailMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mailMessage);
            helper.setTo(user.getEmail());
            helper.setSubject("Subscription Reminder: Services Nearing Expiration");
            helper.setText(emailContent, true);

            mailSender.send(mailMessage);
            logger.info("Subscription reminder email sent to {}", user.getEmail());

            saveNotificationDetails(user, subscriptions);

        } catch (Exception e) {
            logger.error("Failed to send subscription reminder to {}", user.getEmail(), e);
            throw new RuntimeException("Failed to send subscription reminder email", e);
        }
    }

    private void saveNotificationDetails(Users user, List<Subscription> subscriptions) {
        subscriptions.forEach(subscription -> {
            ServiceEntity serviceEntity = serviceRepository.findById(subscription.getServiceId())
                    .orElseThrow(() -> new RuntimeException("Service not found for ID: " + subscription.getServiceId()));

            logger.info("Notification sent for Subscription ID: {}, User ID: {}, Service: {}, Expiry Date: {}",
                    subscription.getId(), user.getId(), serviceEntity.getServiceName(), subscription.getEndDate());
        });
    }


    private void sendSubscriptionConfirmationEmail(Users users, List<Subscription> activeSubscriptions, List<Subscription> deactivatedSubscriptions) {
        try {
            Context context = new Context();
            context.setVariable("Name", users.getName());

            context.setVariable("activeSubscriptions", activeSubscriptions);
            context.setVariable("deactivatedSubscriptions", deactivatedSubscriptions);

            String emailContent = templateEngine.process("SubscriptionConfirmationEmail", context);

            MimeMessage mailMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mailMessage);

            helper.setTo(users.getEmail());
            helper.setSubject("Subscription Confirmation: Your Service is Now Active");
            helper.setText(emailContent, true);

            mailSender.send(mailMessage);

            logger.info("Subscription confirmation email sent to {}", users.getEmail());

            saveConfirmationNotificationDetails(users, activeSubscriptions, deactivatedSubscriptions);

        } catch (Exception e) {
            logger.error("Failed to send subscription confirmation to {}", users.getEmail(), e);
            throw new RuntimeException("Failed to send subscription confirmation email", e);
        }
    }

    private void saveConfirmationNotificationDetails(Users users, List<Subscription> activeSubscriptions, List<Subscription> deactivatedSubscriptions) {
        for (Subscription subscription : activeSubscriptions) {
            logger.info("Confirmation sent for Active Subscription ID: {}, User ID: {}, Service: {}, Expiry Date: {}",
                    subscription.getId(), users.getId(), subscription.getServiceId(), subscription.getEndDate());
        }

        for (Subscription subscription : deactivatedSubscriptions) {
            logger.info("Confirmation sent for Deactivated Subscription ID: {}, User ID: {}, Service: {}, Expiry Date: {}",
                    subscription.getId(), users.getId(), subscription.getServiceId(), subscription.getEndDate());
        }
    }
}