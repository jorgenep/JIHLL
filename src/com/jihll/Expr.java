package com.jihll;

import java.util.List;

abstract class Expr {
    static class Assign extends Expr {
        final Token name;
        final Expr value;
        Assign(Token name, Expr value) { this.name = name; this.value = value; }
    }
    static class Binary extends Expr {
        final Expr left;
        final Token operator;
        final Expr right;
        Binary(Expr left, Token operator, Expr right) {
            this.left = left; this.operator = operator; this.right = right;
        }
    }
    static class Literal extends Expr {
        final Object value;
        Literal(Object value) { this.value = value; }
    }
    static class Variable extends Expr {
        final Token name;
        Variable(Token name) { this.name = name; }
    }
    static class Call extends Expr {
        final Expr callee;
        final List<Expr> arguments;
        Call(Expr callee, List<Expr> arguments) {
            this.callee = callee; this.arguments = arguments;
        }
    }
    static class Array extends Expr {
        final List<Expr> elements;
        Array(List<Expr> elements) { this.elements = elements; }
    }
    static class MapLiteral extends Expr {
        final List<Expr> keys;
        final List<Expr> values;
        MapLiteral(List<Expr> keys, List<Expr> values) {
            this.keys = keys; this.values = values;
        }
    }
    static class Spawn extends Expr {
        final Expr expression;
        Spawn(Expr expression) { this.expression = expression; }
    }
}