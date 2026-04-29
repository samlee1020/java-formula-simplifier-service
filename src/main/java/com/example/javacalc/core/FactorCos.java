package com.example.javacalc.core;

import java.math.BigInteger;
import java.util.Objects;

public class FactorCos implements Factor {
    private Factor factor;
    private BigInteger exp;

    public FactorCos(Factor factor, BigInteger exp) {
        this.factor = factor;
        this.exp = exp;
    }

    public Poly toPoly() {
        Poly newPoly = new Poly();
        Mono mono = new Mono(BigInteger.ONE, BigInteger.ZERO);
        mono.addCos(factor, exp);
        newPoly.addMono(mono);
        return newPoly;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof FactorCos)) {
            return false;
        }
        FactorCos otherCos = (FactorCos) other;
        return factor.equals(otherCos.factor) && exp.equals(otherCos.exp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(factor, exp);
    }

}
