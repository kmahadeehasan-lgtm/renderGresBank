package com.izak.demoBankManagement.exception;


public class InvalidCardOperationException extends RuntimeException {
    public InvalidCardOperationException(String message) {
        super(message);
    }
}