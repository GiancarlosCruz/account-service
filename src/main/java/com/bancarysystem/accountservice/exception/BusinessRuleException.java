package com.bancarysystem.accountservice.exception;

public class BusinessRuleException extends RuntimeException {

    private final int statusCode;

    public BusinessRuleException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}