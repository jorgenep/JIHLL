package com.jihll;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

class VM {
    private final Object[] stack = new Object[256]; 
    private int sp = 0; 
    
    // Global Scope
    public final Map<String, Object> globals = new HashMap<>();
    
    public Chunk chunk;
    public int ip = 0;

    // Call Stack
    static class Frame {
        final Chunk chunk;
        final int ip;
        final Map<String, Object> variables = new HashMap<>(); 
        Frame(Chunk chunk, int ip) { this.chunk = chunk; this.ip = ip; }
    }
    private final Stack<Frame> frames = new Stack<>();

    void defineNative(String name, NativeMethod method) { globals.put(name, method); }

    void interpret(Chunk chunk) {
        this.chunk = chunk;
        this.ip = 0;
        run();
    }
    
    public void push(Object value) { 
        if (sp >= stack.length) throw new RuntimeException("Stack Overflow");
        stack[sp++] = value; 
    }
    
    public Object pop() {
        if (sp <= 0) throw new RuntimeException("Stack Underflow");
        return stack[--sp];
    }
    
    // Helper to peek without popping
    public Object peek() {
        if (sp <= 0) return null;
        return stack[sp - 1];
    }

    private void run() {
        while (ip < chunk.code.size()) {
            int instruction = readByte();
            switch (instruction) {
                case Op.RETURN:
                    if (frames.isEmpty()) return; // End of program
                    Frame frame = frames.pop();
                    this.chunk = frame.chunk;
                    this.ip = frame.ip;
                    break;
                
                case Op.CONSTANT:
                    int constantIndex = readByte();
                    push(chunk.constants.get(constantIndex));
                    break;
                    
                case Op.PRINT: System.out.println(pop()); break;
                case Op.POP: pop(); break; 
                
                case Op.SET_GLOBAL: {
                    String name = (String) chunk.constants.get(readByte());
                    Object val = peek(); // Assignment expressions evaluate to the value
                    
                    if (!frames.isEmpty()) {
                        // We are in a function. 
                        // 1. If local exists, update it.
                        // 2. If global exists, update it.
                        // 3. Else, define NEW LOCAL.
                        if (frames.peek().variables.containsKey(name)) {
                            frames.peek().variables.put(name, val);
                        } else if (globals.containsKey(name)) {
                            globals.put(name, val);
                        } else {
                            frames.peek().variables.put(name, val);
                        }
                    } else {
                        // We are in global scope.
                        globals.put(name, val);
                    }
                    break;
                }
                    
                case Op.GET_GLOBAL: {
                    String name = (String) chunk.constants.get(readByte());
                    if (!frames.isEmpty() && frames.peek().variables.containsKey(name)) {
                        push(frames.peek().variables.get(name));
                    } else if (globals.containsKey(name)) {
                        push(globals.get(name));
                    } else {
                        throw new RuntimeException("Undefined var '" + name + "'");
                    }
                    break;
                }
                
                case Op.ADD: {
                    Object b = pop(); Object a = pop();
                    if (a instanceof String || b instanceof String) push(String.valueOf(a) + String.valueOf(b));
                    else push(toDouble(a) + toDouble(b));
                    break;
                }
                case Op.SUBTRACT: push(toDouble(pop(), pop(), (a, b) -> a - b)); break;
                case Op.MULTIPLY: push(toDouble(pop(), pop(), (a, b) -> a * b)); break;
                case Op.DIVIDE: push(toDouble(pop(), pop(), (a, b) -> a / b)); break;
                case Op.LESS: push(toDouble(pop(), pop(), (a, b) -> (a < b) ? 1.0 : 0.0)); break;
                case Op.GREATER: push(toDouble(pop(), pop(), (a, b) -> (a > b) ? 1.0 : 0.0)); break;
                case Op.EQUAL: {
                     Object b = pop(); Object a = pop();
                     if (a == null) push(b == null);
                     else push(a.equals(b));
                     break;
                }
                case Op.NOT_EQUAL: {
                    Object b = pop(); Object a = pop();
                    if (a == null) push(b != null);
                    else push(!a.equals(b));
                    break;
                }
                case Op.LESS_EQUAL: {
                    Object b = pop(); Object a = pop();
                    push(toDouble(a) <= toDouble(b) ? 1.0 : 0.0);
                    break;
                }
                case Op.GREATER_EQUAL: {
                    Object b = pop(); Object a = pop();
                    push(toDouble(a) >= toDouble(b) ? 1.0 : 0.0);
                    break;
                }

                case Op.JUMP_IF_FALSE: {
                    int offset = readByte();
                    if (isFalsey(pop())) ip += offset;
                    break;
                }
                case Op.JUMP: {
                    int offset = readByte();
                    ip += offset;
                    break;
                }
                
                case Op.BUILD_LIST: {
                    int count = readByte();
                    List<Object> list = new ArrayList<>();
                    for (int i = 0; i < count; i++) list.add(null);
                    for (int i = count - 1; i >= 0; i--) list.set(i, pop());
                    push(list);
                    break;
                }

                case Op.BUILD_MAP: {
                    int count = readByte();
                    Map<Object, Object> map = new HashMap<>();
                    for (int i = 0; i < count; i++) {
                        Object val = pop();
                        Object key = pop();
                        map.put(key, val);
                    }
                    push(map);
                    break;
                }
                
                case Op.IMPORT: {
                    String filename = pop().toString();
                    try {
                        String source = Files.readString(Paths.get(filename));
                        Lexer lexer = new Lexer(source);
                        List<Token> tokens = lexer.scanTokens();
                        Parser parser = new Parser(tokens);
                        List<Stmt> statements = parser.parse();
                        
                        Chunk moduleChunk = new Chunk();
                        Compiler compiler = new Compiler(moduleChunk);
                        compiler.compile(statements);
                        
                        // Execute module in GLOBAL context (no new frame)
                        Chunk prevChunk = this.chunk;
                        int prevIp = this.ip;
                        this.chunk = moduleChunk;
                        this.ip = 0;
                        run();
                        this.chunk = prevChunk;
                        this.ip = prevIp;
                        
                    } catch (IOException e) {
                        throw new RuntimeException("Could not import: " + filename);
                    }
                    break;
                }

                case Op.SPAWN: {
                    int argCount = readByte();
                    Object[] args = new Object[argCount];
                    // Pop args (reverse order)
                    for (int i = argCount - 1; i >= 0; i--) args[i] = pop();
                    
                    Object callee = pop(); 

                    new Thread(() -> {
                        VM threadVM = new VM();
                        threadVM.globals.putAll(this.globals);

                        if (callee instanceof NativeMethod) {
                            ((NativeMethod) callee).invoke(args);
                        } else if (callee instanceof JihllFunction) {
                            JihllFunction fn = (JihllFunction) callee;
                            // Push args onto NEW stack
                            for (Object arg : args) threadVM.push(arg);
                            threadVM.chunk = fn.chunk;
                            threadVM.ip = fn.address;
                            try {
                                threadVM.run();
                            } catch (Exception e) {
                                System.err.println("Thread Error: " + e.getMessage());
                            }
                        }
                    }).start();
                    
                    push(null); // Return null to the spawner so POP doesn't fail
                    break;
                }
                
                case Op.CALL:
                    int argCount = readByte();
                    Object[] args = new Object[argCount];
                    for (int i = argCount - 1; i >= 0; i--) args[i] = pop();
                    
                    Object callee = pop(); 
                    
                    if (callee instanceof NativeMethod) {
                        push(((NativeMethod) callee).invoke(args));
                    } else if (callee instanceof JihllFunction) {
                        JihllFunction fn = (JihllFunction) callee;
                        // Push args back for function
                        for (Object arg : args) push(arg);
                        frames.push(new Frame(this.chunk, this.ip));
                        this.chunk = fn.chunk;
                        this.ip = fn.address;
                    }
                    break;
            }
        }
    }

    private int readByte() { return chunk.code.get(ip++); }
    
    private double toDouble(Object a) {
        if (a instanceof Double) return (Double) a;
        if (a instanceof Integer) return ((Integer) a).doubleValue();
        throw new RuntimeException("Expected number");
    }
    private Object toDouble(Object b, Object a, java.util.function.DoubleBinaryOperator op) {
        return op.applyAsDouble(toDouble(a), toDouble(b));
    }
    private boolean isFalsey(Object o) {
        if (o == null) return true;
        if (o instanceof Boolean) return !(Boolean)o;
        if (o instanceof Double) return (Double)o == 0.0;
        return false;
    }
}