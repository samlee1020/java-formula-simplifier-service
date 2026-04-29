package com.example.javacalc.service;

import com.example.javacalc.api.dto.SimplifyRequest;

public interface CalculatorApi {
    String simplify(String requestJson);

    String simplify(SimplifyRequest request);
}
