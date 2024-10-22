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
    private static final int TOKEN_VALIDITY_MINUTES = 120;

    public AuthTokens toEntity(Users user, String token, LocalDateTime now) throws InvalidDateFormatException {
        AuthTokens authTokens = new AuthTokens();
        authTokens.setUserId(user.getId());
        authTokens.setIssuedDate(LocalDate.from(now));
        authTokens.setExpireDate(LocalDate.from(now.plusMinutes(TOKEN_VALIDITY_MINUTES)));
        authTokens.setToken(token);
        authTokens.setDbstatus(Status.ACTIVE);
        authTokens.setCreatedDate(LocalDate.from(now));
        authTokens.setUpdatedDate(LocalDate.from(now));
        authTokens.setCreatedBy(user.getUserName());
        authTokens.setUpdatedBy(user.getUserName());
        return authTokens;
    }
}

