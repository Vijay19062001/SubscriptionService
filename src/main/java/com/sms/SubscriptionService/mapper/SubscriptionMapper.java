package com.sms.SubscriptionService.mapper;

import com.sms.SubscriptionService.entity.Subscription;
import com.sms.SubscriptionService.enums.Status;
import com.sms.SubscriptionService.exception.custom.InvalidDateFormatException;
import com.sms.SubscriptionService.model.SubscriptionModel;
import com.sms.SubscriptionService.utils.DateUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SubscriptionMapper {

    public Subscription toEntity(SubscriptionModel subscriptionModel) throws InvalidDateFormatException {
        Subscription subscription = new Subscription();

        subscription.setUserId(Integer.parseInt(subscriptionModel.getUserId()));
        subscription.setServiceId(Integer.parseInt(subscriptionModel.getServiceId()));

        LocalDateTime startDateTime = DateUtils.convertToLocalDateTime(subscriptionModel.getStartDate());
        subscription.setStartDate(startDateTime);

        subscription.setDbstatus(Status.fromString(String.valueOf(subscriptionModel.getDbstatus())));

        return subscription;
    }

    public SubscriptionModel toModel(Subscription subscription) throws InvalidDateFormatException {
        SubscriptionModel subscriptionModel = new SubscriptionModel();

        subscriptionModel.setId(String.valueOf(subscription.getId()));
        subscriptionModel.setUserId(String.valueOf(subscription.getUserId()));
        subscriptionModel.setServiceId(String.valueOf(subscription.getServiceId()));

        String formattedStartDate = DateUtils.localDateToString(DateUtils.localDateTimeToLocalDate(subscription.getStartDate()));
        subscriptionModel.setStartDate(formattedStartDate);


        subscriptionModel.setDbstatus(Status.valueOf(subscription.getDbstatus().name()));

        return subscriptionModel;
    }
}
