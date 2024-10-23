package com.sms.SubscriptionService.repository;

import com.sms.SubscriptionService.entity.Subscription;
import com.sms.SubscriptionService.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Integer> {

    List<Subscription> findByUserIdAndDbstatus(Integer userId, Status dbstatus);
    List<Subscription> findByUserId(Integer userId);

    boolean existsByUserIdAndServiceIdAndDbstatus(Integer userId, Integer serviceId, Status dbstatus);
    Optional<Subscription> findById(Integer subscriptionId);

}
