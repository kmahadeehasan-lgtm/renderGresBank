package com.izak.demoBankManagement.config;

import com.izak.demoBankManagement.entity.Branch;
import com.izak.demoBankManagement.entity.User;
import com.izak.demoBankManagement.repository.BranchRepository;
import com.izak.demoBankManagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DataInitializer - Creates test data for the bank management system
 *
 * This component runs on application startup and creates:
 * - 1 ADMIN user (no branch assignment)
 * - 2 Branches (Main Branch and Downtown Branch)
 * - 2 BRANCH_MANAGER users (one per branch)
 * - 1 LOAN_OFFICER for Main Branch
 * - 1 CARD_OFFICER for Downtown Branch
 * - 2 CUSTOMER users (no branch assignment)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Only initialize if database is empty
        if (userRepository.count() == 0) {
            log.info("Initializing test data...");

            initializeBranches();
            initializeUsers();

            log.info("Test data initialization completed successfully!");
            printCredentials();
        } else {
            log.info("Database already contains data. Skipping initialization.");
        }
    }

    /**
     * Creates two branches: Main Branch and Downtown Branch
     */
    private void initializeBranches() {
        // Main Branch
        Branch mainBranch = new Branch();
        mainBranch.setBranchCode("MB001");
        mainBranch.setBranchName("Main Branch");
        mainBranch.setAddress("123 Main Street");
        mainBranch.setCity("Springfield");
        mainBranch.setState("IL");
        mainBranch.setZipCode("62701");
        mainBranch.setPhone("555-0100");
        mainBranch.setEmail("main@bank.com");
        mainBranch.setIfscCode("BANK0001001");
        mainBranch.setSwiftCode("BANKUS33");
        mainBranch.setStatus(Branch.BranchStatus.ACTIVE);
        mainBranch.setWorkingHours("9:00 AM - 5:00 PM");
        mainBranch.setIsMainBranch(true);
        branchRepository.save(mainBranch);
        log.info("Created Main Branch (ID: {})", mainBranch.getId());

        // Downtown Branch
        Branch downtownBranch = new Branch();
        downtownBranch.setBranchCode("DB001");
        downtownBranch.setBranchName("Downtown Branch");
        downtownBranch.setAddress("456 Downtown Avenue");
        downtownBranch.setCity("Springfield");
        downtownBranch.setState("IL");
        downtownBranch.setZipCode("62702");
        downtownBranch.setPhone("555-0200");
        downtownBranch.setEmail("downtown@bank.com");
        downtownBranch.setIfscCode("BANK0001002");
        downtownBranch.setSwiftCode("BANKUS34");
        downtownBranch.setStatus(Branch.BranchStatus.ACTIVE);
        downtownBranch.setWorkingHours("9:00 AM - 5:00 PM");
        downtownBranch.setIsMainBranch(false);
        branchRepository.save(downtownBranch);
        log.info("Created Downtown Branch (ID: {})", downtownBranch.getId());
    }

    /**
     * Creates all test users with proper role assignments and branch associations
     */
    private void initializeUsers() {
        Branch mainBranch = branchRepository.findByBranchCode("MB001")
                .orElseThrow(() -> new RuntimeException("Main Branch not found"));
        Branch downtownBranch = branchRepository.findByBranchCode("DB001")
                .orElseThrow(() -> new RuntimeException("Downtown Branch not found"));

        // 1. ADMIN User (no branch)
        // Username: admin | Password: admin123
        User admin = createUser(
                "admin",
                "admin123",
                "admin@bank.com",
                User.Role.ADMIN,
                null
        );
        log.info("Created ADMIN user: {}", admin.getUsername());

        // 2. BRANCH_MANAGER for Main Branch
        // Username: manager.main | Password: manager123
        User mainManager = createUser(
                "manager.main",
                "manager123",
                "manager.main@bank.com",
                User.Role.BRANCH_MANAGER,
                mainBranch
        );
        log.info("Created BRANCH_MANAGER for Main Branch: {}", mainManager.getUsername());

        // 3. BRANCH_MANAGER for Downtown Branch
        // Username: manager.downtown | Password: manager123
        User downtownManager = createUser(
                "manager.downtown",
                "manager123",
                "manager.downtown@bank.com",
                User.Role.BRANCH_MANAGER,
                downtownBranch
        );
        log.info("Created BRANCH_MANAGER for Downtown Branch: {}", downtownManager.getUsername());

        // 4. LOAN_OFFICER for Main Branch
        // Username: loan.officer | Password: loan123
        User loanOfficer = createUser(
                "loan.officer",
                "loan123",
                "loan.officer@bank.com",
                User.Role.LOAN_OFFICER,
                mainBranch
        );
        log.info("Created LOAN_OFFICER for Main Branch: {}", loanOfficer.getUsername());

        // 5. CARD_OFFICER for Downtown Branch
        // Username: card.officer | Password: card123
        User cardOfficer = createUser(
                "card.officer",
                "card123",
                "card.officer@bank.com",
                User.Role.CARD_OFFICER,
                downtownBranch
        );
        log.info("Created CARD_OFFICER for Downtown Branch: {}", cardOfficer.getUsername());

        // 6. CUSTOMER User 1 (no branch)
        // Username: customer1 | Password: customer123
        User customer1 = createUser(
                "customer1",
                "customer123",
                "customer1@email.com",
                User.Role.CUSTOMER,
                null
        );
        log.info("Created CUSTOMER user: {}", customer1.getUsername());

        // 7. CUSTOMER User 2 (no branch)
        // Username: customer2 | Password: customer123
        User customer2 = createUser(
                "customer2",
                "customer123",
                "customer2@email.com",
                User.Role.CUSTOMER,
                null
        );
        log.info("Created CUSTOMER user: {}", customer2.getUsername());
    }

    /**
     * Helper method to create and save a user with encoded password
     */
    private User createUser(String username, String password, String email,
                            User.Role role, Branch branch) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setRole(role);
        user.setIsActive(true);
        user.setBranch(branch);
        return userRepository.save(user);
    }

    /**
     * Prints all user credentials to the console for easy reference
     */
    private void printCredentials() {
        log.info("\n" +
                "================================================================================\n" +
                "                        TEST USER CREDENTIALS                                   \n" +
                "================================================================================\n" +
                "\n" +
                "ADMIN (No Branch Assignment):\n" +
                "  Username: admin\n" +
                "  Password: admin123\n" +
                "  Email: admin@bank.com\n" +
                "\n" +
                "--------------------------------------------------------------------------------\n" +
                "\n" +
                "BRANCH MANAGERS:\n" +
                "\n" +
                "  Main Branch Manager:\n" +
                "    Username: manager.main\n" +
                "    Password: manager123\n" +
                "    Email: manager.main@bank.com\n" +
                "    Branch: Main Branch (MB001)\n" +
                "\n" +
                "  Downtown Branch Manager:\n" +
                "    Username: manager.downtown\n" +
                "    Password: manager123\n" +
                "    Email: manager.downtown@bank.com\n" +
                "    Branch: Downtown Branch (DB001)\n" +
                "\n" +
                "--------------------------------------------------------------------------------\n" +
                "\n" +
                "LOAN OFFICER:\n" +
                "  Username: loan.officer\n" +
                "  Password: loan123\n" +
                "  Email: loan.officer@bank.com\n" +
                "  Branch: Main Branch (MB001)\n" +
                "\n" +
                "--------------------------------------------------------------------------------\n" +
                "\n" +
                "CARD OFFICER:\n" +
                "  Username: card.officer\n" +
                "  Password: card123\n" +
                "  Email: card.officer@bank.com\n" +
                "  Branch: Downtown Branch (DB001)\n" +
                "\n" +
                "--------------------------------------------------------------------------------\n" +
                "\n" +
                "CUSTOMERS (No Branch Assignment):\n" +
                "\n" +
                "  Customer 1:\n" +
                "    Username: customer1\n" +
                "    Password: customer123\n" +
                "    Email: customer1@email.com\n" +
                "\n" +
                "  Customer 2:\n" +
                "    Username: customer2\n" +
                "    Password: customer123\n" +
                "    Email: customer2@email.com\n" +
                "\n" +
                "================================================================================\n"
        );
    }
}










//package com.izak.demoBankManagement.config;
//
//import com.izak.demoBankManagement.entity.User;
//import com.izak.demoBankManagement.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class DataInitializer implements CommandLineRunner {
//
//    private final UserRepository userRepository;
//    private final PasswordEncoder passwordEncoder;
//
//    @Override
//    public void run(String... args) {
//        // Create Admin user if none exists
//        if (!userRepository.existsByUsername("admin")) {
//            User admin = new User();
//            admin.setUsername("admin");
//            // Encoded once here, directly into the repository
//            admin.setPassword(passwordEncoder.encode("admin123"));
//            admin.setEmail("admin@demobank.com");
//            admin.setRole(User.Role.ADMIN);
//            admin.setIsActive(true);
//
//            userRepository.save(admin);
//            log.info("Successfully created default system admin: 'admin'");
//        }
//    }
//}