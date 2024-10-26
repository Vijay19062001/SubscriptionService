package com.sms.SubscriptionService.dto;

import com.sms.SubscriptionService.model.SubscriptionModel;
import jakarta.validation.Valid;

public class SubscriptionRequest {

    @Valid
    private SubscriptionModel subscriptionModel;

    @Valid
    private PaymentRequestDTO paymentRequestDTO;

    public SubscriptionModel getSubscriptionModel() {
        return subscriptionModel;
    }

    public void setSubscriptionModel(SubscriptionModel subscriptionModel) {
        this.subscriptionModel = subscriptionModel;
    }

    public PaymentRequestDTO getPaymentRequestDTO() {
        return paymentRequestDTO;
    }

    public void setPaymentRequestDTO(PaymentRequestDTO paymentRequestDTO) {
        this.paymentRequestDTO = paymentRequestDTO;
    }
}
