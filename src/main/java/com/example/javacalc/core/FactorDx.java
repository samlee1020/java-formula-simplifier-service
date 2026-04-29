package com.example.javacalc.core;

import java.util.Objects;

public class FactorDx implements Factor {
    private Factor factor;

    public FactorDx(Factor factor) {
        this.factor = factor;
    }

    // 求导
    public Poly toPoly() {
        return factor.toPoly().dx();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof FactorDx) {
            FactorDx other = (FactorDx) obj;
            return this.factor.equals(other.factor);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(factor);
    }
}
