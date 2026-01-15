package com.jihll;

import java.util.ArrayList;
import java.util.List;

class Parser {
    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) { this.tokens = tokens; }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    private Stmt declaration() {
        if (match(TokenType.FUN)) return functionDeclaration();
        return statement();
    }

    private Stmt functionDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Expect function name.");
        List<Token> parameters = new ArrayList<>();
        if (!check(TokenType.COLON)) {
            do {
                parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name."));
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.COLON, "Expect ':' before function body.");
        List<Stmt> body = parseBlock();
        return new Stmt.Function(name, parameters, body);
    }

    private Stmt statement() {
        if (match(TokenType.PRINT)) return printStatement();
        if (match(TokenType.IF)) return ifStatement();
        if (match(TokenType.WHILE)) return whileStatement();
        if (match(TokenType.RETURN)) return returnStatement();
        if (match(TokenType.IMPORT)) return importStatement();
        
        // Fix: Explicitly handle SPAWN as a statement start
        if (match(TokenType.SPAWN)) return spawnStatement();
        
        return expressionStatement();
    }
    
    private Stmt spawnStatement() {
        // "spawn worker(1)" -> parse "worker(1)" as expression
        Expr callExpr = expression(); 
        return new Stmt.Expression(new Expr.Spawn(callExpr));
    }
    
    private Stmt importStatement() {
        Token keyword = previous();
        Expr file = expression();
        return new Stmt.Import(keyword, file);
    }

    private Stmt printStatement() {
        Expr value = expression();
        return new Stmt.Print(value);
    }
    
    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(TokenType.DOT) && !check(TokenType.EOF)) {
             value = expression();
        }
        return new Stmt.Return(keyword, value);
    }

    private Stmt ifStatement() {
        Expr condition = expression();
        consume(TokenType.COLON, "Expect ':' after if condition.");
        
        List<Stmt> thenStmts = new ArrayList<>();
        List<Stmt> elseStmts = null;

        while (!check(TokenType.ELSE) && !check(TokenType.DOT) && !isAtEnd()) {
            thenStmts.add(declaration());
        }

        if (match(TokenType.ELSE)) {
            consume(TokenType.COLON, "Expect ':' after else.");
            elseStmts = new ArrayList<>();
            while (!check(TokenType.DOT) && !isAtEnd()) {
                elseStmts.add(declaration());
            }
        }
        
        consume(TokenType.DOT, "Expect '.' after if block.");

        return new Stmt.If(condition, new Stmt.Block(thenStmts), 
                           (elseStmts != null) ? new Stmt.Block(elseStmts) : null);
    }
    
    private Stmt whileStatement() {
        Expr condition = expression();
        consume(TokenType.COLON, "Expect ':' after while condition.");
        List<Stmt> body = parseBlock();
        return new Stmt.While(condition, new Stmt.Block(body));
    }

    private List<Stmt> parseBlock() {
        List<Stmt> statements = new ArrayList<>();
        while (!check(TokenType.DOT) && !isAtEnd()) {
            statements.add(declaration());
        }
        consume(TokenType.DOT, "Expect '.' to close block.");
        return statements;
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        return new Stmt.Expression(expr);
    }

    private Expr expression() { return assignment(); }

    private Expr assignment() {
        Expr expr = or();
        if (match(TokenType.EQUAL)) {
            Expr value = assignment();
            if (expr instanceof Expr.Variable) {
                return new Expr.Assign(((Expr.Variable)expr).name, value);
            }
            throw new RuntimeException("Invalid assignment target.");
        }
        return expr;
    }

    private Expr or() { return equality(); } 
    private Expr equality() { return comparison(); }

    private Expr comparison() {
        Expr expr = term();
        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL, TokenType.EQUAL_EQUAL, TokenType.BANG_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr term() {
        Expr expr = factor();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr factor() {
        Expr expr = call(); 
        while (match(TokenType.STAR, TokenType.SLASH)) {
            Token operator = previous();
            Expr right = call();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr call() {
        Expr expr = primary();

        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                List<Expr> args = new ArrayList<>();
                if (!check(TokenType.RIGHT_PAREN)) {
                    do { args.add(expression()); } while (match(TokenType.COMMA));
                }
                consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.");
                expr = new Expr.Call(expr, args);
            } else {
                break;
            }
        }
        return expr;
    }

    private Expr primary() {
        if (match(TokenType.FALSE)) return new Expr.Literal(false);
        if (match(TokenType.TRUE)) return new Expr.Literal(true);
        if (match(TokenType.NUMBER)) return new Expr.Literal(previous().literal);
        if (match(TokenType.STRING)) return new Expr.Literal(previous().literal);
        if (match(TokenType.IDENTIFIER)) return new Expr.Variable(previous());

        if (match(TokenType.LEFT_BRACKET)) {
            List<Expr> elements = new ArrayList<>();
            if (!check(TokenType.RIGHT_BRACKET)) {
                do { elements.add(expression()); } while (match(TokenType.COMMA));
            }
            consume(TokenType.RIGHT_BRACKET, "Expect ']'");
            return new Expr.Array(elements);
        }

        if (match(TokenType.LEFT_BRACE)) {
            List<Expr> keys = new ArrayList<>();
            List<Expr> values = new ArrayList<>();
            if (!check(TokenType.RIGHT_BRACE)) {
                do {
                    if (match(TokenType.IDENTIFIER)) {
                        keys.add(new Expr.Literal(previous().lexeme));
                    } else {
                        keys.add(expression());
                    }
                    consume(TokenType.COLON, "Expect ':' after map key.");
                    values.add(expression());
                } while (match(TokenType.COMMA));
            }
            consume(TokenType.RIGHT_BRACE, "Expect '}'");
            return new Expr.MapLiteral(keys, values);
        }

        if (match(TokenType.LEFT_PAREN)) {
            Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expect ')'");
            return expr;
        }
        throw new RuntimeException("Expect expression.");
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) { advance(); return true; }
        }
        return false;
    }
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw new RuntimeException(message);
    }
    private boolean check(TokenType type) { return !isAtEnd() && peek().type == type; }
    private Token advance() { if (!isAtEnd()) current++; return previous(); }
    private boolean isAtEnd() { return peek().type == TokenType.EOF; }
    private Token peek() { return tokens.get(current); }
    private Token previous() { return tokens.get(current - 1); }
}