package com.sms.SubscriptionService.service.servicesImpl;

import com.sms.SubscriptionService.entity.AuthTokens;
import com.sms.SubscriptionService.entity.Users;
import com.sms.SubscriptionService.enums.Status;
import com.sms.SubscriptionService.exception.custom.BusinessValidationException;
import com.sms.SubscriptionService.repository.AuthTokenRepository;
import com.sms.SubscriptionService.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AuthTokenService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthTokenRepository authTokenRepository;

    public String authenticateUser(String userName, String password) {
        Users user = userRepository.findByuserName(userName)
                .orElseThrow(() -> new BusinessValidationException("User not found"));

        if (!password.equals(user.getPassword())) {
            throw new BusinessValidationException("Invalid password");
        }

        return generateToken(user);
    }

    private String generateToken(Users user) {
        // Invalidate existing tokens for the user
        List<AuthTokens> existingTokens = authTokenRepository.findByUserIdAndDbstatus(user.getId(), Status.ACTIVE);
        for (AuthTokens token : existingTokens) {
            token.setDbstatus(Status.INACTIVE);
        }
        authTokenRepository.saveAll(existingTokens);

        String token = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        AuthTokens authTokens = new AuthTokens();
        authTokens.setUserId(user.getId());
        authTokens.setIssuedDate(LocalDate.from(now)); // Use LocalDateTime
        authTokens.setExpireDate(LocalDate.from(now.plusMinutes(2))); // Token valid for 2 minutes
        authTokens.setToken(token); // Store as String
        authTokens.setDbstatus(Status.ACTIVE);
        authTokens.setCreatedDate(LocalDate.from(now)); // Use LocalDateTime
        authTokens.setUpdatedDate(LocalDate.from(now)); // Use LocalDateTime
        authTokens.setCreatedBy(user.getUserName());
        authTokens.setUpdatedBy(user.getUserName());

        authTokenRepository.save(authTokens);

        return token;
    }

    public boolean isUserValid(String token) {
        AuthTokens authToken = authTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessValidationException("Invalid token"));

        Users user = userRepository.findById(authToken.getUserId())
                .orElseThrow(() -> new BusinessValidationException("User not found"));

        return user != null;
    }
}
