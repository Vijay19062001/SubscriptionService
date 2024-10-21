package com.sms.SubscriptionService.controller;

import com.sms.SubscriptionService.service.AuthTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Tag(name = "Auth Token Controller")
public class AuthTokenController {

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
    public ResponseEntity<String> login(@RequestParam String userName, @RequestParam String password) {
        String token = authTokenService.authenticateUser(userName, password);
        return ResponseEntity.ok(token);
    }


}

