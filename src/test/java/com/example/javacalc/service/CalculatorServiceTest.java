package com.example.javacalc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.javacalc.api.dto.SimplifyRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculatorServiceTest {
    private final CalculatorService calculatorService = new CalculatorService();

    @Test
    void simplifyDerivativeExpression() {
        SimplifyRequest request = new SimplifyRequest(List.of(), List.of(),
            "dx(x^2+sin(x))");

        assertThat(calculatorService.simplify(request)).isEqualTo("2*x+cos(x)");
    }

    @Test
    void simplifyNormalFunction() {
        SimplifyRequest request = new SimplifyRequest(List.of("f(x,y)=x+y"), List.of(),
            "f(x,2)");

        assertThat(calculatorService.simplify(request)).isEqualTo("x+2");
    }

    @Test
    void simplifyRecursiveFunction() {
        SimplifyRequest request = new SimplifyRequest(List.of(), List.of(List.of(
            "f{0}(x)=x",
            "f{1}(x)=x^2",
            "f{n}(x)=f{n-1}(x)+f{n-2}(x)"
        )), "f{3}(x)");

        assertThat(calculatorService.simplify(request)).isEqualTo("2*x^2+x");
    }

    @Test
    void simplifyMixedFunctionsFromApiPlan() {
        SimplifyRequest request = new SimplifyRequest(List.of("f(x,y)=x+y"), List.of(List.of(
            "g{0}(x)=x",
            "g{1}(x)=x^2",
            "g{n}(x)=g{n-1}(x)+g{n-2}(x)"
        )), "f(x,2)+g{3}(x)");

        assertThat(calculatorService.simplify(request)).isEqualTo("2*x+2+2*x^2");
    }

    @Test
    void rejectRecursiveFunctionWithWrongLineCount() {
        SimplifyRequest request = new SimplifyRequest(List.of(), List.of(List.of(
            "f{0}(x)=x",
            "f{1}(x)=x^2"
        )), "f{2}(x)");

        assertThatThrownBy(() -> calculatorService.simplify(request))
            .isInstanceOf(CalculatorException.class)
            .hasMessage("请求参数错误");
    }
}
