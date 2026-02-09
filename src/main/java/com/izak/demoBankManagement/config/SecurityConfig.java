package com.izak.demoBankManagement.config;

import com.izak.demoBankManagement.security.JwtAuthenticationEntryPoint;
import com.izak.demoBankManagement.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()

                        // Swagger/OpenAPI endpoints
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/swagger-resources/**", "/webjars/**").permitAll()

                        // Admin-only endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER")

                        // Branch management - Admin/Employee only
                        .requestMatchers("/api/branches/**").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER")

                        // Customer management
                        .requestMatchers(HttpMethod.POST, "/api/customers").permitAll() // Allow registration
                        .requestMatchers(HttpMethod.GET, "/api/customers/{id}").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "CUSTOMER")
                        .requestMatchers(HttpMethod.PUT, "/api/customers/{id}").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "CUSTOMER")
                        .requestMatchers("/api/customers/**").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER")

                        // ============================================
                        // ACCOUNT MANAGEMENT - Updated for new roles
                        // ============================================
                        .requestMatchers(HttpMethod.POST, "/api/accounts").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER")
                        .requestMatchers("/api/accounts/**").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "CUSTOMER")

                        // ============================================
                        // TRANSACTION MANAGEMENT - Updated for new roles
                        // ============================================
                        .requestMatchers("/api/transactions/**").hasAnyRole("ADMIN", "BRANCH_MANAGER", "CUSTOMER")

                        // ============================================
                        // DPS MANAGEMENT - Updated for new roles
                        // ============================================
                        // DPS list and search endpoints - Admin and Branch Manager only
                        .requestMatchers(HttpMethod.POST, "/api/dps").hasAnyRole("ADMIN", "BRANCH_MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/dps").hasAnyRole("ADMIN", "BRANCH_MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/dps/status/*").hasAnyRole("ADMIN", "BRANCH_MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/dps/branch/*").hasAnyRole("ADMIN", "BRANCH_MANAGER")

                        // Individual DPS operations - includes customers
                        .requestMatchers("/api/dps/**").hasAnyRole("ADMIN", "BRANCH_MANAGER", "CUSTOMER")

                        // ============================================
                        // CARD MANAGEMENT - Updated for new roles
                        // ============================================
                        // Card issuance - Admin, Branch Manager, Card Officer
                        .requestMatchers(HttpMethod.POST, "/api/cards").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "CARD_OFFICER")

                        // Admin/Manager/Card Officer only operations
                        .requestMatchers(HttpMethod.GET, "/api/cards/expiring-soon").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "CARD_OFFICER")
                        .requestMatchers(HttpMethod.PATCH, "/api/cards/*/unblock").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "CARD_OFFICER")
                        .requestMatchers(HttpMethod.PATCH, "/api/cards/*/limit").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "CARD_OFFICER")
                        .requestMatchers(HttpMethod.DELETE, "/api/cards/*").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "CARD_OFFICER")

                        // General card operations - includes customers
                        .requestMatchers("/api/cards/**").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "CARD_OFFICER", "CUSTOMER")

                        // ============================================
                        // LOAN MANAGEMENT - Updated for new roles
                        // ============================================

                        // Loan application - Admin, Branch Manager, Loan Officer, Customer
                        .requestMatchers(HttpMethod.POST, "/api/loans/apply").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "LOAN_OFFICER", "CUSTOMER")

                        // Customer-specific endpoints
                        .requestMatchers(HttpMethod.GET, "/api/loans/my-loans").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/loans/*/statement").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "LOAN_OFFICER", "CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/loans/*/repay").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "LOAN_OFFICER", "CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/loans/*/foreclose").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "LOAN_OFFICER", "CUSTOMER")

                        // Loan approval, rejection, disbursement - Admin, Branch Manager, Loan Officer
                        .requestMatchers(HttpMethod.GET, "/api/loans/pending-approval").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "LOAN_OFFICER")
                        .requestMatchers(HttpMethod.GET, "/api/loans/customer/*").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "LOAN_OFFICER")
                        .requestMatchers(HttpMethod.POST, "/api/loans/search").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "LOAN_OFFICER")
                        .requestMatchers(HttpMethod.POST, "/api/loans/*/approve").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "LOAN_OFFICER")
                        .requestMatchers(HttpMethod.POST, "/api/loans/*/reject").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "LOAN_OFFICER")
                        .requestMatchers(HttpMethod.POST, "/api/loans/*/disburse").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "LOAN_OFFICER")

                        // Admin-only loan endpoints
                        .requestMatchers(HttpMethod.POST, "/api/loans/mark-defaults").hasRole("ADMIN")

                        // General loan endpoint (view specific loan)
                        .requestMatchers(HttpMethod.GET, "/api/loans/*").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "LOAN_OFFICER", "CUSTOMER")

                        // Get all loans - Admin, Branch Manager, Loan Officer
                        .requestMatchers(HttpMethod.GET, "/api/loans").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "LOAN_OFFICER")

                        // Any other request must be authenticated
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}


//package com.izak.demoBankManagement.config;
//
//import com.izak.demoBankManagement.security.JwtAuthenticationEntryPoint;
//import com.izak.demoBankManagement.security.JwtAuthenticationFilter;
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpMethod;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.AuthenticationProvider;
//import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
//import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
//import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.CorsConfigurationSource;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//
//import java.util.Arrays;
//import java.util.List;
//
//@Configuration
//@EnableWebSecurity
//@EnableMethodSecurity(prePostEnabled = true)
//@RequiredArgsConstructor
//public class SecurityConfig {
//
//    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
//    private final JwtAuthenticationFilter jwtAuthenticationFilter;
//    private final UserDetailsService userDetailsService;
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .csrf(csrf -> csrf.disable())
//                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
//                .authorizeHttpRequests(auth -> auth
//                        // Public endpoints
//                        .requestMatchers("/api/auth/**").permitAll()
//                        .requestMatchers("/api/public/**").permitAll()
//
//                        // Swagger/OpenAPI endpoints
//                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
//                        .requestMatchers("/swagger-resources/**", "/webjars/**").permitAll()
//
//                        // Admin-only endpoints
//                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
//                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER")
//
//                        // Branch management - Admin/Employee only
//                        .requestMatchers("/api/branches/**").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER")
//
//                        // Customer management
//                        .requestMatchers(HttpMethod.POST, "/api/customers").permitAll() // Allow registration
//                        .requestMatchers(HttpMethod.GET, "/api/customers/{id}").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "CUSTOMER")
//                        .requestMatchers(HttpMethod.PUT, "/api/customers/{id}").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "CUSTOMER")
//                        .requestMatchers("/api/customers/**").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER")
//
//                        // ============================================
//                        // ACCOUNT MANAGEMENT - Updated for new roles
//                        // ============================================
//                        .requestMatchers(HttpMethod.POST, "/api/accounts").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER")
//                        .requestMatchers("/api/accounts/**").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "CUSTOMER")
//
//                        // Transaction management
//                        .requestMatchers("/api/transactions/**").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "CUSTOMER")
//
//                        // DPS management
//                        .requestMatchers("/api/dps/**").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "CUSTOMER")
//
//                        // ============================================
//                        // CARD MANAGEMENT - Updated for new roles
//                        // ============================================
//                        // Card issuance - Admin, Branch Manager, Card Officer
//                        .requestMatchers(HttpMethod.POST, "/api/cards").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "CARD_OFFICER")
//
//                        // Admin/Manager/Card Officer only operations
//                        .requestMatchers(HttpMethod.GET, "/api/cards/expiring-soon").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "CARD_OFFICER")
//                        .requestMatchers(HttpMethod.PATCH, "/api/cards/*/unblock").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "CARD_OFFICER")
//                        .requestMatchers(HttpMethod.PATCH, "/api/cards/*/limit").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "CARD_OFFICER")
//                        .requestMatchers(HttpMethod.DELETE, "/api/cards/*").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "CARD_OFFICER")
//
//                        // General card operations - includes customers
//                        .requestMatchers("/api/cards/**").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "CARD_OFFICER", "CUSTOMER")
//
//                        // ============================================
//                        // LOAN MANAGEMENT - Updated for new roles
//                        // ============================================
//
//                        // Loan application - Admin, Branch Manager, Loan Officer, Customer
//                        .requestMatchers(HttpMethod.POST, "/api/loans/apply").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "LOAN_OFFICER", "CUSTOMER")
//
//                        // Customer-specific endpoints
//                        .requestMatchers(HttpMethod.GET, "/api/loans/my-loans").hasRole("CUSTOMER")
//                        .requestMatchers(HttpMethod.GET, "/api/loans/*/statement").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "LOAN_OFFICER", "CUSTOMER")
//                        .requestMatchers(HttpMethod.POST, "/api/loans/*/repay").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "LOAN_OFFICER", "CUSTOMER")
//                        .requestMatchers(HttpMethod.POST, "/api/loans/*/foreclose").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "LOAN_OFFICER", "CUSTOMER")
//
//                        // Loan approval, rejection, disbursement - Admin, Branch Manager, Loan Officer
//                        .requestMatchers(HttpMethod.GET, "/api/loans/pending-approval").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "LOAN_OFFICER")
//                        .requestMatchers(HttpMethod.GET, "/api/loans/customer/*").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "LOAN_OFFICER")
//                        .requestMatchers(HttpMethod.POST, "/api/loans/search").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "LOAN_OFFICER")
//                        .requestMatchers(HttpMethod.POST, "/api/loans/*/approve").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "LOAN_OFFICER")
//                        .requestMatchers(HttpMethod.POST, "/api/loans/*/reject").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "LOAN_OFFICER")
//                        .requestMatchers(HttpMethod.POST, "/api/loans/*/disburse").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "LOAN_OFFICER")
//
//                        // Admin-only loan endpoints
//                        .requestMatchers(HttpMethod.POST, "/api/loans/mark-defaults").hasRole("ADMIN")
//
//                        // General loan endpoint (view specific loan)
//                        .requestMatchers(HttpMethod.GET, "/api/loans/*").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "LOAN_OFFICER", "CUSTOMER")
//
//                        // Get all loans - Admin, Branch Manager, Loan Officer
//                        .requestMatchers(HttpMethod.GET, "/api/loans").hasAnyRole("ADMIN", "EMPLOYEE", "BRANCH_MANAGER", "LOAN_OFFICER")
//
//                        // Any other request must be authenticated
//                        .anyRequest().authenticated()
//                )
//                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
//                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                .authenticationProvider(authenticationProvider())
//                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
//
//        return http.build();
//    }
//
//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration configuration = new CorsConfiguration();
//        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
//        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
//        configuration.setAllowedHeaders(Arrays.asList("*"));
//        configuration.setExposedHeaders(Arrays.asList("Authorization"));
//        configuration.setAllowCredentials(true);
//        configuration.setMaxAge(3600L);
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", configuration);
//        return source;
//    }
//
//    @Bean
//    public AuthenticationProvider authenticationProvider() {
//        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
//        authProvider.setUserDetailsService(userDetailsService);
//        authProvider.setPasswordEncoder(passwordEncoder());
//        return authProvider;
//    }
//
//    @Bean
//    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
//        return config.getAuthenticationManager();
//    }
//
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
//}
//
//
//
////package com.izak.demoBankManagement.config;
////
////import com.izak.demoBankManagement.security.JwtAuthenticationEntryPoint;
////import com.izak.demoBankManagement.security.JwtAuthenticationFilter;
////import lombok.RequiredArgsConstructor;
////import org.springframework.context.annotation.Bean;
////import org.springframework.context.annotation.Configuration;
////import org.springframework.http.HttpMethod;
////import org.springframework.security.authentication.AuthenticationManager;
////import org.springframework.security.authentication.AuthenticationProvider;
////import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
////import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
////import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
////import org.springframework.security.config.annotation.web.builders.HttpSecurity;
////import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
////import org.springframework.security.config.http.SessionCreationPolicy;
////import org.springframework.security.core.userdetails.UserDetailsService;
////import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
////import org.springframework.security.crypto.password.PasswordEncoder;
////import org.springframework.security.web.SecurityFilterChain;
////import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
////import org.springframework.web.cors.CorsConfiguration;
////import org.springframework.web.cors.CorsConfigurationSource;
////import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
////
////import java.util.Arrays;
////import java.util.List;
////
////@Configuration
////@EnableWebSecurity
////@EnableMethodSecurity(prePostEnabled = true)
////@RequiredArgsConstructor
////public class SecurityConfig {
////
////    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
////    private final JwtAuthenticationFilter jwtAuthenticationFilter;
////    private final UserDetailsService userDetailsService;
////
////    @Bean
////    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
////        http
////                .csrf(csrf -> csrf.disable())
////                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
////                .authorizeHttpRequests(auth -> auth
////                        // Public endpoints
////                        .requestMatchers("/api/auth/**").permitAll()
////                        .requestMatchers("/api/public/**").permitAll()
////
////                        // Swagger/OpenAPI endpoints
////                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
////                        .requestMatchers("/swagger-resources/**", "/webjars/**").permitAll()
////
////                        // Admin-only endpoints
////                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
////                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasAnyRole("ADMIN", "EMPLOYEE")
////
////                        // Branch management - Admin/Employee only
////                        .requestMatchers("/api/branches/**").hasAnyRole("ADMIN", "EMPLOYEE")
////
////                        // Customer management
////                        .requestMatchers(HttpMethod.POST, "/api/customers").permitAll() // Allow registration
////                        .requestMatchers(HttpMethod.GET, "/api/customers/{id}").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
////                        .requestMatchers(HttpMethod.PUT, "/api/customers/{id}").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
////                        .requestMatchers("/api/customers/**").hasAnyRole("ADMIN", "EMPLOYEE")
////
////                        // Account management
////                        .requestMatchers(HttpMethod.POST, "/api/accounts").hasAnyRole("ADMIN", "EMPLOYEE")
////                        .requestMatchers("/api/accounts/**").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
////
////                        // Transaction management
////                        .requestMatchers("/api/transactions/**").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
////
////                        // DPS management
////                        .requestMatchers("/api/dps/**").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
////
////                        // Card management endpoints
////                        .requestMatchers(HttpMethod.POST, "/api/cards").hasAnyRole("ADMIN", "EMPLOYEE")
////                        .requestMatchers(HttpMethod.GET, "/api/cards/expiring-soon").hasAnyRole("ADMIN", "EMPLOYEE")
////                        .requestMatchers(HttpMethod.PATCH, "/api/cards/*/unblock").hasAnyRole("ADMIN", "EMPLOYEE")
////                        .requestMatchers(HttpMethod.PATCH, "/api/cards/*/limit").hasAnyRole("ADMIN", "EMPLOYEE")
////                        .requestMatchers(HttpMethod.DELETE, "/api/cards/*").hasAnyRole("ADMIN", "EMPLOYEE")
////                        .requestMatchers("/api/cards/**").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
////
////                        // ============================================
////                        // LOAN MANAGEMENT ENDPOINTS - NEW
////                        // ============================================
////
////                        // Customer loan endpoints (apply, view own loans, repay, foreclose)
////                        .requestMatchers(HttpMethod.POST, "/api/loans/apply").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
////                        .requestMatchers(HttpMethod.GET, "/api/loans/my-loans").hasRole("CUSTOMER")
////                        .requestMatchers(HttpMethod.GET, "/api/loans/*/statement").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
////                        .requestMatchers(HttpMethod.POST, "/api/loans/*/repay").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
////                        .requestMatchers(HttpMethod.POST, "/api/loans/*/foreclose").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
////
////                        // Employee/Admin loan endpoints (approval, disbursement, search)
////                        .requestMatchers(HttpMethod.GET, "/api/loans/pending-approval").hasAnyRole("ADMIN", "EMPLOYEE")
////                        .requestMatchers(HttpMethod.GET, "/api/loans/customer/*").hasAnyRole("ADMIN", "EMPLOYEE")
////                        .requestMatchers(HttpMethod.POST, "/api/loans/search").hasAnyRole("ADMIN", "EMPLOYEE")
////                        .requestMatchers(HttpMethod.POST, "/api/loans/*/approve").hasAnyRole("ADMIN", "EMPLOYEE")
////                        .requestMatchers(HttpMethod.POST, "/api/loans/*/reject").hasAnyRole("ADMIN", "EMPLOYEE")
////                        .requestMatchers(HttpMethod.POST, "/api/loans/*/disburse").hasAnyRole("ADMIN", "EMPLOYEE")
////
////                        // Admin-only loan endpoints
////                        .requestMatchers(HttpMethod.POST, "/api/loans/mark-defaults").hasRole("ADMIN")
////
////                        // General loan endpoint (view specific loan)
////                        .requestMatchers(HttpMethod.GET, "/api/loans/*").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
////
////                        // Get all loans (Admin/Employee only)
////                        .requestMatchers(HttpMethod.GET, "/api/loans").hasAnyRole("ADMIN", "EMPLOYEE")
////
////                        // Any other request must be authenticated
////                        .anyRequest().authenticated()
////                )
////                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
////                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
////                .authenticationProvider(authenticationProvider())
////                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
////
////        return http.build();
////    }
////
////    @Bean
////    public CorsConfigurationSource corsConfigurationSource() {
////        CorsConfiguration configuration = new CorsConfiguration();
////        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
////        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
////        configuration.setAllowedHeaders(Arrays.asList("*"));
////        configuration.setExposedHeaders(Arrays.asList("Authorization"));
////        configuration.setAllowCredentials(true);
////        configuration.setMaxAge(3600L);
////
////        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
////        source.registerCorsConfiguration("/**", configuration);
////        return source;
////    }
////
////    @Bean
////    public AuthenticationProvider authenticationProvider() {
////        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
////        authProvider.setUserDetailsService(userDetailsService);
////        authProvider.setPasswordEncoder(passwordEncoder());
////        return authProvider;
////    }
////
////    @Bean
////    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
////        return config.getAuthenticationManager();
////    }
////
////    @Bean
////    public PasswordEncoder passwordEncoder() {
////        return new BCryptPasswordEncoder();
////    }
////}
////
//////package com.izak.demoBankManagement.config;
//////
//////import com.izak.demoBankManagement.security.JwtAuthenticationEntryPoint;
//////import com.izak.demoBankManagement.security.JwtAuthenticationFilter;
//////import lombok.RequiredArgsConstructor;
//////import org.springframework.context.annotation.Bean;
//////import org.springframework.context.annotation.Configuration;
//////import org.springframework.http.HttpMethod;
//////import org.springframework.security.authentication.AuthenticationManager;
//////import org.springframework.security.authentication.AuthenticationProvider;
//////import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
//////import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
//////import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
//////import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//////import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//////import org.springframework.security.config.http.SessionCreationPolicy;
//////import org.springframework.security.core.userdetails.UserDetailsService;
//////import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//////import org.springframework.security.crypto.password.PasswordEncoder;
//////import org.springframework.security.web.SecurityFilterChain;
//////import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//////import org.springframework.web.cors.CorsConfiguration;
//////import org.springframework.web.cors.CorsConfigurationSource;
//////import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//////
//////import java.util.Arrays;
//////import java.util.List;
//////
//////@Configuration
//////@EnableWebSecurity
//////@EnableMethodSecurity(prePostEnabled = true)
//////@RequiredArgsConstructor
//////public class SecurityConfig {
//////
//////    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
//////    private final JwtAuthenticationFilter jwtAuthenticationFilter;
//////    private final UserDetailsService userDetailsService;
//////
//////    @Bean
//////    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//////        http
//////                .csrf(csrf -> csrf.disable())
//////                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
//////                .authorizeHttpRequests(auth -> auth
//////                        // Public endpoints
//////                        .requestMatchers("/api/auth/**").permitAll()
//////                        .requestMatchers("/api/public/**").permitAll()
//////
//////                        // Swagger/OpenAPI endpoints
//////                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
//////                        .requestMatchers("/swagger-resources/**", "/webjars/**").permitAll()
//////
//////                        // Admin-only endpoints
//////                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
//////                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasAnyRole("ADMIN", "EMPLOYEE")
//////
//////                        // Branch management - Admin/Employee only
//////                        .requestMatchers("/api/branches/**").hasAnyRole("ADMIN", "EMPLOYEE")
//////
//////                        // Customer management
//////                        .requestMatchers(HttpMethod.POST, "/api/customers").permitAll() // Allow registration
//////                        .requestMatchers(HttpMethod.GET, "/api/customers/{id}").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
//////                        .requestMatchers(HttpMethod.PUT, "/api/customers/{id}").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
//////                        .requestMatchers("/api/customers/**").hasAnyRole("ADMIN", "EMPLOYEE")
//////
//////                        // Account management
//////                        .requestMatchers(HttpMethod.POST, "/api/accounts").hasAnyRole("ADMIN", "EMPLOYEE")
//////                        .requestMatchers("/api/accounts/**").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
//////
//////                        // Transaction management
//////                        .requestMatchers("/api/transactions/**").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
//////
//////                        // DPS management
//////                        .requestMatchers("/api/dps/**").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
//////
//////                        // Card management endpoints
//////                        .requestMatchers(HttpMethod.POST, "/api/cards").hasAnyRole("ADMIN", "EMPLOYEE")
//////                        .requestMatchers(HttpMethod.GET, "/api/cards/expiring-soon").hasAnyRole("ADMIN", "EMPLOYEE")
//////                        .requestMatchers(HttpMethod.PATCH, "/api/cards/*/unblock").hasAnyRole("ADMIN", "EMPLOYEE")
//////                        .requestMatchers(HttpMethod.PATCH, "/api/cards/*/limit").hasAnyRole("ADMIN", "EMPLOYEE")
//////                        .requestMatchers(HttpMethod.DELETE, "/api/cards/*").hasAnyRole("ADMIN", "EMPLOYEE")
//////                        .requestMatchers("/api/cards/**").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
//////
//////                        // ============================================
//////                        // LOAN MANAGEMENT ENDPOINTS - NEW
//////                        // ============================================
//////
//////                        // Customer loan endpoints (apply, view own loans, repay, foreclose)
//////                        .requestMatchers(HttpMethod.POST, "/api/loans/apply").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
//////                        .requestMatchers(HttpMethod.GET, "/api/loans/my-loans").hasRole("CUSTOMER")
//////                        .requestMatchers(HttpMethod.GET, "/api/loans/*/statement").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
//////                        .requestMatchers(HttpMethod.POST, "/api/loans/*/repay").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
//////                        .requestMatchers(HttpMethod.POST, "/api/loans/*/foreclose").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
//////
//////                        // Employee/Admin loan endpoints (approval, disbursement, search)
//////                        .requestMatchers(HttpMethod.GET, "/api/loans/pending-approval").hasAnyRole("ADMIN", "EMPLOYEE")
//////                        .requestMatchers(HttpMethod.GET, "/api/loans/customer/*").hasAnyRole("ADMIN", "EMPLOYEE")
//////                        .requestMatchers(HttpMethod.POST, "/api/loans/search").hasAnyRole("ADMIN", "EMPLOYEE")
//////                        .requestMatchers(HttpMethod.POST, "/api/loans/*/approve").hasAnyRole("ADMIN", "EMPLOYEE")
//////                        .requestMatchers(HttpMethod.POST, "/api/loans/*/reject").hasAnyRole("ADMIN", "EMPLOYEE")
//////                        .requestMatchers(HttpMethod.POST, "/api/loans/*/disburse").hasAnyRole("ADMIN", "EMPLOYEE")
//////
//////                        // Admin-only loan endpoints
//////                        .requestMatchers(HttpMethod.POST, "/api/loans/mark-defaults").hasRole("ADMIN")
//////
//////                        // General loan endpoint (view specific loan)
//////                        .requestMatchers(HttpMethod.GET, "/api/loans/*").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
//////
//////                        // Any other request must be authenticated
//////                        .anyRequest().authenticated()
//////                )
//////                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
//////                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//////                .authenticationProvider(authenticationProvider())
//////                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
//////
//////        return http.build();
//////    }
//////
//////    @Bean
//////    public CorsConfigurationSource corsConfigurationSource() {
//////        CorsConfiguration configuration = new CorsConfiguration();
//////        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
//////        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
//////        configuration.setAllowedHeaders(Arrays.asList("*"));
//////        configuration.setExposedHeaders(Arrays.asList("Authorization"));
//////        configuration.setAllowCredentials(true);
//////        configuration.setMaxAge(3600L);
//////
//////        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//////        source.registerCorsConfiguration("/**", configuration);
//////        return source;
//////    }
//////
//////    @Bean
//////    public AuthenticationProvider authenticationProvider() {
//////        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
//////        authProvider.setUserDetailsService(userDetailsService);
//////        authProvider.setPasswordEncoder(passwordEncoder());
//////        return authProvider;
//////    }
//////
//////    @Bean
//////    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
//////        return config.getAuthenticationManager();
//////    }
//////
//////    @Bean
//////    public PasswordEncoder passwordEncoder() {
//////        return new BCryptPasswordEncoder();
//////    }
//////}
//////
//////
////////package com.izak.demoBankManagement.config;
////////
////////import com.izak.demoBankManagement.security.JwtAuthenticationEntryPoint;
////////import com.izak.demoBankManagement.security.JwtAuthenticationFilter;
////////import lombok.RequiredArgsConstructor;
////////import org.springframework.context.annotation.Bean;
////////import org.springframework.context.annotation.Configuration;
////////import org.springframework.http.HttpMethod;
////////import org.springframework.security.authentication.AuthenticationManager;
////////import org.springframework.security.authentication.AuthenticationProvider;
////////import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
////////import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
////////import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
////////import org.springframework.security.config.annotation.web.builders.HttpSecurity;
////////import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
////////import org.springframework.security.config.http.SessionCreationPolicy;
////////import org.springframework.security.core.userdetails.UserDetailsService;
////////import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
////////import org.springframework.security.crypto.password.PasswordEncoder;
////////import org.springframework.security.web.SecurityFilterChain;
////////import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
////////import org.springframework.web.cors.CorsConfiguration;
////////import org.springframework.web.cors.CorsConfigurationSource;
////////import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
////////
////////import java.util.Arrays;
////////import java.util.List;
////////
////////
////////@Configuration
////////@EnableWebSecurity
////////@EnableMethodSecurity(prePostEnabled = true)
////////@RequiredArgsConstructor
////////public class SecurityConfig {
////////
////////    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
////////    private final JwtAuthenticationFilter jwtAuthenticationFilter;
////////    private final UserDetailsService userDetailsService;
////////
////////    @Bean
////////    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
////////        http
////////                .csrf(csrf -> csrf.disable())
////////                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
////////                .authorizeHttpRequests(auth -> auth
////////                        // Public endpoints
////////                        .requestMatchers("/api/auth/**").permitAll()
////////                        .requestMatchers("/api/public/**").permitAll()
////////
////////                        // Swagger/OpenAPI endpoints
////////                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
////////                        .requestMatchers("/swagger-resources/**", "/webjars/**").permitAll()
////////
////////                        // Admin-only endpoints
////////                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
////////                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasAnyRole("ADMIN", "EMPLOYEE")
////////
////////                        // Branch management - Admin/Employee only
////////                        .requestMatchers("/api/branches/**").hasAnyRole("ADMIN", "EMPLOYEE")
////////
////////                        // Customer management
////////                        .requestMatchers(HttpMethod.POST, "/api/customers").permitAll() // Allow registration
////////                        .requestMatchers(HttpMethod.GET, "/api/customers/{id}").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
////////                        .requestMatchers(HttpMethod.PUT, "/api/customers/{id}").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
////////                        .requestMatchers("/api/customers/**").hasAnyRole("ADMIN", "EMPLOYEE")
////////
////////                        // Account management
////////                        .requestMatchers(HttpMethod.POST, "/api/accounts").hasAnyRole("ADMIN", "EMPLOYEE")
////////                        .requestMatchers("/api/accounts/**").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
////////
////////                        // Transaction management
////////                        .requestMatchers("/api/transactions/**").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
////////
////////                        // DPS management
////////                        .requestMatchers("/api/dps/**").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
////////
////////
////////                        // Card management endpoints
////////                        .requestMatchers(HttpMethod.POST, "/api/cards").hasAnyRole("ADMIN", "EMPLOYEE")
////////                        .requestMatchers(HttpMethod.GET, "/api/cards/expiring-soon").hasAnyRole("ADMIN", "EMPLOYEE")
////////                        .requestMatchers(HttpMethod.PATCH, "/api/cards/*/unblock").hasAnyRole("ADMIN", "EMPLOYEE")
////////                        .requestMatchers(HttpMethod.PATCH, "/api/cards/*/limit").hasAnyRole("ADMIN", "EMPLOYEE")
////////                        .requestMatchers(HttpMethod.DELETE, "/api/cards/*").hasAnyRole("ADMIN", "EMPLOYEE")
////////                        .requestMatchers("/api/cards/**").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
////////
////////
////////                                // Any other request must be authenticated
////////                        .anyRequest().authenticated()
////////                )
////////                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
////////                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
////////                .authenticationProvider(authenticationProvider())
////////                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
////////
////////        return http.build();
////////    }
////////
////////    @Bean
////////    public CorsConfigurationSource corsConfigurationSource() {
////////        CorsConfiguration configuration = new CorsConfiguration();
////////        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
////////        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
////////        configuration.setAllowedHeaders(Arrays.asList("*"));
////////        configuration.setExposedHeaders(Arrays.asList("Authorization"));
////////        configuration.setAllowCredentials(true);
////////        configuration.setMaxAge(3600L);
////////
////////        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
////////        source.registerCorsConfiguration("/**", configuration);
////////        return source;
////////    }
////////
////////    @Bean
////////    public AuthenticationProvider authenticationProvider() {
////////        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
////////        authProvider.setUserDetailsService(userDetailsService);
////////        authProvider.setPasswordEncoder(passwordEncoder());
////////        return authProvider;
////////    }
////////
////////    @Bean
////////    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
////////        return config.getAuthenticationManager();
////////    }
////////
////////    @Bean
////////    public PasswordEncoder passwordEncoder() {
////////        return new BCryptPasswordEncoder();
////////    }
////////}
//////////public class SecurityConfig {
//////////
//////////    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
//////////    private final JwtAuthenticationFilter jwtAuthenticationFilter;
//////////    private final UserDetailsService userDetailsService;
//////////
//////////    @Bean
//////////    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//////////        http
//////////                .csrf(csrf -> csrf.disable())
//////////                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
//////////                .authorizeHttpRequests(auth -> auth
//////////                        // Public endpoints
//////////                        .requestMatchers("/api/auth/**").permitAll()
//////////                        .requestMatchers("/api/public/**").permitAll()
//////////
//////////                        // Admin-only endpoints
//////////                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
//////////                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasAnyRole("ADMIN", "EMPLOYEE")
//////////
//////////                        // Branch management - Admin/Employee only
//////////                        .requestMatchers("/api/branches/**").hasAnyRole("ADMIN", "EMPLOYEE")
//////////
//////////                        // Customer management
//////////                        .requestMatchers(HttpMethod.POST, "/api/customers").permitAll() // Allow registration
//////////                        .requestMatchers(HttpMethod.GET, "/api/customers/{id}").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
//////////                        .requestMatchers(HttpMethod.PUT, "/api/customers/{id}").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
//////////                        .requestMatchers("/api/customers/**").hasAnyRole("ADMIN", "EMPLOYEE")
//////////
//////////                        // Account management
//////////                        .requestMatchers(HttpMethod.POST, "/api/accounts").hasAnyRole("ADMIN", "EMPLOYEE")
//////////                        .requestMatchers("/api/accounts/**").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
//////////
//////////                        // Transaction management
//////////                        .requestMatchers("/api/transactions/**").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
//////////
//////////                        // DPS management
//////////                        .requestMatchers("/api/dps/**").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
//////////
//////////                        // Any other request must be authenticated
//////////                        .anyRequest().authenticated()
//////////                )
//////////                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
//////////                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//////////                .authenticationProvider(authenticationProvider())
//////////                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
//////////
//////////        return http.build();
//////////    }
//////////
//////////    @Bean
//////////    public CorsConfigurationSource corsConfigurationSource() {
//////////        CorsConfiguration configuration = new CorsConfiguration();
//////////        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
//////////        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
//////////        configuration.setAllowedHeaders(Arrays.asList("*"));
//////////        configuration.setExposedHeaders(Arrays.asList("Authorization"));
//////////        configuration.setAllowCredentials(true);
//////////        configuration.setMaxAge(3600L);
//////////
//////////        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//////////        source.registerCorsConfiguration("/**", configuration);
//////////        return source;
//////////    }
//////////
//////////    @Bean
//////////    public AuthenticationProvider authenticationProvider() {
//////////        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
//////////        authProvider.setUserDetailsService(userDetailsService);
//////////        authProvider.setPasswordEncoder(passwordEncoder());
//////////        return authProvider;
//////////    }
//////////
//////////    @Bean
//////////    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
//////////        return config.getAuthenticationManager();
//////////    }
//////////
//////////    @Bean
//////////    public PasswordEncoder passwordEncoder() {
//////////        return new BCryptPasswordEncoder();
//////////    }
//////////}