package com.sms.SubscriptionService.mapper;

import com.sms.SubscriptionService.dto.PaymentRequestDTO;
import com.sms.SubscriptionService.entity.PaymentTransaction;
import com.sms.SubscriptionService.enums.Status;

public class PaymentRequestMapper {

    public static PaymentTransaction toEntity(PaymentRequestDTO dto) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setUserId(dto.getUserId());
        transaction.setAccountNumber(String.valueOf(dto.getAccountNumber()));
        transaction.setServiceId(String.valueOf(Integer.parseInt(dto.getServiceId())));
        transaction.setAmount(Double.valueOf(dto.getAmount()));
        transaction.setPaymentStatus(dto.getPaymentStatus());
        transaction.setPaymentMethod(dto.getPaymentMethod());
        transaction.setTransactionType(dto.getTransactionType());
        transaction.setCreatedBy(dto.getUserId());
        transaction.setUpdatedBy(dto.getUserId());
        transaction.setStatus(Status.ACTIVE);

        return transaction;
    }

    public static PaymentRequestDTO toDTO(PaymentTransaction entity) {
        PaymentRequestDTO dto = new PaymentRequestDTO();
        dto.setId(entity.getId());
        dto.setAccountNumber(entity.getAccountNumber());
        dto.setServiceId(entity.getServiceId());
        dto.setAmount(String.valueOf(entity.getAmount()));
        dto.setPaymentStatus(entity.getPaymentStatus());
        dto.setPaymentMethod(entity.getPaymentMethod());
        dto.setTransactionType(entity.getTransactionType());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setStatus(entity.getStatus().getDbStatus());
        return dto;
    }
}
