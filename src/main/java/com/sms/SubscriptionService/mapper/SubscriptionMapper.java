package com.sms.SubscriptionService.mapper;

import com.sms.SubscriptionService.entity.Subscription;
import com.sms.SubscriptionService.entity.Users;
import com.sms.SubscriptionService.enums.Status;
import com.sms.SubscriptionService.exception.custom.InvalidDateFormatException;
import com.sms.SubscriptionService.model.SubscriptionModel;
import com.sms.SubscriptionService.utils.DateUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SubscriptionMapper {

    public Subscription toEntity(SubscriptionModel subscriptionModel, Users users) throws InvalidDateFormatException {
        Subscription subscription = new Subscription();

        subscription.setUserId(Integer.parseInt(subscriptionModel.getUserId()));
        subscription.setServiceId(Integer.parseInt(subscriptionModel.getServiceId()));
        LocalDateTime startDateTime = DateUtils.convertToLocalDateTime(subscriptionModel.getStartDate());
        subscription.setStartDate(startDateTime);
        LocalDateTime now = LocalDateTime.now();
        subscription.setEndDate(now);
        subscription.setCreatedDate(now);
        subscription.setUpdatedDate(now);
        subscription.setCreatedBy( users.getUserName());
        subscription.setUpdatedBy(users.getUserName());

        subscription.setDbstatus(Status.ACTIVE);
        subscription.setTransactionId(Integer.parseInt(subscriptionModel.getTransactionId()));

        return subscription;
    }

    public SubscriptionModel toModel(Subscription subscription) throws InvalidDateFormatException {
        SubscriptionModel subscriptionModel = new SubscriptionModel();

        subscriptionModel.setId(String.valueOf(subscription.getId()));
        subscriptionModel.setUserId(String.valueOf(subscription.getUserId()));
        subscriptionModel.setServiceId(String.valueOf(subscription.getServiceId()));
        String formattedStartDate = DateUtils.localDateToString(DateUtils.localDateTimeToLocalDate(subscription.getStartDate()));
        subscriptionModel.setStartDate(formattedStartDate);
        subscriptionModel.setDbstatus(Status.ACTIVE);
        subscriptionModel.setTransactionId(String.valueOf(subscription.getTransactionId()));

        return subscriptionModel;
    }


}
