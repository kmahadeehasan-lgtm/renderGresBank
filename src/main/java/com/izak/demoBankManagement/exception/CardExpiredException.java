package com.izak.demoBankManagement.exception;


public class CardExpiredException extends RuntimeException {
    public CardExpiredException(String message) {
        super(message);
    }
}