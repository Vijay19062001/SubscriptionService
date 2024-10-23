package com.sms.SubscriptionService.mapper;


import com.sms.SubscriptionService.entity.AuthTokens;
import com.sms.SubscriptionService.entity.Users;
import com.sms.SubscriptionService.enums.Status;
import com.sms.SubscriptionService.exception.custom.InvalidDateFormatException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Component
public class AuthTokenMapper {


    public AuthTokens toEntity(Users user, String token, LocalDateTime now) throws InvalidDateFormatException {
        AuthTokens authTokens = new AuthTokens();
        authTokens.setUserId(user.getId());
        authTokens.setIssuedDate(now);
        authTokens.setExpireDate(now.plusMinutes(2));
        authTokens.setToken(token);
        authTokens.setDbstatus(Status.ACTIVE);
        authTokens.setCreatedDate(now);
        authTokens.setUpdatedDate(now);
        authTokens.setCreatedBy(user.getUserName());
        authTokens.setUpdatedBy(user.getUserName());
        return authTokens;
    }
}

