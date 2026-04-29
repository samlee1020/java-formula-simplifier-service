package com.example.javacalc.core;

import java.util.ArrayList;

// 词法分析器
public class Lexer {
    private final ArrayList<Token> tokens = new ArrayList<>();
    private int index = 0;

    public Lexer(String input) {
        int pos = 0;
        while (pos < input.length()) {            
            if (input.charAt(pos) == '+') {     
                tokens.add(new Token(Token.TokenType.ADD, "+")); 
            } else if (input.charAt(pos) == '-') {                              
                tokens.add(new Token(Token.TokenType.SUB, "-"));
            } else if (input.charAt(pos) == '*') {                            
                tokens.add(new Token(Token.TokenType.MUL, "*"));
            } else if (input.charAt(pos) == '^') {                             
                tokens.add(new Token(Token.TokenType.POW, "^"));
            } else if (input.charAt(pos) == '(') {                              
                tokens.add(new Token(Token.TokenType.LPAREN, "("));
            } else if (input.charAt(pos) == ')') {                             
                tokens.add(new Token(Token.TokenType.RPAREN, ")"));
            } else if (input.charAt(pos) == 'x' || 
                input.charAt(pos) == 'y' || input.charAt(pos) == 'z') {
                tokens.add(new Token(Token.TokenType.VAR, input.charAt(pos) + ""));     
            } else if (input.charAt(pos) >= '0' && input.charAt(pos) <= '9') {     
                StringBuilder sb = new StringBuilder();                        
                char ch = input.charAt(pos);
                while (ch >= '0' && ch <= '9') {
                    sb.append(ch);
                    pos++;
                    if (pos >= input.length()) { 
                        break;
                    }
                    ch = input.charAt(pos);
                }
                tokens.add(new Token(Token.TokenType.NUM, sb.toString()));
                pos--; // 回退一个字符，因为while循环会自动加1
            } else if (input.charAt(pos) == '{') {                             
                tokens.add(new Token(Token.TokenType.LBRACE, "{"));
            } else if (input.charAt(pos) == '}') {                              
                tokens.add(new Token(Token.TokenType.RBRACE, "}"));
            } else if (input.charAt(pos) == '=') {                             
                tokens.add(new Token(Token.TokenType.EQAL, "="));
            } else if (input.charAt(pos) == 's') {                             
                tokens.add(new Token(Token.TokenType.SIN, "sin"));
                pos += 2; // sin有三个字符，但最后会统一pos++，所以这里只需要跳过两个字符
            } else if (input.charAt(pos) == 'c') {                              
                tokens.add(new Token(Token.TokenType.COS, "cos"));
                pos += 2; // cos有三个字符，但最后会统一pos++，所以这里只需要跳过两个字符
            } else if (input.charAt(pos) == ',') {                            
                tokens.add(new Token(Token.TokenType.COMMA, ","));
            } else if (input.charAt(pos) == 'f' || 
                input.charAt(pos) == 'g' || input.charAt(pos) == 'h') {
                tokens.add(new Token(Token.TokenType.Func,  input.charAt(pos) + "")); 
            } else if (input.charAt(pos) == 'n') {                              
                tokens.add(new Token(Token.TokenType.N, "n"));
            } else if (input.charAt(pos) == 'd') {                              
                tokens.add(new Token(Token.TokenType.Dx, "d"));
                pos++; // dx有两个字符，但最后会统一pos++，所以这里只需要跳过一个字符
            }
            pos++;
        }
    }

    // 预处理1，把tokens中连续的+/-合并为一个
    public void preTreatment_1() {
        ArrayList<Token> newTokens = new ArrayList<>();
        int i = 0;
        while (i < tokens.size()) {
            Token now = tokens.get(i);
            if (now.getType() == Token.TokenType.ADD || 
                now.getType() == Token.TokenType.SUB) {
                // 记录连续的+/-
                ArrayList<Token> group = new ArrayList<>();
                group.add(now);
                i++;
                while (i < tokens.size()) {
                    Token next = tokens.get(i);
                    if (next.getType() == Token.TokenType.ADD || 
                        next.getType() == Token.TokenType.SUB) {
                        group.add(next);
                        i++;
                    } else {
                        break;
                    }
                }
                // 合并连续的+/-
                int sign = 1;
                for (Token t : group) {
                    if (t.getType() == Token.TokenType.SUB) {
                        sign = sign * (-1);
                    }
                }
                // 把合并后结果放入newTokens
                if (sign == 1) {
                    newTokens.add(new Token(Token.TokenType.ADD, "+"));
                } else {
                    newTokens.add(new Token(Token.TokenType.SUB, "-"));
                }
            } else { // 其他情况不变
                newTokens.add(now);
                i++;
            }
        }
        // 把合并后的tokens替换原来的tokens
        tokens.clear();
        tokens.addAll(newTokens);
    }

    // 预处理2，把tokens中^后面的+去掉
    public void preTreatment_2() {
        ArrayList<Token> newTokens = new ArrayList<>();
        int i = 0;
        while (i < tokens.size()) {
            if (tokens.get(i).getType() == Token.TokenType.ADD) {
                if (i > 0 && tokens.get(i - 1).getType() == Token.TokenType.POW) {
                    i++; // 跳过+号
                }
                else {
                    newTokens.add(tokens.get(i));
                    i++;
                }
            }
            else {
                newTokens.add(tokens.get(i));
                i++;
            }
        }    
        // 把预处理后的tokens替换原来的tokens
        tokens.clear();
        tokens.addAll(newTokens);
    }

    public Token getCurToken() {
        if (isEnd()) {
            return new Token(Token.TokenType.EOF, "EOF");
        }
        return tokens.get(index);
    }

    public Token getTokenByIndex(int i) {
        return tokens.get(i);
    }

    public void nextToken() {
        index++;
    }

    public boolean isEnd() {
        return index >= tokens.size();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Token token : tokens) {
            sb.append(token.getContent() + " ");
        }
        return sb.toString();
    }
}
