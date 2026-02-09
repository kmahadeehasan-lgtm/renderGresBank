package com.izak.demoBankManagement.exception;

public class LoanAlreadyDisbursedException extends RuntimeException {
    public LoanAlreadyDisbursedException(String message) {
        super(message);
    }
}