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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class AuthTokenServiceImpl implements AuthTokenService {

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthTokenRepository authTokenRepository;

    @Autowired
    private AuthTokenMapper authTokenMapper;

    @Override
    public Map<String, String> authenticateUser(String userName, String password) {
        logger.info("Attempting to authenticate user: {}", userName);

        Users user = userRepository.findByUserNameIgnoreCase(userName)
                .orElseThrow(() -> {
                    logger.error("User not found for username: {}", userName);
                    return new BusinessValidationException("User not found");
                });

        // Check if the user's account is active
        if (user.getDbstatus() != Status.ACTIVE) {
            logger.error("User account is inactive: {}", userName);
            throw new BusinessValidationException("User account is inactive");
        }

        // Validate password
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
        LocalDateTime expireDate = now.plusMinutes(2);

        AuthTokens authTokens = authTokenMapper.toEntity(user, token, expireDate);
        authTokenRepository.save(authTokens);
        logger.info("New token generated and saved for user: {}", user.getUserName());

        // Return the generated token
        Map<String, String> response = new HashMap<>();
        response.put("token", token);

        return response;
    }

    @Override
    public boolean isUserValid(String token) {
        logger.info("Validating token: {}", token);

        String strippedToken = token.startsWith("Bearer ") ? token.substring(7) : token;

        AuthTokens authTokens = authTokenRepository.findByToken(strippedToken)
                .orElseThrow(() -> {
                    logger.error("Invalid token: {}", strippedToken);
                    return new BusinessValidationException("Invalid token");
                });

        if (authTokens.getDbstatus() == Status.INACTIVE || LocalDateTime.now().isAfter(authTokens.getExpireDate())) {
            logger.error("Token is expired: {}", strippedToken);
            throw new BusinessValidationException("Token is expired");
        }

        Users user = userRepository.findById(authTokens.getUserId())
                .orElseThrow(() -> {
                    logger.error("User not found for token: {}", strippedToken);
                    return new BusinessValidationException("User not found");
                });

        if (user.getDbstatus() != Status.ACTIVE) {
            logger.error("User account is inactive: {}", user.getUserName());
            throw new BusinessValidationException("User account is inactive");
        }

        logger.info("Token validation successful for user: {}", user.getUserName());
        return true;
    }

    @Override
    public Integer validToken(String token, int userId) {
        logger.info("Validating token: {} for userId: {}", token, userId);

        String strippedToken = token.startsWith("Bearer ") ? token.substring(7) : token;

        validateToken(strippedToken, userId);

        return userId;
    }

    @Transactional
    public void validateToken(String authToken, int userId) {
        logger.info("Validating token: {} for userId: {}", authToken, userId);

        AuthTokens authTokens = authTokenRepository.findByTokenAndDbstatus(authToken, Status.ACTIVE)
                .orElseThrow(() -> {
                    logger.error("Token not found or inactive in repository: {}", authToken);
                    return new BusinessValidationException("Invalid or inactive token provided");
                });

        if (authTokens.getUserId() != userId) {
            logger.error("User ID does not match for token: {}, expected: {}, found: {}", authToken, userId, authTokens.getUserId());
            throw new BusinessValidationException("User ID does not match the token");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(authTokens.getExpireDate())) {
            logger.error("Token is expired. Current time: {}, Expiration time: {}", now, authTokens.getExpireDate());

            authTokens.setDbstatus(Status.INACTIVE);
            authTokenRepository.save(authTokens);

            throw new BusinessValidationException("Token is expired and now inactive");
        }

        logger.info("Token validation successful for userId: {}", userId);
    }
}
