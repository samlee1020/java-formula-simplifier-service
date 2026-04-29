package com.example.javacalc.core;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

//单项式：常数 * 幂函数 * sin函数 * cos函数
public class Mono {
    private BigInteger coe;     // 变量系数
    private BigInteger exp;            // 变量指数
    private HashMap<Factor, BigInteger> sinMap; // 保存sin函数的指数
    private HashMap<Factor, BigInteger> cosMap; // 保存cos函数的指数

    public Mono(BigInteger coe, BigInteger exp) {
        this.coe = coe;
        this.exp = exp;
        this.sinMap = new HashMap<>();
        this.cosMap = new HashMap<>();
    }

    public void addSin(Factor factor, BigInteger exp) {
        sinMap.put(factor, exp);
    }

    public void addCos(Factor factor, BigInteger exp) {
        cosMap.put(factor, exp);
    }

    public BigInteger getCoe() {
        return coe;
    }

    public BigInteger getExp() {
        return exp;
    }

    public void setCoe(BigInteger coe) {
        this.coe = coe;
    }

    public void setExp(BigInteger exp) {
        this.exp = exp;
    }

    public HashMap<Factor, BigInteger> getSinMap() {
        return sinMap;
    }

    public HashMap<Factor, BigInteger> getCosMap() {
        return cosMap;
    }

    public int getTriCount() {
        return sinMap.size() + cosMap.size();
    }

    // 判断同类项
    public boolean isLike(Mono other) {
        if (this.exp.equals(other.exp) && 
            this.sinMap.equals(other.sinMap) && this.cosMap.equals(other.cosMap)) {
            return true;
        } else {
            return false;
        }
    }

    // 单项式乘法
    public Mono mul(Mono other) {
        Mono result = new Mono(this.coe.multiply(other.coe), this.exp.add(other.exp));

        // 合并sinMap
        result.sinMap.putAll(this.sinMap);
        for (Map.Entry<Factor, BigInteger> entry : other.sinMap.entrySet()) {
            Factor factor = entry.getKey();
            BigInteger newExp = 
                result.sinMap.getOrDefault(factor, BigInteger.ZERO).add(entry.getValue());
            result.sinMap.put(factor, newExp);
        }

        // 合并cosMap
        result.cosMap.putAll(this.cosMap);
        for (Map.Entry<Factor, BigInteger> entry : other.cosMap.entrySet()) {
            Factor factor = entry.getKey();
            BigInteger newExp = 
                result.cosMap.getOrDefault(factor, BigInteger.ZERO).add(entry.getValue());
            result.cosMap.put(factor, newExp);
        }

        return result;
    }

    // 输出sin清单 
    public String sinToString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Factor, BigInteger> entry : sinMap.entrySet()) {
            String factorStr = entry.getKey().toPoly().toString();
            if (factorStr.equals("0") && 
                !entry.getValue().equals(BigInteger.ZERO)) { 
                return "*0"; // sin(0)的非0次方 = 0
            }
            if (entry.getValue().equals(BigInteger.ZERO)) { 
                continue; // sin()的0次方 = 1
            }
            sb.append("*sin(");
            // 如果sin内的因子是表达式或函数调用，则需要加一个括号
            if ((entry.getKey() instanceof FactorSubExpr || 
                entry.getKey() instanceof FactorFunc) && 
                !entry.getKey().toPoly().isFactor()) {
                sb.append("(" + factorStr + ")");
            } else {
                sb.append(factorStr);
            }
            sb.append(")");
            if (!entry.getValue().equals(BigInteger.ONE)) {
                sb.append("^" + entry.getValue().toString());
            }      
        }
        return sb.toString();
    }

    // 输出cos清单
    public String cosToString() { 
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Factor, BigInteger> entry : cosMap.entrySet()) {
            String factorStr = entry.getKey().toPoly().toString();
            if (factorStr.equals("0") || 
                entry.getValue().equals(BigInteger.ZERO)) {
                continue;
            }
            sb.append("*cos(");
            // 如果cos内的因子是表达式或函数调用，则需要加一个括号
            if ((entry.getKey() instanceof FactorSubExpr || 
                entry.getKey() instanceof FactorFunc) && 
                !entry.getKey().toPoly().isFactor()) {
                sb.append("(" + factorStr + ")");
            } else {
                sb.append(factorStr);
            }
            sb.append(")");
            if (!entry.getValue().equals(BigInteger.ONE)) {
                sb.append("^" + entry.getValue().toString());
                
            }
        }
        return sb.toString();
    }

    @Override 
    public String toString() { 
        String sinString = sinToString();
        String cosString = cosToString();
        // 为0的特殊情况
        if (coe.equals(BigInteger.ZERO) || 
            sinString.equals("*0") || 
            cosString.equals("*0")) {
            return "0";
        }

        // 处理指数为0的情况，即常数项乘以三角函数
        if (exp.equals(BigInteger.ZERO)) {
            if (coe.equals(BigInteger.ONE)) {
                if (!sinString.equals("") || !cosString.equals("")) {
                    return (sinString + cosString).substring(1); // 去掉第一个*
                }
            } else if (coe.equals(BigInteger.ONE.negate())) {
                if (!sinString.equals("") || !cosString.equals("")) {
                    return "-" + (sinString + cosString).substring(1); // 去掉第一个*
                }
            }

            return coe.toString() + sinString + cosString;
        }
        
        // 处理系数为1或-1的情况
        if (coe.equals(BigInteger.ONE)) {
            if (exp.equals(BigInteger.ONE)) {
                return "x" + sinString + cosString; 
            } else {
                return "x^" + exp + sinString + cosString; 
            }
        } else if (coe.equals(BigInteger.ONE.negate())) {
            if (exp.equals(BigInteger.ONE)) {
                return "-x" + sinString + cosString; 
            } else {
                return "-x^" + exp + sinString + cosString; 
            }
        }
        
        // 处理指数为1的情况
        if (exp.equals(BigInteger.ONE)) {
            return coe + "*x" + sinString + cosString; 
        }
        
        // 一般情况
        return coe + "*x^" + exp + sinString + cosString;
    }

    // 判断是否为因子(不含乘号)
    public boolean isFactor() {
        if (sinMap.isEmpty() && cosMap.isEmpty())
        {
            if (coe.equals(BigInteger.ONE) || exp.equals(BigInteger.ZERO)) {
                return true;
            }
        }
        return false;
    }

    // 判断两个单项式是否恰好一个是sin(f)^2,另一个是cos(f)^2
    public boolean isSin2Cos2(Mono other) {
        // this是sin(f)^2，other是cos(f)^2
        if (this.sinMap.size() == 1 && this.cosMap.size() == 0) {
            if (other.sinMap.size() == 0 && other.cosMap.size() == 1) {
                Factor factorThis = this.sinMap.keySet().iterator().next();
                Factor factorOther = other.cosMap.keySet().iterator().next();
                if (factorOther.equals(factorThis)) {
                    BigInteger expThis = this.sinMap.get(factorThis);
                    BigInteger expOther = other.cosMap.get(factorOther);
                    if (expThis.equals(BigInteger.valueOf(2)) && 
                        expOther.equals(BigInteger.valueOf(2))) {
                        return true;
                    }
                }
            }
        }
        // this是cos(f)^2，other是sin(f)^2
        else if (this.cosMap.size() == 1 && this.sinMap.size() == 0) {
            if (other.cosMap.size() == 0 && other.sinMap.size() == 1) {
                Factor factorThis = this.cosMap.keySet().iterator().next();
                Factor factorOther = other.sinMap.keySet().iterator().next();
                if (factorOther.equals(factorThis)) {
                    BigInteger expThis = this.cosMap.get(factorThis);
                    BigInteger expOther = other.sinMap.get(factorOther);
                    if (expThis.equals(BigInteger.valueOf(2)) && 
                        expOther.equals(BigInteger.valueOf(2))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // 判断一个单项式恰好是cos(f)^2
    public boolean isCos2() {
        if (this.cosMap.size() == 1 && this.sinMap.size() == 0) {
            Factor factor = this.cosMap.keySet().iterator().next();
            BigInteger exp = this.cosMap.get(factor);
            if (exp.equals(BigInteger.valueOf(2))) {
                return true;
            }
        }
        return false;
    }

    // 判断一个单项式恰好是sin(f)^2
    public boolean isSin2() {
        if (this.sinMap.size() == 1 && this.cosMap.size() == 0) {
            Factor factor = this.sinMap.keySet().iterator().next();
            BigInteger exp = this.sinMap.get(factor);
            if (exp.equals(BigInteger.valueOf(2))) {
                return true;
            }
        }
        return false;
    }

    // 只返回除系数之外的Mono
    public Mono getPureMono() {
        Mono result = new Mono(BigInteger.ONE, exp);
        result.sinMap.putAll(this.sinMap);
        result.cosMap.putAll(this.cosMap);
        return result;
    }

    // 只返回三角函数因子的Mono
    public Mono getTrigonometricMono() {
        Mono result = new Mono(BigInteger.ONE, BigInteger.ZERO);
        result.sinMap.putAll(this.sinMap);
        result.cosMap.putAll(this.cosMap);
        return result;
    }

    // 只返回幂函数的Mono
    public Mono getPowerMono() {
        Mono result = new Mono(coe, exp);
        return result;
    }

    // 只返回sin函数的Mono
    public Mono getSinMono() {
        Mono result = new Mono(BigInteger.ONE, BigInteger.ZERO);
        result.sinMap.putAll(this.sinMap);
        return result;
    }

    // 只返回cos函数的Mono
    public Mono getCosMono() {
        Mono result = new Mono(BigInteger.ONE, BigInteger.ZERO);
        result.cosMap.putAll(this.cosMap);
        return result;
    }

    // m1比m2多的三角函数因子
    public static Mono extraTrigonometricFactors(Mono m1, Mono m2) {
        Mono result = new Mono(BigInteger.ONE, BigInteger.ZERO); // 创建一个新的Mono对象，系数为1，指数为0

        // 复制m1的sinMap和cosMap
        HashMap<Factor, BigInteger> resultSinMap = new HashMap<>(m1.getSinMap());
        HashMap<Factor, BigInteger> resultCosMap = new HashMap<>(m1.getCosMap());

        // 遍历m2的sinMap，减去相同的因子
        for (Map.Entry<Factor, BigInteger> entry : m2.getSinMap().entrySet()) {
            Factor factor = entry.getKey();
            BigInteger expM1 = resultSinMap.getOrDefault(factor, BigInteger.ZERO);
            BigInteger expM2 = entry.getValue();
            if (expM1.compareTo(expM2) > 0) {
                resultSinMap.put(factor, expM1.subtract(expM2));
            } else {
                resultSinMap.remove(factor);
            }
        }

        // 遍历m2的cosMap，减去相同的因子
        for (Map.Entry<Factor, BigInteger> entry : m2.getCosMap().entrySet()) {
            Factor factor = entry.getKey();
            BigInteger expM1 = resultCosMap.getOrDefault(factor, BigInteger.ZERO);
            BigInteger expM2 = entry.getValue();
            if (expM1.compareTo(expM2) > 0) {
                resultCosMap.put(factor, expM1.subtract(expM2));
            } else {
                resultCosMap.remove(factor);
            }
        }

        // 设置结果Mono的sinMap和cosMap
        result.sinMap = resultSinMap;
        result.cosMap = resultCosMap;

        return result;
    }

    // 两个单项式的公共三角函数因子
    public static Mono commonTrigonometricFactors(Mono m1, Mono m2) {
        Mono result = new Mono(BigInteger.ONE, BigInteger.ZERO); // 创建一个新的Mono对象，系数为1，指数为0

        // 创建结果sinMap和cosMap，用于存储相同的三角函数因子
        HashMap<Factor, BigInteger> resultSinMap = new HashMap<>();
        HashMap<Factor, BigInteger> resultCosMap = new HashMap<>();

        // 遍历m1的sinMap，找出m1和m2中相同的sin因子
        for (Map.Entry<Factor, BigInteger> entry : m1.getSinMap().entrySet()) {
            Factor factor = entry.getKey();
            BigInteger exp1 = entry.getValue();
            BigInteger exp2 = m2.getSinMap().getOrDefault(factor, BigInteger.ZERO);
            BigInteger min = exp1.compareTo(exp2) < 0 ? exp1 : exp2;
            if (min.compareTo(BigInteger.ZERO) > 0) {
                resultSinMap.put(factor, min);
            }
        }

        // 遍历m1的cosMap，找出m1和m2中相同的cos因子
        for (Map.Entry<Factor, BigInteger> entry : m1.getCosMap().entrySet()) {
            Factor factor = entry.getKey();
            BigInteger exp1 = entry.getValue();
            BigInteger exp2 = m2.getCosMap().getOrDefault(factor, BigInteger.ZERO);
            BigInteger min = exp1.compareTo(exp2) < 0 ? exp1 : exp2;
            if (min.compareTo(BigInteger.ZERO) > 0) {
                resultCosMap.put(factor, min);
            }
        }

        // 将相同的三角函数因子设置到结果Mono对象中
        result.sinMap = resultSinMap;
        result.cosMap = resultCosMap;

        return result;
    }

    // 求导
    public Poly dx() {
        return DxTool.monoDx(this);
    }

}
