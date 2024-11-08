package com.sms.SubscriptionService.controller;

import com.sms.SubscriptionService.dto.SubscriptionRequest;
import com.sms.SubscriptionService.model.SubscriptionModel;
import com.sms.SubscriptionService.service.AuthTokenService;
import com.sms.SubscriptionService.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/subscription")
@Tag(name = "Subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final AuthTokenService authTokenService;

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
        return subscriptionService.createSubscription(subscriptionRequest, userId);
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

        subscriptionService.cancelSubscription(subscriptionId, userId);

        logger.info("Subscription with ID: {} cancelled successfully", subscriptionId);
        return ResponseEntity.ok("Subscription with ID " + subscriptionId + " successfully cancelled.");
    }

    @GetMapping("/user/{userId}")
    @Operation(
            summary = "Get User Subscriptions",
            description = "Retrieves a list of subscriptions for the specified user by user ID.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "List of user subscriptions retrieved successfully",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = SubscriptionModel.class)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "User not found or no subscriptions available for the user"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized access due to invalid or missing token"
                    )
            }
    )
    public ResponseEntity<?> getUserSubscriptions(
            @PathVariable Integer userId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String token) {

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

        List<SubscriptionModel> subscriptions = subscriptionService.getListSubscription(userId, actualToken);

        if (subscriptions.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No subscriptions available for the user.");
        }

        return new ResponseEntity<>(subscriptions, HttpStatus.OK);
    }


    @GetMapping
    @Operation(
            summary = "Get All Subscriptions",
            description = "Retrieves a paginated list of subscriptions with optional filters.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of subscriptions retrieved successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized access due to invalid token"),
                    @ApiResponse(responseCode = "404", description = "No subscriptions found")
            }
    )
    public ResponseEntity<List<SubscriptionModel>> getAllSubscriptions(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String searchText,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {

        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        String actualToken = token.substring(7);

        if (!authTokenService.isUserValid(actualToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        List<SubscriptionModel> subscriptions = subscriptionService.getAllSubscriptions(
               id, startDate, endDate, searchText, sortBy, sortDir);

        return ResponseEntity.ok(subscriptions);

    }
}