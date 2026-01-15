package com.jihll;

import java.util.List;

abstract class Stmt {
    static class Expression extends Stmt {
        final Expr expression;
        Expression(Expr expression) { this.expression = expression; }
    }
    static class Print extends Stmt {
        final Expr expression;
        Print(Expr expression) { this.expression = expression; }
    }
    static class Return extends Stmt {
        final Token keyword;
        final Expr value;
        Return(Token keyword, Expr value) { this.keyword = keyword; this.value = value; }
    }
    static class Function extends Stmt {
        final Token name;
        final List<Token> params;
        final List<Stmt> body;
        Function(Token name, List<Token> params, List<Stmt> body) {
            this.name = name; this.params = params; this.body = body;
        }
    }
    static class Block extends Stmt {
        final List<Stmt> statements;
        Block(List<Stmt> statements) { this.statements = statements; }
    }
    static class If extends Stmt {
        final Expr condition;
        final Stmt thenBranch;
        final Stmt elseBranch;
        If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
            this.condition = condition; this.thenBranch = thenBranch; this.elseBranch = elseBranch;
        }
    }
    static class While extends Stmt {
        final Expr condition;
        final Stmt body;
        While(Expr condition, Stmt body) { this.condition = condition; this.body = body; }
    }
    static class Import extends Stmt {
        final Token keyword;
        final Expr file;
        Import(Token keyword, Expr file) { this.keyword = keyword; this.file = file; }
    }
}