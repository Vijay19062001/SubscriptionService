package com.sms.SubscriptionService.service;

import com.sms.SubscriptionService.model.SubscriptionModel;


public interface SubscriptionService {
    public SubscriptionModel createSubscription(SubscriptionModel subscriptionModel) ;
    public void renewSubscription(Integer subscriptionId, SubscriptionModel subscriptionModel) ;
    SubscriptionModel updateSubscription(Integer subscriptionId, SubscriptionModel subscriptionModel);
    public void deleteSubscription(Integer subscriptionId) ;
    public void cancelSubscription(Integer subscriptionId) ;



    }