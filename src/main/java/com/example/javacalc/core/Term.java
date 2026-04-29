package com.example.javacalc.core;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Objects;

// 项类。由一个ArrayList<Factor>组成，用于存储项中的因子。
public class Term {
    private final ArrayList<Factor> factors;
    private final int sign;
    
    public Term(int sign) {
        this.sign = sign;
        factors = new ArrayList<Factor>();
    }
    
    public void addFactor(Factor factor) {
        factors.add(factor);
    }

    public Poly toPoly() {
        Poly newPoly = new Poly();
        // 多项式初始化为1/-1，取决于sign
        newPoly.addMono(new Mono(BigInteger.valueOf(sign), BigInteger.ZERO));
        for (Factor factor : factors) {
            newPoly = newPoly.mul(factor.toPoly());
        }
        return newPoly;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Term)) {
            return false;
        }
        Term other = (Term) obj;
        if (factors.size() != other.factors.size()) {
            return false;
        }
        if (sign != other.sign) {
            return false;
        }
        return this.factors.containsAll(other.factors) && other.factors.containsAll(this.factors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(factors, sign);
    }
}
