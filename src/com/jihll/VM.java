package com.jihll;

import java.util.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

class VM {
    private final Object[] stack = new Object[1024];
    private int sp = 0; 
    public final Map<String, Object> globals = new HashMap<>();
    public Chunk chunk;
    public int ip = 0;

    static class Frame {
        final Chunk chunk;
        final int ip;
        final int fp;
        final Integer catchAddress;
        final Object returnOverride;
        Frame(Chunk chunk, int ip, int fp, Integer catchAddress, Object returnOverride) {
            this.chunk = chunk; this.ip = ip; this.fp = fp; this.catchAddress = catchAddress; this.returnOverride = returnOverride;
        }
    }
    private final Stack<Frame> frames = new Stack<>();
    private int fp = 0;
    private Integer currentCatchAddress = null;

    void defineNative(String name, NativeMethod method) { globals.put(name, method); }

    void interpret(Chunk chunk) {
        this.chunk = chunk;
        this.ip = 0;
        this.fp = 0;
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
    public Object peek() { if (sp <= 0) return null; return stack[sp - 1]; }

    private void run() {
        while (ip < chunk.code.size()) {
            int instruction = readByte();
            try {
                switch (instruction) {
                    case Op.RETURN:
                        Object retVal = pop();
                        if (frames.isEmpty()) return;
                        Frame frame = frames.pop();
                        if (frame.returnOverride != null) retVal = frame.returnOverride;

                        this.sp = this.fp - 1; // discard callee slot
                        this.chunk = frame.chunk;
                        this.ip = frame.ip;
                        this.fp = frame.fp;
                        this.currentCatchAddress = frame.catchAddress;

                        push(retVal);
                        break;
                        
                    case Op.CONSTANT: push(chunk.constants.get(readByte())); break;
                    case Op.PRINT: System.out.println(pop()); break;
                    case Op.POP: pop(); break; 
                    
                    case Op.GET_LOCAL: {
                        int slot = readByte();
                        push(stack[fp + slot]);
                        break;
                    }
                    case Op.SET_LOCAL: {
                        int slot = readByte();
                        stack[fp + slot] = peek();
                        break;
                    }
                    
                    case Op.SET_GLOBAL: {
                        String name = (String) chunk.constants.get(readByte());
                        globals.put(name, peek());
                        break;
                    }
                    case Op.GET_GLOBAL: {
                        String name = (String) chunk.constants.get(readByte());
                        if (globals.containsKey(name)) push(globals.get(name));
                        else throw new RuntimeException("Undefined global '" + name + "'");
                        break;
                    }
                    
                    case Op.CLASS: {
                        String name = (String) chunk.constants.get(readByte());
                        JihllClass klass = new JihllClass(name);
                        globals.put(name, klass);
                        push(klass);
                        break;
                    }
                    case Op.METHOD: {
                        String name = (String) chunk.constants.get(readByte());
                        JihllFunction method = (JihllFunction) pop();
                        JihllClass klass = (JihllClass) peek();
                        klass.methods.put(name, method);
                        break;
                    }
                    case Op.GET_PROPERTY: {
                        String name = (String) chunk.constants.get(readByte());
                        Object obj = pop();
                        if (obj instanceof JihllInstance) {
                            JihllInstance inst = (JihllInstance) obj;
                            if (inst.fields.containsKey(name)) {
                                push(inst.fields.get(name));
                            } else {
                                JihllFunction method = inst.klass.findMethod(name);
                                if (method == null) throw new RuntimeException("Undefined property '" + name + "'.");
                                push(new JihllBoundMethod(inst, method));
                            }
                        } else { throw new RuntimeException("Only instances have properties."); }
                        break;
                    }
                    case Op.SET_PROPERTY: {
                        String name = (String) chunk.constants.get(readByte());
                        Object val = pop();
                        Object obj = pop();
                        if (obj instanceof JihllInstance) {
                            ((JihllInstance)obj).fields.put(name, val);
                            push(val);
                        } else { throw new RuntimeException("Only instances have fields."); }
                        break;
                    }
                    
                    case Op.TRY_ENTER: currentCatchAddress = ip + readByte(); break;
                    case Op.TRY_EXIT: currentCatchAddress = null; break;

                    case Op.ADD: { Object b = pop(); Object a = pop(); if(a instanceof String || b instanceof String) push(""+a+b); else push(toDouble(a)+toDouble(b)); break; }
                    case Op.SUBTRACT: push(toDouble(pop(), pop(), (a, b) -> a - b)); break;
                    case Op.MULTIPLY: push(toDouble(pop(), pop(), (a, b) -> a * b)); break;
                    case Op.DIVIDE: push(toDouble(pop(), pop(), (a, b) -> a / b)); break;
                    case Op.LESS: push(toDouble(pop(), pop(), (a, b) -> (a < b) ? 1.0 : 0.0)); break;
                    case Op.GREATER: push(toDouble(pop(), pop(), (a, b) -> (a > b) ? 1.0 : 0.0)); break;
                    case Op.EQUAL: push(Objects.equals(pop(), pop())); break;
                    case Op.LESS_EQUAL: push(toDouble(pop(), pop(), (a, b) -> (a <= b) ? 1.0 : 0.0)); break;
                    case Op.GREATER_EQUAL: push(toDouble(pop(), pop(), (a, b) -> (a >= b) ? 1.0 : 0.0)); break;
                    case Op.NOT_EQUAL: push(!Objects.equals(pop(), pop())); break;
                    case Op.JUMP_IF_FALSE: { int offset = readByte(); if (isFalsey(pop())) ip += offset; break; }
                    case Op.JUMP: { int offset = readByte(); ip += offset; break; }
                    
                    case Op.BUILD_LIST: { int c = readByte(); List<Object> l = new ArrayList<>(); for(int i=0;i<c;i++) l.add(null); for(int i=c-1;i>=0;i--) l.set(i, pop()); push(l); break; }
                    case Op.BUILD_MAP: { int c = readByte(); Map<Object,Object> m = new HashMap<>(); for(int i=0;i<c;i++) { Object v=pop(); Object k=pop(); m.put(k,v); } push(m); break; }
                    
                    case Op.IMPORT: {
                        String filename = pop().toString();
                        String source = Files.readString(Paths.get(filename));
                        Lexer l = new Lexer(source);
                        Parser p = new Parser(l.scanTokens());
                        Chunk mc = new Chunk();
                        new Compiler(mc).compile(p.parse());
                        Chunk pc = this.chunk; int pip = this.ip; int pfp = this.fp;
                        this.chunk = mc; this.ip = 0;
                        run();
                        this.chunk = pc; this.ip = pip; this.fp = pfp;
                        break;
                    }

                    case Op.SPAWN: {
                        int argCount = readByte(); Object[] args = new Object[argCount];
                        for(int i=argCount-1;i>=0;i--) args[i]=pop();
                        Object callee = pop();
                        new Thread(() -> {
                            VM threadVM = new VM(); threadVM.chunk = this.chunk; threadVM.globals.putAll(this.globals);
                            if(callee instanceof NativeMethod) ((NativeMethod)callee).invoke(args);
                            else if(callee instanceof JihllFunction) {
                                JihllFunction fn=(JihllFunction)callee;
                                threadVM.chunk = fn.chunk; 
                                for(Object arg:args) threadVM.push(arg); 
                                threadVM.ip=fn.address;
                                try { threadVM.run(); } catch(Exception e) { System.err.println("Thread Error: "+e); }
                            }
                        }).start();
                        push(null); break;
                    }
                    
                    case Op.CALL:
                        int argCount = readByte();
                        Object callee = stack[sp - 1 - argCount];

                        if (callee instanceof JihllBoundMethod) {
                            JihllBoundMethod bound = (JihllBoundMethod) callee;
                            argCount = bindReceiver(argCount, bound.receiver, bound.method);
                            callee = bound.method;
                        }
                        
                        if (callee instanceof NativeMethod) {
                            Object[] args = new Object[argCount];
                            for(int i=argCount-1;i>=0;i--) args[i]=pop();
                            pop();
                            push(((NativeMethod)callee).invoke(args));
                        }
                        else if (callee instanceof JihllClass) {
                            JihllClass klass = (JihllClass) callee;
                            JihllInstance instance = new JihllInstance(klass);
                            JihllFunction init = klass.findMethod("init");
                            if (init == null) {
                                for(int i=0;i<argCount;i++) pop();
                                pop();
                                push(instance);
                            } else {
                                argCount = bindReceiver(argCount, instance, init);
                                frames.push(new Frame(this.chunk, this.ip, this.fp, this.currentCatchAddress, instance));
                                this.fp = sp - argCount;
                                this.chunk = init.chunk;
                                this.ip = init.address;
                            }
                        }
                        else if (callee instanceof JihllFunction) {
                            JihllFunction fn = (JihllFunction)callee;

                            frames.push(new Frame(this.chunk, this.ip, this.fp, this.currentCatchAddress, null));
                            this.fp = sp - argCount;
                            this.chunk = fn.chunk;
                            this.ip = fn.address;
                        }
                        break;
                }
            } catch (Exception e) {
                if (currentCatchAddress != null) {
                    push(e.getMessage()); ip = currentCatchAddress; currentCatchAddress = null;
                } else throw new RuntimeException(e);
            }
        }
    }

    private int readByte() { return chunk.code.get(ip++); }
    private double toDouble(Object a) {
        if(a instanceof Double)return(Double)a; if(a instanceof Integer)return((Integer)a).doubleValue(); throw new RuntimeException("Expected number");
    }
    private Object toDouble(Object b, Object a, java.util.function.DoubleBinaryOperator op) { return op.applyAsDouble(toDouble(a), toDouble(b)); }
    private boolean isFalsey(Object o) { return o==null || (o instanceof Boolean && !(Boolean)o) || (o instanceof Double && (Double)o==0.0); }

    private int bindReceiver(int argCount, Object receiver, Object newCallee) {
        int calleeIndex = sp - 1 - argCount;
        stack[calleeIndex] = newCallee;
        if (sp >= stack.length) throw new RuntimeException("Stack Overflow");
        for (int i = sp - 1; i >= calleeIndex + 1; i--) {
            stack[i + 1] = stack[i];
        }
        stack[calleeIndex + 1] = receiver;
        sp++;
        return argCount + 1;
    }
}