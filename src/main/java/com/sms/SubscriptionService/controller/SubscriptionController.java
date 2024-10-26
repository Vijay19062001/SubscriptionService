package com.sms.SubscriptionService.controller;

import com.sms.SubscriptionService.dto.PaymentRequestDTO;
import com.sms.SubscriptionService.dto.PaymentResponse;
import com.sms.SubscriptionService.dto.SubscriptionRequest;
import com.sms.SubscriptionService.entity.PaymentTransaction;
import com.sms.SubscriptionService.entity.Subscription;
import com.sms.SubscriptionService.entity.Users;
import com.sms.SubscriptionService.enums.Status;
import com.sms.SubscriptionService.exception.custom.DuplicateSubscriptionException;
import com.sms.SubscriptionService.exception.custom.InvalidDateFormatException;
import com.sms.SubscriptionService.exception.custom.PaymentFailedException;
import com.sms.SubscriptionService.exception.custom.SubscriptionNotFoundException;
import com.sms.SubscriptionService.mapper.PaymentRequestMapper;
import com.sms.SubscriptionService.mapper.SubscriptionMapper;
import com.sms.SubscriptionService.model.SubscriptionModel;
import com.sms.SubscriptionService.repository.ServiceRepository;
import com.sms.SubscriptionService.repository.SubscriptionRepository;
import com.sms.SubscriptionService.repository.UserRepository;
import com.sms.SubscriptionService.service.AuthTokenService;
import com.sms.SubscriptionService.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/subscription")
@Tag(name = "Subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final AuthTokenService authTokenService;
    private final SubscriptionRepository subscriptionRepository;
    private final ServiceRepository serviceRepository;
    private final SubscriptionMapper subscriptionMapper;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionController.class);

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create Subscription", description = "Creates a new subscription with the provided details.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Subscription created successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized: Invalid or missing token"),
                    @ApiResponse(responseCode = "409", description = "Conflict: Duplicate active subscription for the same service"),
                    @ApiResponse(responseCode = "400", description = "Bad Request: Invalid subscription details")
            }
    )
    public ResponseEntity<String> createSubscription(@RequestHeader("Authorization") String token,
                                                     @RequestHeader("UserId") String userId,
                                                     @Valid @RequestBody SubscriptionRequest subscriptionRequest) {
        logger.info("Attempting to create a subscription for userId: {}", userId);

        if (token == null || !token.startsWith("Bearer ")) {
            logger.warn("Unauthorized access attempt: Invalid or missing token.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Invalid or missing token");
        }

        String actualToken = token.substring(7);

        if (!authTokenService.isUserValid(actualToken)) {
            logger.warn("Unauthorized access attempt with invalid token.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }

        Optional<Users> user = userRepository.findByIdAndDbstatus(Integer.valueOf(userId), Status.ACTIVE);

        if(user.isEmpty()){
            throw new SubscriptionNotFoundException("User not found");
        }

        SubscriptionModel subscriptionModel = subscriptionRequest.getSubscriptionModel();
        subscriptionModel.setCreatedBy(userId);
        subscriptionModel.setUpdatedBy(userId);
        subscriptionModel.setUserId(userId);

        PaymentRequestDTO paymentRequestDTO = subscriptionRequest.getPaymentRequestDTO();

        try {
            boolean hasActiveSubscription = subscriptionService.checkActiveSubscription(Integer.valueOf(userId), subscriptionModel.getServiceId().toString());

            if (hasActiveSubscription) {
                logger.warn("Duplicate subscription attempt for serviceId: {} by userId: {}", subscriptionModel.getServiceId(), userId);
                return ResponseEntity.status(HttpStatus.CONFLICT).body("You already have an active subscription for this service.");
            }

            PaymentTransaction transaction = PaymentRequestMapper.toEntity(paymentRequestDTO);

            ResponseEntity<PaymentResponse> paymentResponse = restTemplate.postForEntity(
                    "http://localhost:8081/payment/transaction",transaction,
                    PaymentResponse.class
            );

            if (paymentResponse.getStatusCode().is2xxSuccessful() ) {
                SubscriptionModel createdSubscription = subscriptionService.createSubscription(subscriptionModel,user.get());

                String responseMessage = String.format("Subscription created successfully with ID: %s, User ID: %s, Service ID: %s.",
                        createdSubscription.getId(), createdSubscription.getUserId(), createdSubscription.getServiceId());

                logger.info("Subscription created successfully for userId: {}", userId);
                return ResponseEntity.status(HttpStatus.CREATED).body(responseMessage);
            } else {
                logger.error("Payment failed: {}", paymentResponse.getBody().getMessage());
                throw new PaymentFailedException("Payment failed: " + paymentResponse.getBody().getMessage());
            }
        } catch (PaymentFailedException e) {
            logger.warn("Payment failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment failed: " + e.getMessage());
        } catch (DuplicateSubscriptionException e) {
            logger.warn("Duplicate subscription error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Conflict: " + e.getMessage());
        } catch (InvalidDateFormatException e) {
            logger.warn("Invalid subscription dates: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Bad Request: " + e.getMessage());
        } catch (Exception e) {
            logger.error("An unexpected error occurred while creating the subscription: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error " + e.getMessage());
        }
    }


    @PostMapping("/cancel/{subscriptionId}")
    @Operation(summary = "Cancel Subscription",
            description = "Cancels the subscription for the specified subscription ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Subscription successfully cancelled"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized: Invalid or missing token"),
                    @ApiResponse(responseCode = "404", description = "Subscription not found")
            })
    public ResponseEntity<String> cancelSubscription(
            @PathVariable("subscriptionId") Integer subscriptionId,
            @RequestHeader("UserId") Integer userId,
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody(required = false) SubscriptionModel subscriptionModel) {

        logger.info("Attempting to cancel subscription with ID: {}", subscriptionId);

        if (token == null || !token.startsWith("Bearer ")) {
            logger.warn("Unauthorized access attempt: Invalid or missing token.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Invalid or missing token");
        }

        String actualToken = token.substring(7);
        if (!authTokenService.isUserValid(actualToken)) {
            logger.warn("Unauthorized access attempt with invalid token.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Invalid token");
        }

        authTokenService.validToken(actualToken, (userId));

        if (subscriptionModel != null) {
            subscriptionModel.setCreatedBy(userId.toString());
            subscriptionModel.setUpdatedBy(userId.toString());
            subscriptionModel.setEndDate(String.valueOf(LocalDateTime.now()));
            subscriptionModel.setUserId(String.valueOf(userId));
        }

        subscriptionService.cancelSubscription(subscriptionId, userId);

        logger.info("Subscription with ID: {} cancelled successfully", subscriptionId);
        return ResponseEntity.ok("Subscription with ID " + subscriptionId + " successfully cancelled.");
    }


    @GetMapping("/details")
    @Operation(
            summary = "Get All Subscription Details",
            description = "Retrieves all subscriptions available in the system.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List of all subscriptions retrieved successfully")
            }
    )
    public ResponseEntity<List<Subscription>> getAllSubscriptionDetails(@RequestHeader("Authorization") String token) {

        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
        }

        String actualToken = token.substring(7);
        if (!authTokenService.isUserValid(actualToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
        }
        List<Subscription> subscriptions = subscriptionService.getAllSubscriptions();

        if (subscriptions.isEmpty()) {
            return ResponseEntity.ok().body(Collections.emptyList());
        }

        return ResponseEntity.ok(subscriptions);
    }

}