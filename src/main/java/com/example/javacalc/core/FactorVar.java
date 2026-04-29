package com.example.javacalc.core;

import java.math.BigInteger;
import java.util.Objects;

public class FactorVar implements Factor {
    private String name; // 变量命
    private BigInteger exponent; // 指数
    
    public FactorVar(String name, BigInteger exponent) {
        this.name = name;
        this.exponent = exponent;
    }

    public Poly toPoly() {
        Poly newPoly = new Poly();
        Mono newMono = new Mono(BigInteger.ONE, exponent);
        newPoly.addMono(newMono);
        return newPoly;
    }

    public String getName() {
        return name;
    }

    public BigInteger getExponent() {
        return exponent;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof FactorVar)) {
            return false;
        }
        FactorVar otherVar = (FactorVar) other;
        return name.equals(otherVar.getName()) && exponent.equals(otherVar.getExponent());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, exponent);
    }
}
