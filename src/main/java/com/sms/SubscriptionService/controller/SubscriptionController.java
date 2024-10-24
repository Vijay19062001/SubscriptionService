package com.sms.SubscriptionService.controller;

import com.sms.SubscriptionService.entity.ServiceEntity;
import com.sms.SubscriptionService.entity.Subscription;
import com.sms.SubscriptionService.exception.custom.BusinessValidationException;
import com.sms.SubscriptionService.exception.custom.DuplicateSubscriptionException;
import com.sms.SubscriptionService.exception.custom.InvalidDateFormatException;
import com.sms.SubscriptionService.exception.custom.SubscriptionNotFoundException;
import com.sms.SubscriptionService.mapper.SubscriptionMapper;
import com.sms.SubscriptionService.model.SubscriptionModel;
import com.sms.SubscriptionService.repository.ServiceRepository;
import com.sms.SubscriptionService.repository.SubscriptionRepository;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
                                                     @Valid @RequestBody Map<String, String> subscriptionData,SubscriptionModel subscriptionModel) {
        String transactionId = subscriptionData.get("transactionId");
        String subscriptionId = subscriptionData.get("subscriptionId");
        String userId = subscriptionData.get("userId");
        String createdBy = subscriptionData.get("createdBy");

        logger.info("Attempting to create a subscription for userId: {}", userId);

        // Token validation
        if (token == null || !token.startsWith("Bearer ")) {
            logger.warn("Unauthorized access attempt: Invalid or missing token.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Invalid or missing token");
        }

        String actualToken = token.substring(7);
        if (!authTokenService.isUserValid(actualToken)) {
            logger.warn("Unauthorized access attempt with invalid token.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid token");
        }

        authTokenService.validToken(token, Integer.parseInt(userId));

        try {
            boolean hasActiveSubscription = subscriptionService.checkActiveSubscription(Integer.valueOf(userId), subscriptionId);
            if (hasActiveSubscription) {
                logger.warn("Duplicate subscription attempt for serviceId: {} by userId: {}", subscriptionId, userId);
                return ResponseEntity.status(HttpStatus.CONFLICT).body("message: You already have an active subscription for this service.");
            }

            ServiceEntity service = serviceRepository.findById(Integer.parseInt(subscriptionId))
                    .orElseThrow(() -> new SubscriptionNotFoundException("Service not found with ID: " + subscriptionId));


            SubscriptionModel createdSubscription = subscriptionService.createSubscription(subscriptionModel, transactionId, subscriptionId, userId, createdBy);

            logger.info("Subscription completed - subscriptionId: {}, transactionId: {}",
                    createdSubscription.getId(), createdSubscription.getTransactionId());

            logger.info("Subscription completed - subscriptionId: {}, transactionId: {}",
                    createdSubscription.getId(), createdSubscription.getTransactionId());

            String responseMessage = String.format("Subscription created successfully with ID: %s, User ID: %s, Service ID: %s.",
                    createdSubscription.getId(), createdSubscription.getUserId(), createdSubscription.getServiceId());

            return ResponseEntity.status(HttpStatus.CREATED).body(responseMessage);

        } catch (DuplicateSubscriptionException e) {
            logger.warn("Duplicate subscription error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Conflict: " + e.getMessage());
        } catch (InvalidDateFormatException e) {
            logger.warn("Invalid subscription dates: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Bad Request: " + e.getMessage());
        } catch (Exception e) {
            logger.error("An error occurred while creating the subscription: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Bad Request: " + e.getMessage());
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

        authTokenService.validToken(actualToken, userId);

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
    @Operation(summary = "Get Subscription Details",
            description = "Retrieves subscription details for a user.", responses = {
            @ApiResponse(responseCode = "200", description = "Subscription details retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "No subscriptions found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized: Invalid or missing token")
    })
    public ResponseEntity<List<Subscription>> getSubscriptionDetails(@RequestHeader("Authorization") String token, @RequestParam String userId) {
        logger.info("Retrieving subscription details for userId: {}", userId);

        if (token == null || !token.startsWith("Bearer ")) {
            logger.warn("Unauthorized access attempt: Invalid or missing token.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
        }

        String actualToken = token.substring(7);
        if (!authTokenService.isUserValid(actualToken)) {
            logger.warn("Unauthorized access attempt with invalid token.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
        }

        try {
            List<Subscription> subscriptions = subscriptionService.getSubscriptionDetails(Integer.valueOf(userId));
            if (subscriptions.isEmpty()) {
                logger.warn("No subscriptions found for userId: {}", userId);
                return ResponseEntity.notFound().build();
            }
            logger.info("Successfully retrieved subscriptions for userId: {}", userId);
            return ResponseEntity.ok(subscriptions);
        } catch (Exception e) {
            logger.error("An unexpected error occurred while retrieving subscription details: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Collections.emptyList());
        }
    }
}