package com.sms.SubscriptionService.repository;

import com.sms.SubscriptionService.entity.Subscription;
import com.sms.SubscriptionService.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Integer> {

    List<Subscription> findByServiceIdAndDbstatus(Integer serviceId, Status dbstatus);
    boolean existsByUserIdAndServiceIdAndDbstatus(Integer userId, Integer serviceId, Status dbstatus);
    Optional<Subscription> findById(Integer id);
    Optional<Subscription> findByUserIdAndIdAndDbstatus(Integer userId, Integer id, Status dbstatus);
    List<Subscription> findByUserIdAndEndDateBetween(Integer userId, LocalDate startDate, LocalDate endDate);
    List<Subscription> findAllByUserId(int id);
}
