package com.example.javacalc.core;

import java.util.ArrayList;

// 表达式类。由一个ArrayList<Term>来存储它的 Term 项。
public class Expr {
    private final ArrayList<Term> terms = new ArrayList<>();

    public void addTerm(Term term) {
        terms.add(term);
    }

    public ArrayList<Term> getTerms() {
        return terms;
    }

    public Poly toPoly() {
        Poly newPoly = new Poly();
        for (Term term : terms) {
            newPoly = newPoly.add(term.toPoly());
        }
        return newPoly;
    }
}
