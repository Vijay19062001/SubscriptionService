package com.sms.SubscriptionService.controller;

import com.sms.SubscriptionService.entity.Subscription;
import com.sms.SubscriptionService.exception.custom.BusinessValidationException;
import com.sms.SubscriptionService.exception.custom.DuplicateSubscriptionException;
import com.sms.SubscriptionService.exception.custom.InvalidSubscriptionDatesException;
import com.sms.SubscriptionService.model.SubscriptionModel;
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
import java.util.Collections;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/subscription")
@Tag(name = "Subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final AuthTokenService authTokenService;
    private final SubscriptionRepository subscriptionRepository;
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionController.class);

    @PostMapping("/create")
    @Operation(summary = "Create Subscription",
            description = "Creates a new subscription with the provided details.", responses = {
            @ApiResponse(description = "Subscription created successfully"),
            @ApiResponse(description = "Unauthorized: Invalid or missing token"),
            @ApiResponse(description = "Conflict: Duplicate active subscription for the same service"),
            @ApiResponse(description = "Bad Request: Invalid subscription details")
    })
    public ResponseEntity<String> createSubscription(@RequestBody @Valid SubscriptionModel subscriptionModel,
                                                     @RequestHeader(value = "Authorization", required = false) String token,
                                                     @RequestHeader(value = "UserId", required = false) Integer userId) {
        logger.info("Attempting to create a subscription for userId: {}", subscriptionModel.getUserId());

        if (token == null) {
            logger.warn("Unauthorized access attempt: Token is missing.");
            throw new BusinessValidationException("Unauthorized: Missing token");
        }

        if (!token.startsWith("Bearer ")) {
            logger.warn("Unauthorized access attempt: Invalid token format.");
            throw new BusinessValidationException("Unauthorized: Invalid token format");
        }

        String actualToken = token.substring(7);
        logger.info("Extracted Token: {}", actualToken);
        if (!authTokenService.isUserValid(actualToken)) {
            logger.warn("Unauthorized access attempt with expired token.");
            throw new BusinessValidationException("message: expired token");
        }

        try {
            if (subscriptionModel.getServiceId() == null || subscriptionModel.getServiceId().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Bad Request: Service ID is required.");
            }

            boolean hasActiveSubscriptionForService = subscriptionService.checkActiveSubscription(
                    subscriptionModel.getUserId(), subscriptionModel.getServiceId());

            if (hasActiveSubscriptionForService) {
                logger.warn("Duplicate subscription attempt for serviceId: {}", subscriptionModel.getServiceId());
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Conflict: You already have an active subscription for this service.");
            }


            SubscriptionModel createdSubscription = subscriptionService.createSubscription(subscriptionModel);
            String responseMessage = String.format("Subscription created successfully with ID: %s, User ID: %s, Service ID: %s.",
                    createdSubscription.getId(), createdSubscription.getUserId(), createdSubscription.getServiceId());

            logger.info("Subscription created successfully for userId: {}", subscriptionModel.getUserId());
            return ResponseEntity.ok(responseMessage);
        } catch (DuplicateSubscriptionException e) {
            logger.warn("message: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Conflict: " + e.getMessage());
        } catch (InvalidSubscriptionDatesException e) {
            logger.warn("Bad Request: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Bad Request: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Bad Request: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Bad Request: "+ e.getMessage());
        }
    }

    @PutMapping("/update/{subscriptionId}")
    @Operation(summary = "Update Subscription",
            description = "Updates the subscription details for the specified subscription ID.", responses = {
            @ApiResponse(description = "Subscription updated successfully"),
            @ApiResponse(description = "Bad Request: Invalid subscription ID"),
            @ApiResponse(description = "Unauthorized: Invalid or missing token")
    })
    public ResponseEntity<String> updateSubscription(
            @PathVariable Integer subscriptionId,
            @RequestBody @Valid SubscriptionModel subscriptionModel, @RequestHeader("Authorization") String token) {

        logger.info("Attempting to update subscription with ID: {}", subscriptionId);

        if (token == null || !token.startsWith("Bearer ")) {
            logger.warn("Unauthorized access attempt: Invalid or missing token.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Invalid or missing token");
        }

        String actualToken = token.substring(7);
        if (!authTokenService.isUserValid(actualToken)) {
            logger.warn("Unauthorized access attempt with invalid token.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Invalid token");
        }

        try {
            SubscriptionModel updatedSubscription = subscriptionService.updateSubscription(subscriptionId, subscriptionModel);
            String responseMessage = String.format("Subscription updated successfully with ID: %s.", updatedSubscription.getId());

            logger.info("Subscription with ID: {} updated successfully", subscriptionId);
            return ResponseEntity.ok(responseMessage);
        } catch (BusinessValidationException e) {
            logger.error("Business validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Bad Request: " + e.getMessage());
        } catch (Exception e) {
            logger.error("An unexpected error occurred while updating the subscription: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Bad Request: Unable to update subscription. Please check your details.");
        }
    }


    @PostMapping("/cancel/{subscriptionId}")
    @Operation(summary = "Cancel Subscription",
            description = "Cancels the subscription for the specified subscription ID.", responses = {
            @ApiResponse(responseCode = "200", description = "Subscription successfully cancelled"),
            @ApiResponse(responseCode = "404", description = "Subscription not found")
    })
    public ResponseEntity<String> cancelSubscription(@PathVariable Integer subscriptionId, @RequestHeader("Authorization") String token) {
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

        try {
            subscriptionService.cancelSubscription(subscriptionId);
            logger.info("Subscription with ID: {} cancelled successfully", subscriptionId);
            return ResponseEntity.ok("Subscription with ID " + subscriptionId + " successfully cancelled.");
        } catch (BusinessValidationException e) {
            logger.error("Business validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Bad Request: " + e.getMessage());
        } catch (Exception e) {
            logger.error("An unexpected error occurred while cancelling the subscription: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Bad Request: Unable to cancel subscription.");
        }
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