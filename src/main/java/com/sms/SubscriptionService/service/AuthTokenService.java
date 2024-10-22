package com.sms.SubscriptionService.service;

import java.util.Map;

public interface AuthTokenService {

    public Map<String, String> authenticateUser(String userName, String password) ;
    public boolean isUserValid(String token) ;
    public String validToken(String token) ;

    }
