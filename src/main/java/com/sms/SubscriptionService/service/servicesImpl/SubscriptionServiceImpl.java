package com.sms.SubscriptionService.service.servicesImpl;

import com.sms.SubscriptionService.dto.PaymentRequestDTO;
import com.sms.SubscriptionService.dto.PaymentResponse;
import com.sms.SubscriptionService.dto.SubscriptionRequest;
import com.sms.SubscriptionService.entity.PaymentTransaction;
import com.sms.SubscriptionService.entity.ServiceEntity;
import com.sms.SubscriptionService.entity.Users;
import com.sms.SubscriptionService.enums.Status;
import com.sms.SubscriptionService.exception.custom.*;
import com.sms.SubscriptionService.mapper.PaymentRequestMapper;
import com.sms.SubscriptionService.mapper.SubscriptionMapper;
import com.sms.SubscriptionService.model.SubscriptionModel;
import com.sms.SubscriptionService.entity.Subscription;
import com.sms.SubscriptionService.repository.ServiceRepository;
import com.sms.SubscriptionService.repository.SubscriptionRepository;
import com.sms.SubscriptionService.repository.UserRepository;
import com.sms.SubscriptionService.service.AuthTokenService;
import com.sms.SubscriptionService.service.SubscriptionService;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
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
    private final RestTemplate restTemplate;
    @Autowired
    private AuthTokenService authTokenService;

    @Autowired
    private ServiceRepository serviceRepository;

    @Transactional
    @Override
    public ResponseEntity<String> createSubscription(SubscriptionRequest subscriptionRequest, String userId) {
        logger.info("Starting subscription creation for user ID {}", userId);

        Optional<Users> userOptional = userRepository.findByIdAndDbstatus(Integer.valueOf(userId), Status.ACTIVE);
        if (userOptional.isEmpty()) {
            throw new SubscriptionNotFoundException("User not found or inactive");
        }

        Users user = userOptional.get();
        SubscriptionModel subscriptionModel = subscriptionRequest.getSubscriptionModel();
        subscriptionModel.setCreatedBy(userId);
        subscriptionModel.setUpdatedBy(userId);

        Integer serviceId = Integer.valueOf(subscriptionModel.getServiceId());
        ServiceEntity service = serviceRepository.findByIdAndDbstatus(serviceId, Status.ACTIVE)
                .orElseThrow(() -> new BusinessValidationException("Service not found or inactive with ID: " + serviceId));

        validateSubscriptionDates(subscriptionModel.getStartDate());

        if (subscriptionRepository.existsByUserIdAndServiceIdAndDbstatus(user.getId(), serviceId, Status.ACTIVE)) {
            throw new DuplicateSubscriptionException("User already has an active subscription for this service.");
        }

        PaymentRequestDTO paymentRequestDTO = subscriptionRequest.getPaymentRequestDTO();

        PaymentTransaction transaction = PaymentRequestMapper.toEntity(paymentRequestDTO);

        ResponseEntity<PaymentResponse> paymentResponse = restTemplate.postForEntity(
                "http://localhost:8081/payment/transaction", transaction, PaymentResponse.class);

        if (!paymentResponse.getStatusCode().is2xxSuccessful()) {
            throw new PaymentFailedException("Payment failed: " + paymentResponse.getBody().getMessage());
        }

        Subscription subscription = subscriptionMapper.toEntity(subscriptionModel, user);
        subscription.setUserId(user.getId());

        logger.info("Saving subscription for user ID {}", user.getId());
        Subscription savedSubscription = subscriptionRepository.save(subscription);

        sendSubscriptionConfirmationEmail(user, savedSubscription);

        String responseMessage = String.format("Subscription created successfully with ID: %s, User ID: %s, Service ID: %s.",
                savedSubscription.getId(), savedSubscription.getUserId(), savedSubscription.getServiceId());

        logger.info("Subscription created successfully for userId: {}", userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseMessage);
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
        subscription.setUpdatedDate(LocalDateTime.now());

        subscriptionRepository.save(subscription);

        logger.info("Subscription with ID {} successfully cancelled.", subscriptionId);
    }


    @Override
    public List<SubscriptionModel> getListSubscription(Integer userId, String token) {
        logger.info("Fetching subscription list for user ID {}", userId);

        if (!userRepository.existsById(userId)) {
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
    public List<SubscriptionModel> getAllSubscriptions(Integer id, LocalDate startDate, LocalDate endDate, String searchText, String sortBy, String sortDir) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Subscription> query = cb.createQuery(Subscription.class);
        Root<Subscription> subscriptionRoot = query.from(Subscription.class);

        Join<Subscription, ServiceEntity> serviceJoin = subscriptionRoot.join("serviceEntity", JoinType.LEFT);

        List<Predicate> predicates = new ArrayList<>();

        if (id != null) {
            predicates.add(cb.equal(subscriptionRoot.get("id"), id));
        }

        if (startDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(subscriptionRoot.get("startDate"), startDate));
        }

        if (endDate != null) {
            predicates.add(cb.lessThanOrEqualTo(subscriptionRoot.get("endDate"), endDate));
        }

        if (searchText != null && !searchText.trim().isEmpty()) {
            String lowerCaseSearchText = "%" + searchText.toLowerCase() + "%";
            predicates.add(cb.like(cb.lower(serviceJoin.get("serviceName")), lowerCaseSearchText));
        }

        query.where(predicates.toArray(new Predicate[0]));

        if (sortBy != null && !sortBy.trim().isEmpty()) {
            query.orderBy(sortDir.equalsIgnoreCase("ASC")
                    ? cb.asc(subscriptionRoot.get(sortBy))
                    : cb.desc(subscriptionRoot.get(sortBy)));
        }

        List<Subscription> subscriptions = entityManager.createQuery(query).getResultList();

        return subscriptions.stream()
                .map(subscriptionMapper::toModel)
                .collect(Collectors.toList());
    }


    @Value("${scheduler.enabled}")
    private boolean isSchedulerEnabled;

    @Scheduled(cron = "${scheduler.cron}")
    public void scheduleDailySubscriptionReminder() {
        logger.info("Scheduling daily subscription reminder emails.");

        if (!isSchedulerEnabled) {
            logger.info("Scheduler is disabled. Skipping daily subscription reminder emails.");
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate threeDaysFromNow = today.plusDays(3);

        try {
            List<Users> users = userRepository.findAll();

            for (Users user : users) {
                sendEmailReminderForUser(user, threeDaysFromNow, "Hotstar");
                sendEmailReminderForUser(user, threeDaysFromNow, "Netflix");
                sendEmailReminderForUser(user, threeDaysFromNow, "Amazon");

            }

        } catch (Exception e) {
            logger.error("Error in scheduleDailySubscriptionReminder", e);
        }
    }

    private void sendEmailReminderForUser(Users user, LocalDate threeDaysFromNow, String subscriptionType) {
        try {
            logger.info("Preparing {} email for user: {}", subscriptionType, user.getId());

            List<Subscription> activeSubscriptions = getSubscriptionsByTypeAndStatus(user.getId(), threeDaysFromNow, subscriptionType, Status.ACTIVE);
            List<Subscription> deactivatedSubscriptionsList = getSubscriptionsByTypeAndStatus(user.getId(), threeDaysFromNow, subscriptionType, Status.INACTIVE);

            if (!activeSubscriptions.isEmpty() || !deactivatedSubscriptionsList.isEmpty()) {
                sendSubscriptionReminderEmail(user, activeSubscriptions, deactivatedSubscriptionsList, subscriptionType);
                logger.info("Successfully sent {} reminder email to user: {}", subscriptionType, user.getId());
            } else {
                logger.info("No subscriptions found nearing expiration or recently deactivated for user: {}", user.getId());
            }

        } catch (Exception e) {
            logger.error("Failed to send {} subscription reminder to user: {}", subscriptionType, user.getId(), e);
        }
    }

    private List<Subscription> getSubscriptionsByTypeAndStatus(Integer userId, LocalDate threeDaysFromNow, String subscriptionType, Status status) {
        return subscriptionRepository.findByUserIdAndDbstatus(userId, status)
                .stream()
                .filter(subscription -> {
                    return serviceRepository.findById(subscription.getServiceId())
                            .map(serviceEntity -> serviceEntity.getServiceName().equalsIgnoreCase(subscriptionType))
                            .orElse(false);
                })
                .collect(Collectors.toList());
    }

    private void sendSubscriptionReminderEmail(Users user, List<Subscription> activeSubscriptions, List<Subscription> deactivatedSubscriptions, String subscriptionType) {
        try {
            Context context = new Context();
            context.setVariable("Name", user.getName());

            List<Map<String, String>> activeSubscriptionDetails = activeSubscriptions.stream()
                    .map(subscription -> {
                        ServiceEntity serviceEntity = serviceRepository.findById(subscription.getServiceId())
                                .orElseThrow(() -> new RuntimeException("Service not found for ID: " + subscription.getServiceId()));

                        Map<String, String> details = new HashMap<>();
                        details.put("serviceName", serviceEntity.getServiceName());
                        details.put("startDate", subscription.getStartDate().toString());
                        details.put("endDate", subscription.getEndDate().toString());
                        return details;
                    }).collect(Collectors.toList());

            List<Map<String, String>> deactivatedSubscriptionDetails = deactivatedSubscriptions.stream()
                    .map(subscription -> {
                        ServiceEntity serviceEntity = serviceRepository.findById(subscription.getServiceId())
                                .orElseThrow(() -> new RuntimeException("Service not found for ID: " + subscription.getServiceId()));

                        Map<String, String> details = new HashMap<>();
                        details.put("serviceName", serviceEntity.getServiceName());
                        details.put("subscriptionId", String.valueOf(subscription.getId()));
                        details.put("deactivationDate", subscription.getEndDate().toString());
                        return details;
                    }).collect(Collectors.toList());


            context.setVariable("activeSubscriptions", activeSubscriptionDetails);
            context.setVariable("deactivatedSubscriptions", deactivatedSubscriptionDetails);

            String emailContent = templateEngine.process("EmailTemplate", context);

            MimeMessage mailMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mailMessage);
            helper.setTo(user.getEmail());
            helper.setSubject("Subscription Reminder: Services Nearing Expiration or Recently Deactivated");
            helper.setText(emailContent, true);

            mailSender.send(mailMessage);
            logger.info("Subscription reminder email sent to {}", user.getEmail());

            saveNotificationDetails(user, activeSubscriptions, deactivatedSubscriptions);

        } catch (Exception e) {
            logger.error("Failed to send subscription reminder to {}", user.getEmail(), e);
            throw new RuntimeException("Failed to send subscription reminder email", e);
        }
    }

    private void saveNotificationDetails(Users user, List<Subscription> activeSubscriptions, List<Subscription> deactivatedSubscriptions) {
        activeSubscriptions.forEach(subscription -> {
            ServiceEntity serviceEntity = serviceRepository.findById(subscription.getServiceId())
                    .orElseThrow(() -> new RuntimeException("Service not found for ID: " + subscription.getServiceId()));

            logger.info("Notification sent for Active Subscription ID: {}, User ID: {}, Service: {}, Expiry Date: {}",
                    subscription.getId(), user.getId(), serviceEntity.getServiceName(), subscription.getEndDate());
        });

        deactivatedSubscriptions.forEach(subscription -> {
            ServiceEntity serviceEntity = serviceRepository.findById(subscription.getServiceId())
                    .orElseThrow(() -> new RuntimeException("Service not found for ID: " + subscription.getServiceId()));

            logger.info("Notification sent for Deactivated Subscription ID: {}, User ID: {}, Service: {}, Deactivation Date: {}",
                    subscription.getId(), user.getId(), serviceEntity.getServiceName(), subscription.getEndDate());
        });
    }


    private void sendSubscriptionConfirmationEmail(Users user, Subscription subscription) {
        try {
            ServiceEntity serviceEntity = serviceRepository.findById(subscription.getServiceId())
                    .orElseThrow(() -> new RuntimeException("Service not found for ID: " + subscription.getServiceId()));

            Context context = new Context();
            context.setVariable("Name", user.getName());
            context.setVariable("subscription", subscription);
            context.setVariable("serviceName", serviceEntity.getServiceName());

            String emailContent = templateEngine.process("SubscriptionConfirmationEmail", context);

            MimeMessage mailMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mailMessage);
            helper.setTo(user.getEmail());
            helper.setSubject("Subscription Confirmation for " + serviceEntity.getServiceName());
            helper.setText(emailContent, true);

            mailSender.send(mailMessage);

            logger.info("Subscription confirmation email for {} sent to {}", serviceEntity.getServiceName(), user.getEmail());

            saveConfirmationNotificationDetails(user, subscription);

        } catch (Exception e) {
            logger.error("Failed to send subscription confirmation to {}", user.getEmail(), e);
            throw new RuntimeException("Failed to send subscription confirmation email", e);
        }
    }


    private void saveConfirmationNotificationDetails(Users user, Subscription subscription) {
        logger.info("Confirmation sent for Subscription ID: {}, User ID: {}, Service: {}, Expiry Date: {}",
                subscription.getId(), user.getId(), subscription.getServiceId(), subscription.getEndDate());
    }

}
