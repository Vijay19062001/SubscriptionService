package com.sms.SubscriptionService.exception.custom;

public class InvalidSubscriptionException extends RuntimeException {
    public InvalidSubscriptionException(String message) {
        super(message);
    }
}
