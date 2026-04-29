package com.example.javacalc.api.dto;

import java.util.ArrayList;
import java.util.List;

public class SimplifyRequest {
    private List<String> normalFunctions = new ArrayList<>();
    private List<List<String>> recursiveFunctions = new ArrayList<>();
    private String expression;

    public SimplifyRequest() {
    }

    public SimplifyRequest(List<String> normalFunctions, List<List<String>> recursiveFunctions,
        String expression) {
        setNormalFunctions(normalFunctions);
        setRecursiveFunctions(recursiveFunctions);
        this.expression = expression;
    }

    public List<String> getNormalFunctions() {
        return normalFunctions;
    }

    public void setNormalFunctions(List<String> normalFunctions) {
        this.normalFunctions = normalFunctions == null ? new ArrayList<>() : normalFunctions;
    }

    public List<List<String>> getRecursiveFunctions() {
        return recursiveFunctions;
    }

    public void setRecursiveFunctions(List<List<String>> recursiveFunctions) {
        this.recursiveFunctions = recursiveFunctions == null
            ? new ArrayList<>() : recursiveFunctions;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }
}
