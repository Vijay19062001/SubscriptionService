package com.sms.SubscriptionService.repository;

import com.sms.SubscriptionService.entity.AuthTokens;
import com.sms.SubscriptionService.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthTokens, Integer> {

    List<AuthTokens> findByUserIdAndDbstatus(int userId, Status dbstatus);

    Optional<AuthTokens> findByTokenAndDbstatus(String authToken, Status dbstatus);
    Optional<AuthTokens> findByToken(String token);


    List<AuthTokens> findByExpireDateBeforeAndDbstatus(LocalDateTime expireDate, Status dbstatus);

}
