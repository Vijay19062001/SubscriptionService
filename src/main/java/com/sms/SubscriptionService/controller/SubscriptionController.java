package com.sms.SubscriptionService.controller;

import com.sms.SubscriptionService.entity.Subscription;
import com.sms.SubscriptionService.model.SubscriptionModel;
import com.sms.SubscriptionService.service.AuthTokenService;
import com.sms.SubscriptionService.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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


    @PostMapping("/create")
    @Operation( summary = "Create Subscription",
            description = "Creates a new subscription with the provided details.", responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(description = "Subscription created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(description = "Unauthorized: Invalid or missing token")
    })
    public ResponseEntity<String> createSubscription(@RequestBody @Valid SubscriptionModel subscriptionModel, @RequestHeader("Authorization") String token) {

        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Invalid or missing token");
        }

        String actualToken = token.substring(7);

        if (!authTokenService.isUserValid(actualToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Invalid token");
        }
        SubscriptionModel createdSubscription = subscriptionService.createSubscription(subscriptionModel);
        String responseMessage = String.format("Subscription created successfully with ID: %s, User ID: %s, Service ID: %s.",
                createdSubscription.getId(), createdSubscription.getUserId(), createdSubscription.getServiceId());

        return ResponseEntity.ok(responseMessage);
    }

    @PutMapping("/update/{subscriptionId}")
    @Operation(
            summary = "Update Subscription",
            description = "Updates the subscription details for the specified subscription ID.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse( description = "Subscription updated successfully"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse( description = "Subscription not found")
            }
    )
    public ResponseEntity<String> updateSubscription(
            @PathVariable Integer subscriptionId,
            @RequestBody @Valid SubscriptionModel subscriptionModel, @RequestHeader("Authorization") String token) {

        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Invalid or missing token");
        }

        String actualToken = token.substring(7);
        if (!authTokenService.isUserValid(actualToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Invalid token");
        }

        SubscriptionModel updatedSubscription = subscriptionService.updateSubscription(subscriptionId, subscriptionModel);
        String responseMessage = String.format("Subscription updated successfully with ID: %s.", updatedSubscription.getId());
        return ResponseEntity.ok(responseMessage);
    }


    @PostMapping("/renew/{subscriptionId}")
    @Operation(
            summary = "Renew Subscription",
            description = "Renews the subscription for the specified subscription ID.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Subscription renewed successfully"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Subscription not found")
            }
    )
    public ResponseEntity<String> renewSubscription(
            @PathVariable Integer subscriptionId,
            @RequestBody @Valid SubscriptionModel subscriptionModel) {
        subscriptionService.renewSubscription(subscriptionId, subscriptionModel);
        return ResponseEntity.ok("Subscription renewed successfully.");
    }

    @DeleteMapping("/{subscriptionId}")
    @Operation(
            summary = "Delete Subscription",
            description = "Deletes the subscription for the specified subscription ID.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Subscription successfully deleted"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Subscription not found")
            }
    )
    public ResponseEntity<String> deleteSubscription(@PathVariable Integer subscriptionId, @RequestHeader("Authorization") String token) {

        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Invalid or missing token");
        }

        String actualToken = token.substring(7);
        if (!authTokenService.isUserValid(actualToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Invalid token");
        }
        subscriptionService.deleteSubscription(subscriptionId);
        return ResponseEntity.ok("Subscription with ID " + subscriptionId + " successfully deleted.");
    }

    @PostMapping("/cancel/{subscriptionId}")
    @Operation(
            summary = "Cancel Subscription",
            description = "Cancels the subscription for the specified subscription ID.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Subscription successfully cancelled"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Subscription not found")
            }
    )
    public ResponseEntity<String> cancelSubscription(@PathVariable Integer subscriptionId) {
        subscriptionService.cancelSubscription(subscriptionId);
        return ResponseEntity.ok("Subscription with ID " + subscriptionId + " successfully cancelled.");
    }


    @GetMapping("/{userId}")
    @Operation(
            summary = "Get Subscription Details",
            description = "Retrieves the subscription details for the specified user ID.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List of subscriptions retrieved successfully"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid user ID")
            }
    )
    public ResponseEntity<List<Subscription>> getSubscriptionDetails(@PathVariable Integer userId) {
        if (userId == null || userId <= 0) {
            return ResponseEntity.badRequest().body(Collections.emptyList()); // Or return an error message
        }

        List<Subscription> subscriptions = subscriptionService.getSubscriptionDetails(userId);
        return ResponseEntity.ok(subscriptions);
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
