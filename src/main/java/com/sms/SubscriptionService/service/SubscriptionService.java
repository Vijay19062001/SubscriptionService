package com.sms.SubscriptionService.service;

import com.sms.SubscriptionService.entity.Subscription;
import com.sms.SubscriptionService.enums.Status;
import com.sms.SubscriptionService.model.SubscriptionModel;

import java.util.List;


public interface SubscriptionService {
    public SubscriptionModel createSubscription(SubscriptionModel subscriptionModel) ;
    SubscriptionModel updateSubscription(Integer subscriptionId, SubscriptionModel subscriptionModel);
    public void cancelSubscription(Integer subscriptionId, Integer userId) ;
    public List<Subscription> getSubscriptionDetails(Integer userId) ;
    public List<Subscription> getAllSubscriptions() ;
    public boolean checkActiveSubscription(Integer userId, String serviceId) ;
    public List<SubscriptionModel> getSubscriptionUserId(Integer userId, Status status) ;


    }
