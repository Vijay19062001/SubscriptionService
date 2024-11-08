package com.sms.SubscriptionService.dto;

import lombok.*;

@Data
public class PaymentRequestDTO {

    private String id;
    private String userId;
    private String accountNumber;
    private String serviceId;
    private String amount;
    private String paymentStatus;
    private String paymentMethod;
    private String transactionType;
    private String createdBy;
    private String updatedBy;
    private String status;

}
