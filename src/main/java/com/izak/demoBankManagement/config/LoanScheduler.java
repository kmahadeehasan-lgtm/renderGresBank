package com.izak.demoBankManagement.config;

import com.izak.demoBankManagement.service.LoanService;
import com.izak.demoBankManagement.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Scheduled tasks for loan management
 * FIXED: Now creates a system-level JWT token for scheduled tasks
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class LoanScheduler {

    private final LoanService loanService;
    private final JwtUtil jwtUtil;

    @Value("${scheduler.admin.username:admin}")
    private String schedulerAdminUsername;

    /**
     * Automatically mark defaulted loans
     * Runs daily at 2:00 AM
     * FIXED: Creates a system JWT token with ADMIN role for the scheduled task
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void markDefaultedLoans() {
        log.info("Starting scheduled task: Mark defaulted loans");
        try {
            // Create a system-level JWT token for the scheduler with ADMIN role
            String systemToken = createSystemToken();

            loanService.markDefaults(systemToken);
            log.info("Completed scheduled task: Mark defaulted loans");
        } catch (Exception e) {
            log.error("Error in scheduled task - Mark defaulted loans", e);
        }
    }

    /**
     * Optional: Send reminders for upcoming EMI payments
     * Runs daily at 9:00 AM
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendEMIReminders() {
        log.info("Starting scheduled task: Send EMI reminders");
        try {
            // Implementation for sending email/SMS reminders
            // This can be added based on requirements
            log.info("Completed scheduled task: Send EMI reminders");
        } catch (Exception e) {
            log.error("Error in scheduled task - Send EMI reminders", e);
        }
    }

    /**
     * Create a system-level JWT token for scheduled tasks
     * This token has ADMIN role and no branch restriction
     */
    private String createSystemToken() {
        java.util.Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("role", "ADMIN");
        claims.put("customerId", null);
        claims.put("branchId", null);

        // Create a mock UserDetails for system user
        org.springframework.security.core.userdetails.User systemUser =
                new org.springframework.security.core.userdetails.User(
                        schedulerAdminUsername,
                        "",
                        java.util.Collections.singletonList(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")
                        )
                );

        return jwtUtil.generateToken(claims, systemUser);
    }
}









//package com.izak.demoBankManagement.config;
//
//import com.izak.demoBankManagement.service.LoanService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.scheduling.annotation.EnableScheduling;
//import org.springframework.scheduling.annotation.Scheduled;
//
///**
// * Scheduled tasks for loan management
// */
//@Configuration
//@EnableScheduling
//@RequiredArgsConstructor
//@Slf4j
//public class LoanScheduler {
//
//    private final LoanService loanService;
//
//    /**
//     * Automatically mark defaulted loans
//     * Runs daily at 2:00 AM
//     */
//    @Scheduled(cron = "0 0 2 * * *")
//    public void markDefaultedLoans() {
//        log.info("Starting scheduled task: Mark defaulted loans");
//        try {
//            loanService.markDefaults();
//            log.info("Completed scheduled task: Mark defaulted loans");
//        } catch (Exception e) {
//            log.error("Error in scheduled task - Mark defaulted loans", e);
//        }
//    }
//
//    /**
//     * Optional: Send reminders for upcoming EMI payments
//     * Runs daily at 9:00 AM
//     */
//    @Scheduled(cron = "0 0 9 * * *")
//    public void sendEMIReminders() {
//        log.info("Starting scheduled task: Send EMI reminders");
//        // Implementation for sending email/SMS reminders
//        // This can be added based on requirements
//        log.info("Completed scheduled task: Send EMI reminders");
//    }
//}
//
