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
    public SubscriptionModel createSubscription(SubscriptionModel subscriptionModel, Users users) {
        logger.info("Starting subscription creation for user ID {}", subscriptionModel.getUserId());

        Integer serviceId = Integer.valueOf(subscriptionModel.getServiceId());
        ServiceEntity service = serviceRepository.findByIdAndDbstatus(serviceId, Status.ACTIVE)
                .orElseThrow(() -> new BusinessValidationException("Service not found or inactive with ID: " + serviceId));

        validateSubscriptionDates(subscriptionModel.getStartDate());

//        if (service.getDbstatus()) {
            if (subscriptionRepository.existsByUserIdAndServiceIdAndDbstatus(
                    Integer.valueOf(subscriptionModel.getUserId()),
                    serviceId,
                    Status.ACTIVE)) {
                throw new DuplicateSubscriptionException("User already has an active subscription for this service.");
            }
//        } else if (subscriptionModel.getDbstatus() == Status.INACTIVE) {
//            throw new BusinessValidationException("Cannot create a subscription with an inactive status.");
//        }

//        if (!isValidDbStatus(subscriptionModel.getDbstatus())) {
//            throw new BusinessValidationException("Invalid subscription status. Must be 'ACTIVE'.");
//        }

        Subscription subscription = subscriptionMapper.toEntity(subscriptionModel,users);


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
    public List<Subscription> getAllSubscriptions() {
        return subscriptionRepository.findAll();
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
        Context context = new Context();
        context.setVariable("userName", user.getUserName());
        context.setVariable("userEmail", user.getEmail());
        context.setVariable("subscriptions", subscriptions);

        String emailContent = templateEngine.process("EmailTemplate", context);

        MimeMessage mailMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mailMessage);

        try {
            helper.setTo(user.getEmail());
            helper.setSubject("Subscription Reminder: Services Nearing Expiration");
            helper.setText(emailContent, true);
            mailSender.send(mailMessage);

            logger.info("Subscription reminder email sent to {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send subscription reminder to {}", user.getEmail(), e);
            throw new RuntimeException("Failed to send subscription reminder email", e);
        }
    }


}


