package com.sms.SubscriptionService.service;

public interface AuthTokenService {

    public String authenticateUser(String userName, String password) ;
    public boolean isUserValid(String token) ;
    public String validToken(String token) ;


    }
