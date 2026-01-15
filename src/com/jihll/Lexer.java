package com.jihll;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Lexer {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("if", TokenType.IF);
        keywords.put("else", TokenType.ELSE);
        keywords.put("while", TokenType.WHILE);
        keywords.put("for", TokenType.FOR);
        keywords.put("in", TokenType.IN);
        keywords.put("fun", TokenType.FUN);
        keywords.put("return", TokenType.RETURN);
        keywords.put("spawn", TokenType.SPAWN);
        keywords.put("true", TokenType.TRUE);
        keywords.put("false", TokenType.FALSE);
        keywords.put("print", TokenType.PRINT);
        keywords.put("import", TokenType.IMPORT);
    }

    Lexer(String source) { this.source = source; }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", null));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case ':': addToken(TokenType.COLON); break;
            case '.': addToken(TokenType.DOT); break;
            case '(': addToken(TokenType.LEFT_PAREN); break;
            case ')': addToken(TokenType.RIGHT_PAREN); break;
            case '[': addToken(TokenType.LEFT_BRACKET); break;
            case ']': addToken(TokenType.RIGHT_BRACKET); break;
            case '{': addToken(TokenType.LEFT_BRACE); break;  // Map Start
            case '}': addToken(TokenType.RIGHT_BRACE); break; // Map End
            case ',': addToken(TokenType.COMMA); break;
            
            case '+': addToken(TokenType.PLUS); break;
            case '-': addToken(TokenType.MINUS); break;
            case '*': addToken(TokenType.STAR); break;
            case '/': addToken(TokenType.SLASH); break;
            
            case '=': addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL); break;
            case '!': addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG); break;
            case '<': addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS); break;
            case '>': addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER); break;
            
            case '#': while (peek() != '\n' && !isAtEnd()) advance(); break;

            case ' ':
            case '\r':
            case '\t':
            case '\n': break;

            case '"': string(); break;

            default:
                if (isDigit(c)) number();
                else if (isAlpha(c)) identifier();
                else throw new RuntimeException("Unexpected character: " + c);
        }
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();
        String text = source.substring(start, current);
        TokenType type = keywords.getOrDefault(text, TokenType.IDENTIFIER);
        addToken(type);
    }

    private void number() {
        while (isDigit(peek())) advance();
        if (peek() == '.' && isDigit(peekNext())) {
            advance(); while (isDigit(peek())) advance();
        }
        addToken(TokenType.NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) advance();
        if (isAtEnd()) throw new RuntimeException("Unterminated string.");
        advance();
        addToken(TokenType.STRING, source.substring(start + 1, current - 1));
    }

    private boolean match(char expected) {
        if (isAtEnd() || source.charAt(current) != expected) return false;
        current++; return true;
    }
    private char peek() { return isAtEnd() ? '\0' : source.charAt(current); }
    private char peekNext() { return (current + 1 >= source.length()) ? '\0' : source.charAt(current + 1); }
    private boolean isAlpha(char c) { return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_'; }
    private boolean isAlphaNumeric(char c) { return isAlpha(c) || isDigit(c); }
    private boolean isDigit(char c) { return c >= '0' && c <= '9'; }
    private boolean isAtEnd() { return current >= source.length(); }
    private char advance() { return source.charAt(current++); }
    private void addToken(TokenType type) { addToken(type, null); }
    private void addToken(TokenType type, Object literal) { tokens.add(new Token(type, source.substring(start, current), literal)); }
}