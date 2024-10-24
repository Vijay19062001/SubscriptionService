package com.sms.SubscriptionService.repository;

import com.sms.SubscriptionService.entity.ServiceEntity;
import com.sms.SubscriptionService.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServiceRepository extends JpaRepository<ServiceEntity, Integer> {
    Optional<ServiceEntity> findByIdAndDbstatus(Integer id, Status dbstatus);}
