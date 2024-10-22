package com.sms.SubscriptionService.service.servicesImpl;

import com.sms.SubscriptionService.entity.AuthTokens;
import com.sms.SubscriptionService.entity.Users;
import com.sms.SubscriptionService.enums.Status;
import com.sms.SubscriptionService.exception.custom.BusinessValidationException;
import com.sms.SubscriptionService.mapper.AuthTokenMapper;
import com.sms.SubscriptionService.repository.AuthTokenRepository;
import com.sms.SubscriptionService.repository.UserRepository;
import com.sms.SubscriptionService.service.AuthTokenService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthTokenServiceImpl implements AuthTokenService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthTokenRepository authTokenRepository;

    @Autowired
    private AuthTokenMapper authTokenMapper;

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenServiceImpl.class);

    @Override
    public Map<String, String> authenticateUser(String userName, String password) {
        logger.info("Attempting to authenticate user: {}", userName);

        Users user = userRepository.findByUserNameIgnoreCase(userName)
                .orElseThrow(() -> {
                    logger.error("User not found for username: {}", userName);
                    return new BusinessValidationException("User not found");
                });

        if (!password.equals(user.getPassword())) {
            logger.error("Invalid password for user: {}", userName);
            throw new BusinessValidationException("Invalid password");
        }

        logger.info("Authentication successful for user: {}", userName);
        return generateToken(user);
    }

    private Map<String, String> generateToken(Users user) {
        logger.info("Generating new token for user: {}", user.getUserName());

        List<AuthTokens> existingTokens = authTokenRepository.findByUserIdAndDbstatus(user.getId(), Status.ACTIVE);
        logger.info("Deactivating {} existing tokens for user: {}", existingTokens.size(), user.getUserName());

        for (AuthTokens token : existingTokens) {
            token.setDbstatus(Status.INACTIVE);
        }
        authTokenRepository.saveAll(existingTokens);

        String token = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        AuthTokens authTokens = authTokenMapper.toEntity(user, token, now);

        authTokenRepository.save(authTokens);
        logger.info("New token generated and saved for user: {}", user.getUserName());

        Map<String, String> response = new HashMap<>();
        response.put("token", token);

        return response;
    }

    public boolean isUserValid(String token) {
        logger.info("Validating token: {}", token);

        AuthTokens authToken = authTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    logger.error("Invalid token: {}", token);
                    return new BusinessValidationException("Invalid token");
                });

        if (authToken.getDbstatus() == Status.INACTIVE || LocalDate.now().isAfter(authToken.getExpireDate())) {
            logger.error("Token is either inactive or expired: {}", token);
            throw new BusinessValidationException("Token is a expired");
        }

        Users user = userRepository.findById(authToken.getUserId())
                .orElseThrow(() -> {
                    logger.error("User not found for token: {}", token);
                    return new BusinessValidationException("User not found");
                });

        logger.info("Token validation successful for user: {}", user.getUserName());
        return user != null;
    }

    public String validToken(String token) {
        logger.info("Refreshing token: {}", token);

        AuthTokens authToken = authTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    logger.error("Invalid token for refresh: {}", token);
                    return new BusinessValidationException("Invalid token");
                });

        if (authToken.getDbstatus() == Status.INACTIVE || LocalDate.now().isAfter(authToken.getExpireDate())) {
            logger.error("Cannot refresh: Token is inactive or expired: {}", token);
            throw new BusinessValidationException("Cannot refresh: Token is  a expired");
        }

        Users user = userRepository.findById(authToken.getUserId())
                .orElseThrow(() -> {
                    logger.error("User not found for token refresh: {}", token);
                    return new BusinessValidationException("User not found");
                });

        logger.info("Token refreshed successfully for user: {}", user.getUserName());
        return generateToken(user).get("token");
    }
}
