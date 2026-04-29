package com.example.javacalc.api.dto;

public class SimplifyResponse {
    private String result;

    public SimplifyResponse() {
    }

    public SimplifyResponse(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
