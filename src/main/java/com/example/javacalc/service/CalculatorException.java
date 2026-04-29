package com.example.javacalc.service;

import org.springframework.http.HttpStatus;

public class CalculatorException extends RuntimeException {
    private final String code;
    private final String detail;
    private final HttpStatus status;

    public CalculatorException(String code, String message, String detail, HttpStatus status) {
        super(message);
        this.code = code;
        this.detail = detail;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public String getDetail() {
        return detail;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
