package com.sms.SubscriptionService.repository;

import com.sms.SubscriptionService.entity.Service;
import com.sms.SubscriptionService.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServiceRepository extends JpaRepository<Service, Integer> {
    Optional<Service> findByIdAndDbstatus(Integer id, Status dbstatus);}
