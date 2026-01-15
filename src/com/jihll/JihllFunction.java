package com.jihll;

class JihllFunction {
    final String name;
    final int arity;
    final int address;
    final Chunk chunk;

    JihllFunction(String name, int arity, int address, Chunk chunk) {
        this.name = name;
        this.arity = arity;
        this.address = address;
        this.chunk = chunk;
    }

    @Override
    public String toString() { return "<fn " + name + ">"; }
}