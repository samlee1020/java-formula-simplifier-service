package com.example.javacalc.core;

import java.util.ArrayList;
import java.util.Objects;

public class FactorSubExpr implements Factor {
    private final ArrayList<Term> terms = new ArrayList<>();
    private int exponent;

    public void setExponent(int exponent) {
        this.exponent = exponent;
    }

    public void setExpr(Expr expr) {
        terms.clear();
        terms.addAll(expr.getTerms());
    }

    public Poly toPoly() {
        Poly newPoly = new Poly();
        for (Term term : terms) {
            newPoly = newPoly.add(term.toPoly());
        }
        newPoly = newPoly.pow(exponent);
        return newPoly;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof FactorSubExpr)) {
            return false;
        }
        FactorSubExpr otherFactor = (FactorSubExpr) other;
        if (terms.size() != otherFactor.terms.size() || exponent != otherFactor.exponent) {
            return false;
        }
        return terms.containsAll(otherFactor.terms) && otherFactor.terms.containsAll(terms);
    }

    @Override
    public int hashCode() {
        return Objects.hash(terms, exponent);
    }
}
