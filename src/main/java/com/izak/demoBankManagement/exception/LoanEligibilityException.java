package com.izak.demoBankManagement.exception;

public class LoanEligibilityException extends RuntimeException {
    private final java.util.List<String> reasons;

    public LoanEligibilityException(String message, java.util.List<String> reasons) {
        super(message);
        this.reasons = reasons;
    }

    public java.util.List<String> getReasons() {
        return reasons;
    }
}
