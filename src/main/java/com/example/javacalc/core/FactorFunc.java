package com.example.javacalc.core;

import java.util.ArrayList;
import java.util.Objects;   

public class FactorFunc implements Factor {
    private final String funcName;
    private final int funcIndex;
    private final ArrayList<Factor> actualParams;
    private final Definer definer;
    
    public FactorFunc(String funcName, int funcIndex, ArrayList<Factor> actualParams,
        Definer definer) {
        this.funcName = funcName;
        this.funcIndex = funcIndex;
        this.actualParams = new ArrayList<>(actualParams);
        this.definer = definer;
    }

    public Poly toPoly() {
        Poly newPoly = new Poly();
        if (funcIndex == 0 || funcIndex == 1) {
            String defStr = definer.getFuncDef(funcName, funcIndex);
            ArrayList<String> paramList = definer.getParamList(funcName);
            for (int i = 0; i < paramList.size(); i++) {
                defStr = defStr.replaceAll(paramList.get(i), 
                "(" + actualParams.get(i).toPoly().toString().replaceAll("x","X") + ")"); // 替换参数
            }
            defStr = defStr.replaceAll("X", "x");
            Lexer lexer = new Lexer("(" + defStr + ")");
            lexer.preTreatment_1();
            lexer.preTreatment_2();
            Parser parser = new Parser(lexer, definer);
            Expr expr = parser.parseExpr();
            newPoly = expr.toPoly();
        } else { 
            // 递归调用
            String recStr = definer.getRecExpr(funcName);
            ArrayList<String> paramList = definer.getParamList(funcName);
            recStr = recStr.replaceAll("n-1",funcIndex - 1 + "");
            recStr = recStr.replaceAll("n-2",funcIndex - 2 + "");
            for (int i = 0; i < paramList.size(); i++) {
                recStr = recStr.replaceAll(paramList.get(i), 
                "(" + actualParams.get(i).toPoly().toString().replaceAll("x","X") + ")"); // 替换参数
            }
            recStr = recStr.replaceAll("X", "x");
            Lexer lexer = new Lexer("(" + recStr + ")");
            lexer.preTreatment_1();
            lexer.preTreatment_2();
            Parser parser = new Parser(lexer, definer);
            Expr expr = parser.parseExpr();
            newPoly = expr.toPoly();
        }
        return newPoly;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof FactorFunc)) {
            return false;
        }
        FactorFunc otherFunc = (FactorFunc) other;
        return funcName.equals(otherFunc.funcName) && funcIndex == otherFunc.funcIndex &&
            actualParams.equals(otherFunc.actualParams) && definer == otherFunc.definer;
    }

    @Override
    public int hashCode() {
        return Objects.hash(funcName, funcIndex, actualParams, System.identityHashCode(definer));
    }
}
