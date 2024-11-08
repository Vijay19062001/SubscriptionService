package com.sms.SubscriptionService.entity;

import com.sms.SubscriptionService.enums.Status;
import lombok.*;

@Data
public class PaymentTransaction {
    private String id;
    private String userId;
    private String accountNumber;
    private String serviceId;
    private Double amount;
    private String paymentStatus;
    private String paymentMethod;
    private String transactionType;
    private String createdBy;
    private String updatedBy;
    private Status status;
}
