package com.jihll;

class Op {
    static final int RETURN = 0;
    static final int CONSTANT = 1;
    static final int ADD = 2;
    static final int SUBTRACT = 3;
    static final int MULTIPLY = 4;
    static final int DIVIDE = 5;
    static final int NEGATE = 6;
    static final int PRINT = 7;
    static final int POP = 8;
    static final int DEFINE_GLOBAL = 9;
    static final int GET_GLOBAL = 10;
    static final int SET_GLOBAL = 11;
    static final int JUMP_IF_FALSE = 12;
    static final int JUMP = 13;
    static final int CALL = 14;
    static final int LESS = 15;
    static final int GREATER = 16;
    static final int BUILD_LIST = 17;
    static final int EQUAL = 18;
    static final int SPAWN = 19;
    static final int BUILD_MAP = 20;
    static final int IMPORT = 21;
    static final int GET_LOCAL = 22;
    static final int SET_LOCAL = 23;
    static final int CLASS = 24;
    static final int GET_PROPERTY = 25;
    static final int SET_PROPERTY = 26;
    static final int METHOD = 27;
    static final int TRY_ENTER = 28;
    static final int TRY_EXIT = 29;
    static final int LESS_EQUAL = 30;
    static final int GREATER_EQUAL = 31;
    static final int NOT_EQUAL = 32;
}