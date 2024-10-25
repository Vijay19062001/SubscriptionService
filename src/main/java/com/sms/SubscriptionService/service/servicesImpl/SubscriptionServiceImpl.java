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
import com.sms.SubscriptionService.utils.DateUtils;
import jakarta.mail.MessagingException;
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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;



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
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${scheduler.enabled}")
    private boolean isSchedulerEnabled;
    @Autowired
    private ServiceRepository serviceRepository;

    @Transactional
    @Override
    public SubscriptionModel createSubscription(SubscriptionModel subscriptionModel) {
        logger.info("Starting subscription creation for user ID {}", subscriptionModel.getUserId());

        Integer serviceId = Integer.valueOf(subscriptionModel.getServiceId());
        ServiceEntity service =  serviceRepository.findByIdAndDbstatus(serviceId, Status.ACTIVE)
                .orElseThrow(() -> new BusinessValidationException("Service not found or inactive with ID: " + serviceId));

        validateSubscriptionDates(subscriptionModel.getStartDate());

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
        logger.info("Attempting to cancel subscription with ID {}.", subscriptionId);

        boolean userExists = userRepository.existsById(userId);
        if (!userExists) {
            logger.error("User with ID {} not found. Cancellation failed.", userId);
            throw new SubscriptionNotFoundException("User with ID " + userId + " not found.");
        }

        Subscription subscription = subscriptionRepository
                .findByUserIdAndIdAndDbstatus(userId,subscriptionId, Status.ACTIVE)
                .orElse(null);

        if (subscription == null) {
            logger.error("No active subscription found for subscription ID {}.", subscriptionId);
            throw new SubscriptionNotFoundException("Active subscription with ID '" + subscriptionId +"' not found.");
        }

        subscription.setDbstatus(Status.INACTIVE);
        subscription.setUpdatedBy("Admin");
        subscription.setUpdatedDate(LocalDateTime.now());

        subscriptionRepository.save(subscription);

        logger.info("Subscription with ID {} successfully cancelled.", subscriptionId);
    }


    @Override
    public List<Subscription> getSubscriptionId(Integer subscriptionId) {
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

        List<Subscription> subscriptions = subscriptionRepository.findByServiceIdAndDbstatus(subscriptionId, Status.ACTIVE);

        if (subscriptions.isEmpty()) {
            logger.warn("No subscriptions found for user ID {}", subscriptionId);
            throw new SubscriptionNotFoundException("No subscriptions found for userId: " + subscriptionId);
        }
        logger.info("Subscription details fetched successfully for user ID {}", subscriptionId);
        return subscriptions;
    }


    @Override
    public List<Subscription> getSubscriptionDetails(Integer userId) {
        return subscriptionRepository.findByUserIdAndDbstatus(userId, Status.ACTIVE);
    }

    private void sendSubscriptionConfirmationEmail(Subscription subscription, ServiceEntity service) {
        // Retrieve the user details
        Users user = userRepository.findById(subscription.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + subscription.getUserId()));

        // Prepare email content using Thymeleaf template engine (or any other engine you are using)
        Context context = new Context();
        context.setVariable("userName", user.getUserName());
        context.setVariable("userEmail", user.getEmail());
        context.setVariable("serviceName", service.getServiceName());
        context.setVariable("subscriptionStartDate", subscription.getStartDate());
        context.setVariable("subscriptionEndDate", subscription.getEndDate());

        String emailContent = templateEngine.process("SubscriptionConfirmationTemplate", context);

        MimeMessage mailMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mailMessage);

        try {
            helper.setTo(user.getEmail());
            helper.setSubject("Subscription Confirmation for " + service.getServiceName());
            helper.setText(emailContent, true);
            mailSender.send(mailMessage);

            logger.info("Subscription confirmation email sent to {}", user.getEmail());
        } catch (MessagingException e) {
            logger.error("Failed to send email to {}", user.getEmail(), e);
            throw new RuntimeException("Failed to send subscription confirmation email", e);
        }
    }


    @Scheduled(cron = "${scheduler.cron}")
    public void scheduleDailySubscriptionReminder() {
        logger.info("Scheduling daily subscription reminder emails.");

        if (!isSchedulerEnabled) {
            logger.info("Scheduler is disabled. Skipping daily subscription reminder emails.");
            return;
        }

        logger.info("Starting scheduled task to send daily subscription reminder emails.");

        try {
            List<Users> allUsers = userRepository.findAll();
            logger.info("Found {} users to send subscription reminders.", allUsers.size());

            for (Users user : allUsers) {
                try {
                    logger.info("Preparing email for user: {}", user.getId());

                    List<Subscription> subscriptions = subscriptionRepository
                            .findByUserIdAndEndDateBetween(user.getId(), LocalDate.now().minusDays(3), LocalDate.now());


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

        } catch (Exception e) {
            logger.error("Unexpected error in scheduleDailySubscriptionReminder", e);
        }
    }

    private void sendSubscriptionReminderEmail(Users user, List<Subscription> subscriptions) {
        // Prepare email content using Thymeleaf or any other engine
        Context context = new Context();
        context.setVariable("userName", user.getUserName());
        context.setVariable("userEmail", user.getEmail());
        context.setVariable("subscriptions", subscriptions); // Include list of subscriptions

        String emailContent = templateEngine.process("SubscriptionReminderTemplate", context);

        MimeMessage mailMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mailMessage);

        try {
            helper.setTo(user.getEmail());
            helper.setSubject("Subscription Reminder: Services Nearing Expiration");
            helper.setText(emailContent, true); // true for HTML content
            mailSender.send(mailMessage);

            logger.info("Subscription reminder email sent to {}", user.getEmail());
        } catch (MessagingException e) {
            logger.error("Failed to send subscription reminder to {}", user.getEmail(), e);
            throw new RuntimeException("Failed to send subscription reminder email", e);
        }
    }



}
