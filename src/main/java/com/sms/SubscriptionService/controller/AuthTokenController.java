package com.sms.SubscriptionService.controller;

import com.sms.SubscriptionService.exception.custom.BusinessValidationException;
import com.sms.SubscriptionService.service.AuthTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Tag(name = "Auth Token Controller")
public class AuthTokenController {

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenController.class);

    @Autowired
    private AuthTokenService authTokenService;

    @PostMapping()
    @Operation(
            summary = "Generate Authentication Token",
            description = "Generates an authentication token for the user based on provided username and password.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Authentication token generated successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized: Invalid username or password"),
                    @ApiResponse(responseCode = "400", description = "Bad Request: Missing or invalid parameters")
            }
    )
    public ResponseEntity<Map<String, String>> authenticateUser(@RequestParam String userName, @RequestParam String password) {
        logger.info("Login attempt for user: {}", userName);
        try {
            logger.info("Authenticating user: {}", userName);
            Map<String, String> tokenResponse = authTokenService.authenticateUser(userName, password);
            return new ResponseEntity<>(tokenResponse, HttpStatus.OK);
        } catch (BusinessValidationException e) {
            logger.error("Authentication failed for user: {}", userName);
            return new ResponseEntity<>(Map.of("error", e.getMessage()), HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            logger.error("Duplicates Names are not allowed: {}", e.getMessage());
            return new ResponseEntity<>(Map.of("error", "Duplicates Names are not allowed."), HttpStatus.BAD_REQUEST);
        }
    }
}