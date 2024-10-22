package com.sms.SubscriptionService.exception.custom;

public class InvalidSubscriptionDatesException extends RuntimeException {
    public InvalidSubscriptionDatesException(String message) {
        super(message);
    }
}
