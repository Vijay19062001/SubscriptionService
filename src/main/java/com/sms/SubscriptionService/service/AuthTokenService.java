package com.sms.SubscriptionService.service;

import java.util.Map;

public interface AuthTokenService {

    public Map<String, String> authenticateUser(String userName, String password) ;
    public Integer validToken(String token, int userId) ;
    public boolean isUserValid(String authToken) ;

    }
