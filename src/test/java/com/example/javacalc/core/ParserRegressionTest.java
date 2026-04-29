package com.example.javacalc.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.javacalc.api.dto.SimplifyRequest;
import com.example.javacalc.service.CalculatorService;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParserRegressionTest {
    private final CalculatorService calculatorService = new CalculatorService();

    @Test
    void inputFormatExamplesKeepTheirOriginalOutputs() {
        assertThat(calculatorService.simplify(new SimplifyRequest(
            List.of("f(x,y)=x+y"), List.of(), "f(x,2)"
        ))).isEqualTo("x+2");

        assertThat(calculatorService.simplify(new SimplifyRequest(List.of(), List.of(List.of(
            "f{0}(x)=x",
            "f{1}(x)=x^2",
            "f{n}(x)=f{n-1}(x)+f{n-2}(x)"
        )), "f{3}(x)"))).isEqualTo("2*x^2+x");

        assertThat(calculatorService.simplify(new SimplifyRequest(
            List.of(), List.of(), "dx(x^2+sin(x))"
        ))).isEqualTo("2*x+cos(x)");
    }
}
