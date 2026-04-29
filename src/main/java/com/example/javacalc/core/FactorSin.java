package com.example.javacalc.core;

import java.math.BigInteger;
import java.util.Objects;

public class FactorSin implements Factor {
    private Factor factor;
    private BigInteger exp;

    public FactorSin(Factor factor, BigInteger exp) {
        this.factor = factor;
        this.exp = exp;
    }

    public Poly toPoly() {
        Poly newPoly = new Poly();
        Mono mono = new Mono(BigInteger.ONE, BigInteger.ZERO);
        mono.addSin(factor, exp);
        newPoly.addMono(mono);
        return newPoly;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof FactorSin)) {
            return false;
        }
        FactorSin otherSin = (FactorSin) other;
        return factor.equals(otherSin.factor) && exp.equals(otherSin.exp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(factor, exp);
    }
}

