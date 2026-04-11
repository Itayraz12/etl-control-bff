package com.example.controller;

import com.example.model.LoginRequest;
import com.example.model.LoginResult;
import com.example.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth Service", description = "Authentication operations")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Login using encrypted username and password")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        logger.info("Request arrived - POST /api/auth/login");
        try {
            LoginResult result = authService.login(request.username(), request.password());
            logger.info("Login successful for team '{}' with role '{}'", result.teamName(), result.userRole());
            return ResponseEntity.ok()
                .header("user-role", result.userRole())
                .body(Map.of("teamName", result.teamName()));
        } catch (SecurityException ex) {
            logger.warn("Login failed due to invalid credentials");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", ex.getMessage()));
        }
    }
}
