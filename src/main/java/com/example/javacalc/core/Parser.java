package com.example.javacalc.core;

import java.math.BigInteger;
import java.util.ArrayList;

// 解析器
public class Parser {
    private final Lexer lexer;
    private final Definer definer;

    public Parser(Lexer lexer) {
        this(lexer, new Definer());
    }

    public Parser(Lexer lexer, Definer definer) {
        this.lexer = lexer;
        this.definer = definer;
    }

    // parseExpr()调用了parseTerm()来解析项，parseTerm()会调用parseFactor()来解析因子

    // 解析表达式。
    public Expr parseExpr() {
        Expr expr = new Expr();

        // 解析第一个项，要注意第一个项前面可能有一个符号+/-来决定其正负
        int sign = 1;
        if (lexer.getCurToken().getType() == Token.TokenType.ADD) {
            lexer.nextToken(); // 跳过符号+
        } else if (lexer.getCurToken().getType() == Token.TokenType.SUB) {
            sign = -1;
            lexer.nextToken(); // 跳过符号-
        }
        expr.addTerm(parseTerm(sign));

        // 解析剩余项，剩余项前面一定有一个+/-
        while (!lexer.isEnd() && lexer.getCurToken().getType() == Token.TokenType.ADD || 
            lexer.getCurToken().getType() == Token.TokenType.SUB) {
            if (lexer.getCurToken().getType() == Token.TokenType.ADD) {
                lexer.nextToken(); // 跳过符号+
                expr.addTerm(parseTerm(1));
            } else {
                lexer.nextToken(); // 跳过符号-
                expr.addTerm(parseTerm(-1));
            }
        }

        return expr;
    }

    // 解析项。传入一个sign表示项的正负
    public Term parseTerm(int sign) {
        Term term = new Term(sign);

        // 解析第一个因子
        term.addFactor(parseFactor());
        // 解析剩余因子
        while (!lexer.isEnd() && lexer.getCurToken().getType() == Token.TokenType.MUL) {
            lexer.nextToken(); // 跳过符号*
            term.addFactor(parseFactor());
        }
        return term;
    }

    // 解析因子。   
    public Factor parseFactor() {
        // 解析符号
        int sign = 1;
        if (lexer.getCurToken().getType() == Token.TokenType.ADD || 
            lexer.getCurToken().getType() == Token.TokenType.SUB) {
            if (lexer.getCurToken().getType() == Token.TokenType.ADD) {
                lexer.nextToken(); // 跳过符号+
            } else {
                lexer.nextToken(); // 跳过符号-
                sign = -1;
            }
        }

        // 解析内容，数字、变量、括号表达式、三角函数、自定义函数、求导表达式
        if (lexer.getCurToken().getType() == Token.TokenType.NUM) {
            return parseNum(sign);
        } else if (lexer.getCurToken().getType() == Token.TokenType.VAR) {
            return parseVar();
        } else if (lexer.getCurToken().getType() == Token.TokenType.LPAREN) {
            return parseSubExpr();
        } else if (lexer.getCurToken().getType() == Token.TokenType.SIN) {
            return parseSin();
        } else if (lexer.getCurToken().getType() == Token.TokenType.COS) {
            return parseCos();
        } else if (lexer.getCurToken().getType() == Token.TokenType.Func) {
            return parseFunc();
        } else if (lexer.getCurToken().getType() == Token.TokenType.Dx) {
            return parseDx();
        } else {
            return null;
        }
    }

    // 解析数字。传入一个sign表示数字的正负
    public FactorNum parseNum(int sign) {
        BigInteger value = new BigInteger(lexer.getCurToken().getContent());
        value = value.multiply(BigInteger.valueOf(sign));
        lexer.nextToken(); // 跳过数字
        return new FactorNum(value);
    }

    // 解析变量。需要考虑变量的指数
    public FactorVar parseVar() {
        String name = lexer.getCurToken().getContent();
        lexer.nextToken(); // 跳过变量名x

        // 解析可能的指数
        BigInteger exponent = BigInteger.ONE;
        if (lexer.getCurToken().getType() == Token.TokenType.POW) {
            lexer.nextToken(); // 跳过符号^
            exponent = new BigInteger(lexer.getCurToken().getContent());
            lexer.nextToken(); // 跳过指数
        }

        return new FactorVar(name, exponent);
    }

    // 解析括号表达式
    public FactorSubExpr parseSubExpr() {
        FactorSubExpr subExpr = new FactorSubExpr();
        // 跳过左括号
        lexer.nextToken();
        // 解析表达式
        subExpr.setExpr(parseExpr());
        // 跳过右括号
        lexer.nextToken();
        // 解析指数
        subExpr.setExponent(1);
        if (lexer.getCurToken().getType() == Token.TokenType.POW) {
            lexer.nextToken(); // 跳过符号^
            subExpr.setExponent(Integer.parseInt(lexer.getCurToken().getContent()));
            lexer.nextToken(); // 跳过指数
        }
        return subExpr;
    }

    // 解析sin
    public FactorSin parseSin() {
        // 跳过sin
        lexer.nextToken();
        // 跳过左括号
        lexer.nextToken();
        // 解析因子
        Factor factor = parseFactor();
        // 跳过右括号
        lexer.nextToken();
        // 解析指数
        BigInteger exp = BigInteger.ONE;
        if (lexer.getCurToken().getType() == Token.TokenType.POW) {
            lexer.nextToken(); // 跳过符号^
            exp = new BigInteger(lexer.getCurToken().getContent());
            lexer.nextToken(); // 跳过指数
        }
        return new FactorSin(factor, exp);
    }

    // 解析cos
    public FactorCos parseCos() {
        // 跳过cos
        lexer.nextToken();
        // 跳过左括号
        lexer.nextToken();
        // 解析因子
        Factor factor = parseFactor();
        // 跳过右括号
        lexer.nextToken();
        // 解析指数
        BigInteger exp = BigInteger.ONE;
        if (lexer.getCurToken().getType() == Token.TokenType.POW) {
            lexer.nextToken(); // 跳过符号^
            exp = new BigInteger(lexer.getCurToken().getContent());
            lexer.nextToken(); // 跳过指数
        }
        return new FactorCos(factor, exp);
    }

    // 解析自定义函数
    public FactorFunc parseFunc() {
        // 解析函数名
        String funcName;
        funcName = lexer.getCurToken().getContent();
        lexer.nextToken();

        int funcIndex = 0;
        // 如果是递推函数，解析函数序号
        if (lexer.getCurToken().getType() == Token.TokenType.LBRACE) {
            // 跳过左大括号
            lexer.nextToken();
            // 解析函数序号
            funcIndex = Integer.parseInt(lexer.getCurToken().getContent());
            lexer.nextToken(); // 跳过函数序号
            // 跳过右大括号
            lexer.nextToken();
        } 

        // 跳过左括号
        lexer.nextToken();
        // 解析实参
        ArrayList<Factor> actualParams = new ArrayList<>();
        actualParams.add(parseFactor()); // 解析第一个实参
        while (!lexer.isEnd() && lexer.getCurToken().getType() == Token.TokenType.COMMA) { 
            lexer.nextToken(); // 跳过逗号
            actualParams.add(parseFactor()); // 解析下一个实参
        }
        // 跳过右括号
        lexer.nextToken();
        return new FactorFunc(funcName, funcIndex, actualParams, definer);
    }

    // 解析求导表达式
    public FactorDx parseDx() {
        // 跳过dx
        lexer.nextToken(); 
        // dx后面一定是用"()"括起来的表达式，可以看作一个子表达式，也是一个因子    
        return new FactorDx(parseFactor());
    }
}
