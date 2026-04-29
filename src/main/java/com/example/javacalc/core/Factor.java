package com.example.javacalc.core;

public interface Factor {
    public Poly toPoly();

    // 重写equals方法
    @Override
    public boolean equals(Object obj);

    // 重写hascode方法
    @Override
    public int hashCode();
}


