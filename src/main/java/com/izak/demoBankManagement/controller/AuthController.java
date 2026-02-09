package com.izak.demoBankManagement.controller;


import com.izak.demoBankManagement.dto.*;
import com.izak.demoBankManagement.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO request) {
        log.info("Login request for user: {}", request.getUsername());

        AuthResponseDTO response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> register(
            @Valid @RequestBody CustomerCreateRequestDTO request) {
        log.info("Registration request for: {} {}", request.getFirstName(), request.getLastName());

        CustomerResponseDTO response = authService.registerCustomer(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> refreshToken(
            @Valid @RequestBody RefreshTokenRequestDTO request) {
        log.info("Token refresh request");

        AuthResponseDTO response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        log.info("Logout request");
        // In a stateless JWT system, logout is typically handled client-side
        // by removing the token. You can implement token blacklisting if needed.
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<Boolean>> validateToken(
            @RequestHeader("Authorization") String token) {
        log.info("Token validation request");

        boolean isValid = authService.validateToken(token.substring(7));
        return ResponseEntity.ok(ApiResponse.success("Token validation result", isValid));
    }
}