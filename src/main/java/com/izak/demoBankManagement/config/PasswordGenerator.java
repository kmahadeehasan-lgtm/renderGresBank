package com.izak.demoBankManagement.config;


import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility class to generate BCrypt encoded passwords for test data
 * Run this class to generate encoded passwords for your SQL scripts
 */
public class PasswordGenerator {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        System.out.println("================================================================================");
        System.out.println("                    BCRYPT PASSWORD GENERATOR                                   ");
        System.out.println("================================================================================\n");

        // Generate passwords for all test users
        String[] passwords = {
                "admin123",
                "manager123",
                "loan123",
                "card123",
                "customer123"
        };

        for (String password : passwords) {
            String encoded = encoder.encode(password);
            System.out.println("Plain text: " + password);
            System.out.println("BCrypt:     " + encoded);
            System.out.println();
        }

        System.out.println("================================================================================");
        System.out.println("Copy these encoded passwords into your SQL script");
        System.out.println("================================================================================");
    }
}
