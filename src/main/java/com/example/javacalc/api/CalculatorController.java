package com.example.javacalc.api;

import com.example.javacalc.api.dto.SimplifyRequest;
import com.example.javacalc.api.dto.SimplifyResponse;
import com.example.javacalc.service.CalculatorService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CalculatorController {
    private final CalculatorService calculatorService;

    public CalculatorController(CalculatorService calculatorService) {
        this.calculatorService = calculatorService;
    }

    @PostMapping("/simplify")
    public SimplifyResponse simplify(@RequestBody SimplifyRequest request) {
        return new SimplifyResponse(calculatorService.simplify(request));
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
