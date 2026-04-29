package com.example.javacalc.core;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// 多项式类
public class Poly {
    private ArrayList<Mono> monos;

    public void addMono(Mono mono) {
        monos.add(mono);
    }

    public Poly() {
        monos = new ArrayList<>();
    }

    public Poly(Mono mono) {
        monos = new ArrayList<>();
        monos.add(mono);
    }

    // 多项式加法
    public Poly add(Poly other) {
        Poly newPoly = new Poly();
        // 把当前多项式的项初始化到新多项式中
        newPoly.monos.addAll(this.monos);

        // 把其他多项式的项添加到新多项式中
        for (Mono om : other.monos) {
            boolean found = false;
            for (Mono nm : newPoly.monos) {
                if (nm.isLike(om)) { // 找到同类项
                    // 系数相加
                    BigInteger newCoe = nm.getCoe().add(om.getCoe());
                    // 如果系数为0，则删除该项
                    if (newCoe.equals(BigInteger.ZERO)) {
                        newPoly.monos.remove(nm);
                    } else { // 否则更新系数
                        nm.setCoe(newCoe);
                    }
                    found = true;
                    break;
                }
            }
            if (!found) { // 未找到同类项，则添加到新多项式中
                newPoly.monos.add(om);
            }
        }
        return newPoly;
    }    

    // 多项式乘法
    public Poly mul(Poly other) {
        Poly result = new Poly();
        
        for (Mono m1 : this.monos) {
            for (Mono m2 : other.monos) {
                Mono newMono = m1.mul(m2);
                Poly newPoly = new Poly();
                newPoly.monos.add(newMono);
                result = result.add(newPoly);
            }
        }

        return result;
    }

    // 多项式乘方
    public Poly pow(int exp) {
        Poly newPoly = new Poly();
        // 指数为0，返回1
        if (exp == 0) {
            newPoly.monos.add(new Mono(BigInteger.ONE, BigInteger.ZERO));
        } else {
            newPoly.monos.addAll(this.monos);
            for (int i = 1; i < exp; i++)
            {
                newPoly = newPoly.mul(this);
            }
        }
        return newPoly;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < monos.size(); i++) {
            String ms = monos.get(i).toString();
            if (ms.equals("0")) {
                continue;
            }
            if (i > 0 && !ms.startsWith("-")) {
                sb.append("+");
            }
            sb.append(ms);
        }
        if (sb.length() == 0) {
            sb.append("0");
        }
        return sb.toString();
    }

    // 由Poly获取表达式因子
    public FactorSubExpr toFactorSubExpr() {
        String polyStr = this.trigonometric().toString();
        Lexer lexer = new Lexer(polyStr);
        Parser parser = new Parser(lexer);
        Expr expr = parser.parseExpr();
        FactorSubExpr factorSubExpr = new FactorSubExpr();
        factorSubExpr.setExpr(expr);
        factorSubExpr.setExponent(1);
        return factorSubExpr;
    }

    // 判断是否为因子
    public boolean isFactor() {
        return monos.size() == 1 && monos.get(0).isFactor();
    }

    // 三角函数化简尝试，对第二次解析后的表达式调用
    public Poly trigonometric() { //二次解析后的表达式，每一个项都是Mono的标准输出格式
        Poly newPoly = new Poly();
        newPoly.monos.addAll(this.monos);
        boolean isSimplified = false;
        while (!isSimplified) { // 项与项之间的化简：cos二倍角、平方和
            int isChanged = 0;
            for (Mono m1 : newPoly.monos) {
                for (Mono m2 : newPoly.monos) {
                    if (m1.equals(m2)) { continue; }
                    if (tryMerge(m1,m2)) { // 尝试合并项
                        int mergeSuccess = 0;
                        int a = differTerm1.getTriCount();
                        int b = differTerm2.getTriCount();
                        Poly tempPoly = new Poly();
                        if (a == 1 && b == 1 && 
                            differTerm1.isSin2Cos2(differTerm2)) {                     
                            if (m1.getCoe().add(m2.getCoe()).equals(BigInteger.ZERO)) {
                                simplify1(m1, m2, tempPoly);
                            } else { simplify2(m1, m2, tempPoly); }
                            mergeSuccess = 1;
                        } 
                        else if (a + b == 1 && 
                            m1.getCoe().add(m2.getCoe()).equals(BigInteger.ZERO)) {
                            Mono m = (a == 1) ? differTerm1 : differTerm2;
                            if (m.isCos2() || m.isSin2()) {
                                simplify3(m, tempPoly, (a == 1) ? m1.getCoe() : m2.getCoe());
                                mergeSuccess = 1;
                            }
                        }
                        if (mergeSuccess == 1) {
                            newPoly.monos.remove(m1);
                            newPoly.monos.remove(m2);
                            newPoly = newPoly.add(tempPoly);
                            isChanged = 1;
                            break;
                        }
                    }
                }
                if (isChanged == 1) { break; }
            }
            if (isChanged == 0) { isSimplified = true; }
        }    
        isSimplified = false;
        while (!isSimplified) { // 项本身化简：sin二倍角
            int isChanged = 0;
            Poly tempPoly = new Poly();
            for (Mono m : newPoly.monos) {
                if (simplify4(m, tempPoly)) {
                    newPoly.monos.remove(m);
                    newPoly = newPoly.add(tempPoly);
                    isChanged = 1;
                    break;
                }
            }
            if (isChanged == 0) { isSimplified = true; }
        } 
        return newPoly;
    }

    // k*cos(x)^2 - k*sin(x)^2 = k*cos(2*x)
    private void simplify1(Mono m1, Mono m2, Poly tempPoly) {
        BigInteger k = m1.isCos2() ? m1.getCoe() : m2.getCoe();
        Factor factor = m1.isCos2() ? 
            m1.getCosMap().keySet().iterator().next() : 
            m2.getCosMap().keySet().iterator().next();
        Poly twice = new Poly();
        twice.addMono(new Mono(BigInteger.ONE.add(BigInteger.ONE), BigInteger.ZERO));
        Factor factorTwice = factor.toPoly().mul(twice).toFactorSubExpr();
        Mono cos2x = new Mono(BigInteger.ONE, BigInteger.ZERO);
        cos2x.addCos(factorTwice, BigInteger.ONE);
        tempPoly.addMono(sharedTerm.mul(cos2x).mul(new Mono(k,BigInteger.ZERO)));
    }

    // coe1*sin(x)^2 + coe2*cos(x)^2 = min + (coe1-min)*sin(x)^2 + (coe2-min)*cos(x)^2
    private void simplify2(Mono m1, Mono m2, Poly tempPoly) {
        BigInteger min = 
            m1.getCoe().abs().compareTo(m2.getCoe().abs()) < 0 ? 
            m1.getCoe() : m2.getCoe();
        tempPoly.addMono(sharedTerm.mul(new Mono(min,BigInteger.ZERO)));
        if (!m1.getCoe().equals(min)) {
            tempPoly.addMono(m1.getPureMono().mul(new Mono(
                m1.getCoe().subtract(min),BigInteger.ZERO)));
        }
        if (!m2.getCoe().equals(min)) {
            tempPoly.addMono(m2.getPureMono().mul(new Mono(
                m2.getCoe().subtract(min),BigInteger.ZERO)));
        }
    }

    // sin(x)^2 - 1 = -cos(x)^2  ||  cos(x)^2 - 1 = -sin(x)^2
    private void simplify3(Mono m, Poly tempPoly, BigInteger k) {
        Factor factor;
        if (m.isSin2()) {
            factor = m.getSinMap().keySet().iterator().next();
        } else {
            factor = m.getCosMap().keySet().iterator().next();
        }

        Mono tempMono = new Mono(k.negate(), BigInteger.ZERO);
        if (m.isSin2()) {
            tempMono.addCos(factor, BigInteger.valueOf(2));
        } else {
            tempMono.addSin(factor, BigInteger.valueOf(2));
        }
        tempPoly.addMono(sharedTerm.mul(tempMono));
    }

    // k*2^n*sin(x)^n*cos(x)^n = k*sin(2*x)^n
    private boolean simplify4(Mono m, Poly tempPoly) {
        BigInteger coe = m.getCoe();
        if (!coe.and(BigInteger.ONE).equals(BigInteger.ZERO)) {
            return false; // 系数不是2的整数倍，不能化简
        } 
        HashMap<Factor, BigInteger> sinMap = m.getSinMap();
        HashMap<Factor, BigInteger> cosMap = m.getCosMap();
        if (sinMap.size() < 1 || cosMap.size() < 1) {
            return false;
        }
        for (Map.Entry<Factor, BigInteger> entry1 : sinMap.entrySet()) {
            for (Map.Entry<Factor, BigInteger> entry2 : cosMap.entrySet()) {
                if (entry1.getKey().equals(entry2.getKey()) && 
                    entry1.getValue().equals(entry2.getValue())) {
                    Factor factor = entry1.getKey();
                    BigInteger exp = entry1.getValue();
                    if (exp.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
                        continue; // 指数过大，不能化简
                    }
                    BigInteger twoPowExp = BigInteger.valueOf(2).pow(exp.intValue()); 
                    if (coe.mod(twoPowExp).equals(BigInteger.ZERO)) {
                        BigInteger k = coe.divide(twoPowExp);
                        Mono tempMono = new Mono(k, m.getExp());
                        tempMono.getSinMap().putAll(sinMap);
                        tempMono.getCosMap().putAll(cosMap);
                        tempMono.getSinMap().remove(entry1.getKey());
                        tempMono.getCosMap().remove(entry2.getKey());
                        Poly twice = new Poly(new Mono(BigInteger.valueOf(2), BigInteger.ZERO));
                        Factor factorTwice = factor.toPoly().mul(twice).toFactorSubExpr();
                        Mono sin2x = new Mono(BigInteger.ONE, BigInteger.ZERO);
                        sin2x.addSin(factorTwice, exp);
                        tempMono = tempMono.mul(sin2x);
                        tempPoly.addMono(tempMono);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // 工具对象
    private Mono sharedTerm;
    private Mono differTerm1;
    private Mono differTerm2;

    // 判断是否能合并，并实时更新工具对象
    public boolean tryMerge(Mono m1, Mono m2) {
        if (Math.abs(m1.getTriCount() - m2.getTriCount()) > 1) {
            return false; // 项数相差超过1，不能合并
        }
        if (!m1.getExp().equals(m2.getExp())) {
            return false; // 指数不同，不能合并
        }
        BigInteger exp = m1.getExp();
        // 重置工具对象
        sharedTerm = new Mono(BigInteger.ONE, exp);
        differTerm1 = new Mono(BigInteger.ONE, exp);
        differTerm2 = new Mono(BigInteger.ONE, exp);

        differTerm1 = differTerm1.mul(Mono.extraTrigonometricFactors(m1, m2));
        if (differTerm1.getTriCount() > 1) {
            return false; // 项数超过1，不能合并
        }
        differTerm2 = differTerm2.mul(Mono.extraTrigonometricFactors(m2, m1));
        if (differTerm2.getTriCount() > 1) {
            return false; // 项数超过1，不能合并
        }
        sharedTerm = sharedTerm.mul(Mono.commonTrigonometricFactors(m1, m2));
        return true;
    }
    
    // 多项式求导
    public Poly dx() {
        Poly newPoly = new Poly();
        // 多项式的导数等于各项的导数之和
        for (Mono m : monos) {
            newPoly = newPoly.add(m.dx());
        }
        return newPoly;
    }
}
