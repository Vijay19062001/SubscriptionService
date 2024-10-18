package com.sms.SubscriptionService.repository;

import com.sms.SubscriptionService.entity.Subscription;
import com.sms.SubscriptionService.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Integer> {

    List<Subscription> findByUserIdAndStatus(Integer userId, SubscriptionStatus status);
}