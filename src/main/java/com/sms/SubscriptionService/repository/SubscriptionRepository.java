package com.sms.SubscriptionService.repository;

import com.sms.SubscriptionService.entity.Subscription;
import com.sms.SubscriptionService.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Integer> {
    boolean existsByUserIdAndServiceIdAndDbstatus(Integer userId, Integer serviceId, Status dbstatus);
    Optional<Subscription> findById(Integer id);
    Optional<Subscription> findByUserIdAndIdAndDbstatus(Integer userId, Integer id, Status dbstatus);
    List<Subscription> findByUserIdAndEndDateBetween(Integer userId, LocalDateTime startDate, LocalDateTime endDate);
    List<Subscription> findAllByUserId(int id);
    List<Subscription>findByUserIdAndDbstatus(Integer id, Status dbstatus);
}
