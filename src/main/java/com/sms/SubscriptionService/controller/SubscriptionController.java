package com.sms.SubscriptionService.controller;

import com.sms.SubscriptionService.model.SubscriptionModel;
import com.sms.SubscriptionService.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/subscription")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/create")
    public ResponseEntity<String> createSubscription(@RequestBody @Valid SubscriptionModel subscriptionModel) {
        SubscriptionModel createdSubscription = subscriptionService.createSubscription(subscriptionModel);
        String responseMessage = String.format("Subscription created successfully with ID: %s, User ID: %s, Service ID: %s.",
                createdSubscription.getId(), createdSubscription.getUserId(), createdSubscription.getServiceId());

        // Optionally, send confirmation email logic goes here...
        return ResponseEntity.ok(responseMessage);
    }

    @PutMapping("/update/{subscriptionId}")
    public ResponseEntity<String> updateSubscription(
            @PathVariable Integer subscriptionId,
            @RequestBody @Valid SubscriptionModel subscriptionModel) {
        SubscriptionModel updatedSubscription = subscriptionService.updateSubscription(subscriptionId, subscriptionModel);
        String responseMessage = String.format("Subscription updated successfully with ID: %s.", updatedSubscription.getId());
        return ResponseEntity.ok(responseMessage);
    }


    @PostMapping("/renew/{subscriptionId}")
    public ResponseEntity<String> renewSubscription(
            @PathVariable Integer subscriptionId,
            @RequestBody @Valid SubscriptionModel subscriptionModel) {
        subscriptionService.renewSubscription(subscriptionId, subscriptionModel);
        return ResponseEntity.ok("Subscription renewed successfully.");
    }

    @DeleteMapping("/{subscriptionId}")
    public ResponseEntity<String> deleteSubscription(@PathVariable Integer subscriptionId) {
        subscriptionService.deleteSubscription(subscriptionId);
        return ResponseEntity.ok("Subscription with ID " + subscriptionId + " successfully deleted.");
    }

    @PostMapping("/cancel/{subscriptionId}")
    public ResponseEntity<String> cancelSubscription(@PathVariable Integer subscriptionId) {
        subscriptionService.cancelSubscription(subscriptionId);
        return ResponseEntity.ok("Subscription with ID " + subscriptionId + " successfully cancelled.");
    }
}
