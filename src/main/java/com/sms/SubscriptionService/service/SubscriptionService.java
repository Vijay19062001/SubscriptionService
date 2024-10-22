package com.sms.SubscriptionService.service;

import com.sms.SubscriptionService.entity.Subscription;
import com.sms.SubscriptionService.model.SubscriptionModel;

import java.util.List;


public interface SubscriptionService {
    public SubscriptionModel createSubscription(SubscriptionModel subscriptionModel) ;
    SubscriptionModel updateSubscription(Integer subscriptionId, SubscriptionModel subscriptionModel);
    public void cancelSubscription(Integer subscriptionId) ;
    public List<Subscription> getSubscriptionDetails(Integer userId) ;
    public List<Subscription> getAllSubscriptions() ;
    public boolean checkActiveSubscription(String userId, String serviceId) ;


    }
