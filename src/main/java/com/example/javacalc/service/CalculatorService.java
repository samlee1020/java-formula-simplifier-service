package com.example.javacalc.service;

import com.example.javacalc.api.dto.SimplifyRequest;
import com.example.javacalc.core.Definer;
import com.example.javacalc.core.Expr;
import com.example.javacalc.core.Lexer;
import com.example.javacalc.core.Parser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class CalculatorService implements CalculatorApi {
    private static final int MAX_FUNCTION_COUNT = 64;
    private static final int MAX_EXPRESSION_LENGTH = 10000;
    private static final int MAX_FUNCTION_LINE_LENGTH = 5000;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String simplify(String requestJson) {
        try {
            return simplify(objectMapper.readValue(requestJson, SimplifyRequest.class));
        } catch (JsonProcessingException ex) {
            throw invalidRequest("请求 JSON 格式错误: " + ex.getOriginalMessage());
        }
    }

    @Override
    public String simplify(SimplifyRequest request) {
        validateRequest(request);
        Definer definer = new Definer();
        try {
            for (String normalFunction : request.getNormalFunctions()) {
                ArrayList<String> lines = new ArrayList<>();
                lines.add(normalizeFunctionLine(normalFunction));
                definer.addFunc(lines);
            }
            for (List<String> recursiveFunction : request.getRecursiveFunctions()) {
                if (recursiveFunction == null || recursiveFunction.size() != 3) {
                    throw invalidRequest("recursive function definition must contain exactly 3 lines");
                }
                ArrayList<String> lines = new ArrayList<>();
                for (String line : recursiveFunction) {
                    lines.add(normalizeFunctionLine(line));
                }
                definer.addFunc(lines);
            }
        } catch (CalculatorException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new CalculatorException("INVALID_FUNCTION_DEFINITION", "函数定义格式错误",
                detail(ex), HttpStatus.BAD_REQUEST);
        }

        try {
            return simplifyExpression(normalizeExpression(request.getExpression()), definer);
        } catch (RuntimeException ex) {
            throw new CalculatorException("INVALID_EXPRESSION", "表达式格式错误", detail(ex),
                HttpStatus.BAD_REQUEST);
        }
    }

    private String simplifyExpression(String exprString, Definer definer) {
        Lexer lexer = new Lexer(exprString);
        lexer.preTreatment_1();
        lexer.preTreatment_2();

        Parser parser = new Parser(lexer, definer);
        Expr expr = parser.parseExpr();

        Lexer newLexer = new Lexer(expr.toPoly().toString());
        Parser newParser = new Parser(newLexer, definer);
        Expr newExpr = newParser.parseExpr();
        return newExpr.toPoly().trigonometric().toString();
    }

    private void validateRequest(SimplifyRequest request) {
        if (request == null) {
            throw invalidRequest("request body is required");
        }
        if (request.getExpression() == null || request.getExpression().trim().isEmpty()) {
            throw invalidRequest("expression must not be blank");
        }
        if (request.getNormalFunctions() == null) {
            throw invalidRequest("normalFunctions must be an array");
        }
        if (request.getRecursiveFunctions() == null) {
            throw invalidRequest("recursiveFunctions must be an array");
        }
        int functionCount = request.getNormalFunctions().size()
            + request.getRecursiveFunctions().size();
        if (functionCount > MAX_FUNCTION_COUNT) {
            throw invalidRequest("function definition count exceeds " + MAX_FUNCTION_COUNT);
        }
        if (normalizeExpression(request.getExpression()).length() > MAX_EXPRESSION_LENGTH) {
            throw invalidRequest("expression length exceeds " + MAX_EXPRESSION_LENGTH);
        }
    }

    private String normalizeExpression(String value) {
        return value.replaceAll("\\s+", "");
    }

    private String normalizeFunctionLine(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw invalidRequest("function definition line must not be blank");
        }
        String normalized = value.replaceAll("\\s+", "");
        if (normalized.length() > MAX_FUNCTION_LINE_LENGTH) {
            throw invalidRequest("function definition line length exceeds "
                + MAX_FUNCTION_LINE_LENGTH);
        }
        return normalized;
    }

    private CalculatorException invalidRequest(String detail) {
        return new CalculatorException("INVALID_REQUEST", "请求参数错误", detail,
            HttpStatus.BAD_REQUEST);
    }

    private String detail(RuntimeException ex) {
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }
}
