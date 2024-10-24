package com.sms.SubscriptionService.model;

import lombok.Data;

@Data
public class PaymentModel {

    private String id;
    private String subscriptionId;
    private String amount;
    private String paymentMethod;
    private String transactionType;
    private String paymentStatus;
    private String createdBy;
}
