package com.izak.demoBankManagement.exception;

public class UnauthorizedLoanAccessException extends RuntimeException {
    public UnauthorizedLoanAccessException(String message) {
        super(message);
    }
}