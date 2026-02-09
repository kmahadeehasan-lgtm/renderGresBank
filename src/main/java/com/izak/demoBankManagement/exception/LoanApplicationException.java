package com.izak.demoBankManagement.exception;


public class LoanApplicationException extends RuntimeException {
    public LoanApplicationException(String message) {
        super(message);
    }
}