package com.example.javacalc.core;

import java.math.BigInteger;
import java.util.HashMap;

// 求导工具类，在Mono中调用
public class DxTool {
    public static Poly monoDx(Mono m) {
        // (常数 * 幂函数) * (sinMap * cosMap)
        Mono term1 = m.getPowerMono();
        Mono term2 = m.getTrigonometricMono();
        Poly dx1 = kxDx(term1);
        Poly dx2 = triDx(term2);
        Poly dx1Term2 = dx1.mul(new Poly(term2));
        Poly dx2Term1 = dx2.mul(new Poly(term1));
        return dx1Term2.add(dx2Term1);
    }

    // 常数 * 幂函数求导
    private static Poly kxDx(Mono m) {
        BigInteger k = m.getCoe();
        BigInteger exp = m.getExp();
        Poly result = new Poly();
        if (exp.equals(BigInteger.ZERO)) {
            // dx(k) = 0
            result.addMono(new Mono(BigInteger.ZERO, BigInteger.ZERO));
        } else {
            // dx(k*x^exp) = k*exp*x^(exp-1)
            result.addMono(new Mono(k.multiply(exp), exp.subtract(BigInteger.ONE)));
        } 
        return result;
    }

    // sinMap * cosMap求导
    private static Poly triDx(Mono m) {
        Poly term1 = new Poly(m.getSinMono());
        Poly term2 = new Poly(m.getCosMono());
        Poly dx1 = sinMapDx(m);
        Poly dx2 = cosMapDx(m);
        Poly dx1Term2 = dx1.mul(term2);
        Poly dx2Term1 = dx2.mul(term1);
        return dx1Term2.add(dx2Term1);
    }

    // sinMap求导
    private static Poly sinMapDx(Mono m) {
        HashMap<Factor, BigInteger> sinMap = m.getSinMap();
        Poly result = new Poly();
        // 链式法则
        for (HashMap.Entry<Factor, BigInteger> entry1 : sinMap.entrySet()) {
            Poly dx = sinDx(entry1.getKey(), entry1.getValue());
            Mono other = new Mono(BigInteger.ONE, BigInteger.ZERO);
            for (HashMap.Entry<Factor, BigInteger> entry2 : sinMap.entrySet()) {
                if (!entry1.equals(entry2)) {
                    other.addSin(entry2.getKey(), entry2.getValue());
                }
            }
            result = result.add(dx.mul(new Poly(other)));       
        }
        return result;
    }

    // cosMap求导
    private static Poly cosMapDx(Mono m) {
        HashMap<Factor, BigInteger> cosMap = m.getCosMap();
        Poly result = new Poly();
        // 链式法则
        for (HashMap.Entry<Factor, BigInteger> entry1 : cosMap.entrySet()) {
            Poly dx = cosDx(entry1.getKey(), entry1.getValue());
            Mono other = new Mono(BigInteger.ONE, BigInteger.ZERO);
            for (HashMap.Entry<Factor, BigInteger> entry2 : cosMap.entrySet()) {
                if (!entry1.equals(entry2)) {
                    other.addCos(entry2.getKey(), entry2.getValue());
                }
            }
            result = result.add(dx.mul(new Poly(other)));       
        }
        return result;
    }

    // sin求导
    private static Poly sinDx(Factor factor, BigInteger exp) {
        // dx(sin(factor)^exp) = exp*sin(factor)^(exp-1) * dx(sin(factor))
        // = exp*sin(factor)^(exp-1) * cos(factor) * dx(factor)

        Mono m1 = new Mono(exp, BigInteger.ZERO);
        m1.addSin(factor, exp.subtract(BigInteger.ONE));
        Poly term1 = new Poly(m1);

        Mono m2 = new Mono(BigInteger.ONE, BigInteger.ZERO);
        m2.addCos(factor, BigInteger.ONE);
        Poly term2 = new Poly(m2);

        return term1.mul(term2).mul(factor.toPoly().dx());
    }

    // cos求导
    private static Poly cosDx(Factor factor, BigInteger exp) {
        // dx(cos(factor)^exp) = -exp*cos(factor)^(exp-1) * dx(cos(factor))
        // = exp*cos(factor)^(exp-1) * -sin(factor)* dx(factor)

        Mono m1 = new Mono(exp, BigInteger.ZERO);
        m1.addCos(factor, exp.subtract(BigInteger.ONE));
        Poly term1 = new Poly(m1);

        Mono m2 = new Mono(BigInteger.ONE.negate(), BigInteger.ZERO);
        m2.addSin(factor, BigInteger.ONE);
        Poly term2 = new Poly(m2);

        return term1.mul(term2).mul(factor.toPoly().dx());
    }

}
