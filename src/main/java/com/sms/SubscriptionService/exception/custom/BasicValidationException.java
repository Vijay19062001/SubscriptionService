package com.sms.SubscriptionService.exception.custom;

public class BasicValidationException extends RuntimeException {
    public BasicValidationException(String message) {
        super(message);
    }
}
