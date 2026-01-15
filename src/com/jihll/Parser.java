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
        if (match(TokenType.CLASS)) return classDeclaration();
        if (match(TokenType.FUN)) return functionDeclaration();
        return statement();
    }

    private Stmt classDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Expect class name.");
        consume(TokenType.COLON, "Expect ':' before class body.");
        
        List<Stmt.Function> methods = new ArrayList<>();
        while (!check(TokenType.BLOCK_DOT) && !isAtEnd()) {
            consume(TokenType.FUN, "Expect 'fun' in class body.");
            methods.add((Stmt.Function) functionDeclaration());
        }
        consume(TokenType.BLOCK_DOT, "Expect '.' after class body.");
        return new Stmt.Class(name, methods);
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
        if (match(TokenType.TRY)) return tryStatement();
        if (match(TokenType.IF)) return ifStatement();
        if (match(TokenType.WHILE)) return whileStatement();
        if (match(TokenType.RETURN)) return returnStatement();
        if (match(TokenType.IMPORT)) return importStatement();
        if (match(TokenType.SPAWN)) return spawnStatement();
        return expressionStatement();
    }
    
    // FIX: tryStatement now manually parses until CATCH, instead of using parseBlock()
    private Stmt tryStatement() {
        consume(TokenType.COLON, "Expect ':' after try.");
        
        List<Stmt> tryStmts = new ArrayList<>();
        // Keep parsing statements until we hit 'catch' or EOF
        while (!check(TokenType.CATCH) && !isAtEnd()) {
            tryStmts.add(declaration());
        }
        Stmt tryBlock = new Stmt.Block(tryStmts);
        
        consume(TokenType.CATCH, "Expect 'catch' after try block.");
        Token errorVar = consume(TokenType.IDENTIFIER, "Expect error variable name.");
        consume(TokenType.COLON, "Expect ':' after catch.");
        
        // The catch block DOES end with a dot, so we can use parseBlock() here
        Stmt catchBlock = new Stmt.Block(parseBlock());
        
        return new Stmt.Try(tryBlock, errorVar, catchBlock);
    }
    
    private Stmt spawnStatement() {
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
           if (!check(TokenType.BLOCK_DOT) && !check(TokenType.EOF)) {
             value = expression();
        }
        return new Stmt.Return(keyword, value);
    }

    private Stmt ifStatement() {
        Expr condition = expression();
        consume(TokenType.COLON, "Expect ':' after if condition.");
        
        List<Stmt> thenStmts = new ArrayList<>();
        List<Stmt> elseStmts = null;

        while (!check(TokenType.ELSE) && !check(TokenType.BLOCK_DOT) && !isAtEnd()) {
            thenStmts.add(declaration());
        }

        if (match(TokenType.ELSE)) {
            consume(TokenType.COLON, "Expect ':' after else.");
            elseStmts = new ArrayList<>();
            while (!check(TokenType.BLOCK_DOT) && !isAtEnd()) {
                elseStmts.add(declaration());
            }
        }
        consume(TokenType.BLOCK_DOT, "Expect '.' after if block.");
        return new Stmt.If(condition, new Stmt.Block(thenStmts), (elseStmts != null) ? new Stmt.Block(elseStmts) : null);
    }
    
    private Stmt whileStatement() {
        Expr condition = expression();
        consume(TokenType.COLON, "Expect ':' after while condition.");
        List<Stmt> body = parseBlock();
        return new Stmt.While(condition, new Stmt.Block(body));
    }

    private List<Stmt> parseBlock() {
        List<Stmt> statements = new ArrayList<>();
        while (!check(TokenType.BLOCK_DOT) && !isAtEnd()) {
            statements.add(declaration());
        }
        consume(TokenType.BLOCK_DOT, "Expect '.' to close block.");
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
            } else if (expr instanceof Expr.Get) {
                Expr.Get get = (Expr.Get)expr;
                return new Expr.Set(get.object, get.name, value);
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
            } 
            else if (check(TokenType.DOT) && peekNext().type == TokenType.IDENTIFIER) {
                advance(); // Consume dot
                Token name = consume(TokenType.IDENTIFIER, "Expect property name after '.'.");
                expr = new Expr.Get(expr, name);
            }
            else {
                break;
            }
        }
        return expr;
    }
    
    private Expr primary() {
        if (match(TokenType.FALSE)) return new Expr.Literal(false);
        if (match(TokenType.TRUE)) return new Expr.Literal(true);
        if (match(TokenType.THIS)) return new Expr.This(previous());
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
    
    private Token peekNext() {
        if (current + 1 >= tokens.size()) return tokens.get(tokens.size() - 1);
        return tokens.get(current + 1);
    }
    
    private Token advance() { if (!isAtEnd()) current++; return previous(); }
    private boolean isAtEnd() { return peek().type == TokenType.EOF; }
    private Token peek() { return tokens.get(current); }
    private Token previous() { return tokens.get(current - 1); }
}