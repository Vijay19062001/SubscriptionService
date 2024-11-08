package com.sms.SubscriptionService.service;

import com.sms.SubscriptionService.dto.SubscriptionRequest;
import com.sms.SubscriptionService.model.SubscriptionModel;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

public interface SubscriptionService {
    public ResponseEntity<String> createSubscription(SubscriptionRequest subscriptionRequest, String userId) ;
    public void cancelSubscription(Integer subscriptionId, Integer userId) ;
    public List<SubscriptionModel> getAllSubscriptions(Integer id, LocalDate startDate, LocalDate endDate, String searchText, String sortBy, String sortDir) ;
    public boolean checkActiveSubscription(Integer userId, String serviceId) ;
    public List<SubscriptionModel> getListSubscription(Integer userId, String token) ;

}
