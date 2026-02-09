package com.izak.demoBankManagement.exception;

public class DisbursementFailedException extends RuntimeException {
    public DisbursementFailedException(String message) {
        super(message);
    }

    public DisbursementFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
