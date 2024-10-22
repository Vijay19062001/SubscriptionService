package com.sms.SubscriptionService.repository;

import com.sms.SubscriptionService.entity.Subscription;
import com.sms.SubscriptionService.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Integer> {
    List<Subscription> findByUserIdAndDbstatus(Integer userId, Status dbstatus);
    List<Subscription> findByUserId(Integer userId);
    List<Subscription> findActiveSubscriptionsByUserId(Integer userId);
}
