package com.example.javacalc.core;

public class Token {
    public enum TokenType {
        // +、-、*、^、(、)、x/y/z、number
        ADD, SUB, MUL, POW, LPAREN, RPAREN, VAR, NUM, EOF,
        // {、}、=、sin、cos、,、f/g/h 、n
        LBRACE, RBRACE, EQAL, SIN, COS, COMMA, Func, N,
        // dx
        Dx
    }

    private final TokenType type;
    private final String content;

    public Token(TokenType type, String content) {
        this.type = type;
        this.content = content;
    }
    
    public TokenType getType() {
        return this.type;
    }

    public String getContent() {
        return this.content;
    }
}
