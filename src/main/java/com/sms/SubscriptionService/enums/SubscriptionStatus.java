package com.sms.SubscriptionService.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SubscriptionStatus {
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE"),
    CANCELLED("CANCELLED"),
    SUSPENDED("SUSPENDED");

    private final String status;

    // Constructor
    SubscriptionStatus(String status) {
        this.status = status;
    }

    @JsonValue // Used for serialization
    public String getStatus() {
        return status;
    }

    @JsonCreator // Used for deserialization
    public static SubscriptionStatus fromString(String status) {
        for (SubscriptionStatus subscriptionStatus : SubscriptionStatus.values()) {
            if (subscriptionStatus.name().equalsIgnoreCase(status)) { // Use name() for case-insensitivity
                return subscriptionStatus;
            }
        }
        throw new IllegalArgumentException("No enum constant found for status: " + status);
    }
}
