package com.izak.demoBankManagement.service;


import com.izak.demoBankManagement.dto.*;
import com.izak.demoBankManagement.entity.Customer;
import com.izak.demoBankManagement.entity.User;
import com.izak.demoBankManagement.exception.InvalidCredentialsException;
import com.izak.demoBankManagement.repository.CustomerRepository;
import com.izak.demoBankManagement.repository.UserRepository;
import com.izak.demoBankManagement.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    private final PasswordEncoder passwordEncoder;

//    @Transactional
//    public AuthResponseDTO login(LoginRequestDTO request) {
//        try {
//            Authentication authentication = authenticationManager.authenticate(
//                    new UsernamePasswordAuthenticationToken(
//                            request.getUsername(),
//                            request.getPassword()
//                    )
//            );
//
//            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
//            User user = userRepository.findByUsername(userDetails.getUsername())
//                    .orElseThrow(() -> new InvalidCredentialsException("User not found"));
//
//            Map<String, Object> claims = new HashMap<>();
//            claims.put("role", user.getRole().name());
//            claims.put("userId", user.getId());
//
//            // If user is a customer, add customer ID to token
//            if (user.getRole() == User.Role.CUSTOMER) {
//                Customer customer = customerRepository.findByUserId(user.getId())
//                        .orElseThrow(() -> new InvalidCredentialsException("Customer profile not found"));
//                claims.put("customerId", customer.getCustomerId());
//            }
//
//            String token = jwtUtil.generateToken(claims, userDetails);
//
//            AuthResponseDTO response = new AuthResponseDTO();
//            response.setToken(token);
//            response.setUsername(user.getUsername());
//            response.setRole(user.getRole().name());
//            response.setEmail(user.getEmail());
//            response.setId(user.getId());
//
//            if (user.getRole() == User.Role.CUSTOMER) {
//                Customer customer = customerRepository.findByUserId(user.getId()).orElse(null);
//                if (customer != null) {
//                    response.setCustomerId(customer.getCustomerId());
//                    response.setFullName(customer.getFirstName() + " " + customer.getLastName());
//                }
//            }
//
//            log.info("User {} logged in successfully", user.getUsername());
//            return response;
//
//        } catch (Exception e) {
//            log.error("Login failed for user: {}", request.getUsername(), e);
//            throw new InvalidCredentialsException("Invalid username or password");
//        }
//    }


    @Transactional
    public AuthResponseDTO login(LoginRequestDTO request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new InvalidCredentialsException("User not found"));

            Map<String, Object> claims = new HashMap<>();
            claims.put("role", user.getRole().name());
            claims.put("userId", user.getId());

            // Add branchId to claims if user has a branch assigned
            // Only for branch-specific roles (not ADMIN or CUSTOMER)
            if (user.getBranch() != null &&
                    user.getRole() != User.Role.ADMIN &&
                    user.getRole() != User.Role.CUSTOMER) {
                claims.put("branchId", user.getBranch().getId());
                log.debug("Added branchId {} to JWT claims for user {}",
                        user.getBranch().getId(), user.getUsername());
            }

            // If user is a customer, add customer ID to token
            if (user.getRole() == User.Role.CUSTOMER) {
                Customer customer = customerRepository.findByUserId(user.getId())
                        .orElseThrow(() -> new InvalidCredentialsException("Customer profile not found"));
                claims.put("customerId", customer.getCustomerId());
            }

            String token = jwtUtil.generateToken(claims, userDetails);

            AuthResponseDTO response = new AuthResponseDTO();
            response.setToken(token);
            response.setUsername(user.getUsername());
            response.setRole(user.getRole().name());
            response.setEmail(user.getEmail());
            response.setId(user.getId());

            if (user.getRole() == User.Role.CUSTOMER) {
                Customer customer = customerRepository.findByUserId(user.getId()).orElse(null);
                if (customer != null) {
                    response.setCustomerId(customer.getCustomerId());
                    response.setFullName(customer.getFirstName() + " " + customer.getLastName());
                }
            }

            log.info("User {} logged in successfully", user.getUsername());
            return response;

        } catch (Exception e) {
            log.error("Login failed for user: {}", request.getUsername(), e);
            throw new InvalidCredentialsException("Invalid username or password");
        }
    }



    @Transactional
    public CustomerResponseDTO registerCustomer(CustomerCreateRequestDTO request) {
        // Encode password before saving
//        request.setPassword(passwordEncoder.encode(request.getPassword()));
        return customerService.createCustomer(request);
    }

    public AuthResponseDTO refreshToken(RefreshTokenRequestDTO request) {
        String username = jwtUtil.extractUsername(request.getRefreshToken());
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (jwtUtil.validateToken(request.getRefreshToken(), userDetails)) {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new InvalidCredentialsException("User not found"));

            Map<String, Object> claims = new HashMap<>();
            claims.put("role", user.getRole().name());
            claims.put("userId", user.getId());

            if (user.getRole() == User.Role.CUSTOMER) {
                Customer customer = customerRepository.findByUserId(user.getId())
                        .orElseThrow(() -> new InvalidCredentialsException("Customer profile not found"));
                claims.put("customerId", customer.getCustomerId());
            }

            String newToken = jwtUtil.generateToken(claims, userDetails);

            AuthResponseDTO response = new AuthResponseDTO();
            response.setToken(newToken);
            response.setUsername(user.getUsername());
            response.setRole(user.getRole().name());
            response.setEmail(user.getEmail());
            response.setId(user.getId());

            return response;
        }

        throw new InvalidCredentialsException("Invalid refresh token");
    }

    public boolean validateToken(String token) {
        try {
            String username = jwtUtil.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            return jwtUtil.validateToken(token, userDetails);
        } catch (Exception e) {
            return false;
        }
    }
}