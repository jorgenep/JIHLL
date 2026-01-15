package com.jihll;

enum TokenType {
    // Keywords
    IF, ELSE,
    WHILE, FOR, IN,
    FUN, RETURN, SPAWN,
    IMPORT, // New Keyword
    TRUE, FALSE, 
    PRINT,

    // Structure
    COLON, DOT,
    COMMA,
    LEFT_PAREN, RIGHT_PAREN,
    LEFT_BRACKET, RIGHT_BRACKET,
    LEFT_BRACE, RIGHT_BRACE,
    
    // Operators
    EQUAL,
    PLUS, MINUS, STAR, SLASH,
    EQUAL_EQUAL, BANG, BANG_EQUAL,
    LESS, LESS_EQUAL, GREATER, GREATER_EQUAL,
    
    // Literals
    IDENTIFIER, STRING, NUMBER,
    
    EOF
}