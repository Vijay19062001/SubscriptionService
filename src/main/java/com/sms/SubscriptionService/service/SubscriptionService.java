package com.sms.SubscriptionService.service;

import com.sms.SubscriptionService.entity.ServiceEntity;
import com.sms.SubscriptionService.entity.Subscription;
import com.sms.SubscriptionService.entity.Users;
import com.sms.SubscriptionService.model.SubscriptionModel;

import java.util.List;


public interface SubscriptionService {
    public SubscriptionModel createSubscription(SubscriptionModel subscriptionModel, Users users) ;
    public void cancelSubscription(Integer subscriptionId, Integer userId) ;
    public List<Subscription> getAllSubscriptions() ;
    public boolean checkActiveSubscription(Integer userId, String serviceId) ;
    public List<SubscriptionModel> getListSubscription(Integer userId) ;


    }
