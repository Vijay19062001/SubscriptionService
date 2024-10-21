package com.sms.SubscriptionService.service.servicesImpl;

import com.sms.SubscriptionService.entity.AuthTokens;
import com.sms.SubscriptionService.entity.Users;
import com.sms.SubscriptionService.enums.Status;
import com.sms.SubscriptionService.exception.custom.BusinessValidationException;
import com.sms.SubscriptionService.repository.AuthTokenRepository;
import com.sms.SubscriptionService.repository.UserRepository;
import com.sms.SubscriptionService.service.AuthTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthTokenServiceImpl implements AuthTokenService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthTokenRepository authTokenRepository;
    private static final int TOKEN_VALIDITY_MINUTES = 120;

    @Override
    public String authenticateUser(String userName, String password) {
        Users user = userRepository.findByUserNameIgnoreCase(userName)
                .orElseThrow(() -> new BusinessValidationException("User not found"));

        if (!password.equals(user.getPassword())) {
            throw new BusinessValidationException("Invalid password");
        }

        return generateToken(user);
    }

    private String generateToken(Users user) {
        List<AuthTokens> existingTokens = authTokenRepository.findByUserIdAndDbstatus(user.getId(), Status.ACTIVE);
        for (AuthTokens token : existingTokens) {
            token.setDbstatus(Status.INACTIVE);
        }
        authTokenRepository.saveAll(existingTokens);

        String token = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

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

    public String validToken(String token) {
        AuthTokens authToken = authTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessValidationException("Invalid token"));

        if (LocalDate.now().isAfter(authToken.getExpireDate())) {
            throw new BusinessValidationException("Cannot refresh: Token is expired");
        }

        Users user = userRepository.findById(authToken.getUserId())
                .orElseThrow(() -> new BusinessValidationException("User not found"));
        return generateToken(user);
    }
}
