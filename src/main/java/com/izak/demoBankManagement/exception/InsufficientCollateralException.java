package com.izak.demoBankManagement.exception;

public class InsufficientCollateralException extends RuntimeException {
    public InsufficientCollateralException(String message) {
        super(message);
    }
}
