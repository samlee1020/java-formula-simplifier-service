package com.example.javacalc.core;

import java.math.BigInteger;
import java.util.Objects;

public class FactorNum implements Factor {
    private BigInteger value;

    public FactorNum(BigInteger value) {
        this.value = value;
    }
    
    public Poly toPoly() {
        Poly newPoly = new Poly();
        Mono newMono = new Mono(value, BigInteger.ZERO);
        newPoly.addMono(newMono);
        return newPoly;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FactorNum && Objects.equals(value, ((FactorNum) other).value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
