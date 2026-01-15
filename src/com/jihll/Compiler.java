package com.jihll;

import java.util.ArrayList;
import java.util.List;

class Compiler {
    public final Chunk chunk;
    private final List<Local> locals = new ArrayList<>();
    private int scopeDepth = 0;

    private static class Local {
        String name;
        int depth;
        Local(String name, int depth) { this.name = name; this.depth = depth; }
    }

    Compiler(Chunk chunk) { this.chunk = chunk; }

    void compile(List<Stmt> statements) {
        for (Stmt statement : statements) compile(statement);
        chunk.write(Op.CONSTANT); chunk.write(chunk.addConstant(null));
        chunk.write(Op.RETURN); 
    }

    private int resolveLocal(String name) {
        for (int i = locals.size() - 1; i >= 0; i--) {
            if (locals.get(i).name.equals(name)) return i;
        }
        return -1;
    }
    
    private void beginScope() { scopeDepth++; }
    
    private void endScope() {
        scopeDepth--;
        while (!locals.isEmpty() && locals.get(locals.size() - 1).depth > scopeDepth) {
            chunk.write(Op.POP);
            locals.remove(locals.size() - 1);
        }
    }
    
    private void addLocal(String name) { 
        locals.add(new Local(name, scopeDepth)); 
    }
    
    private void compile(Stmt stmt) {
        if (stmt instanceof Stmt.Print) {
            compile(((Stmt.Print) stmt).expression);
            chunk.write(Op.PRINT);
        } else if (stmt instanceof Stmt.Expression) {
            Expr expr = ((Stmt.Expression) stmt).expression;
            compile(expr);
            chunk.write(Op.POP);
        } else if (stmt instanceof Stmt.Class) {
            Stmt.Class classStmt = (Stmt.Class) stmt;
            int nameIdx = chunk.addConstant(classStmt.name.lexeme);
            chunk.write(Op.CLASS);
            chunk.write(nameIdx);
            
            for (Stmt.Function method : classStmt.methods) {
                int methodIdx = chunk.addConstant(method.name.lexeme);
                Stmt.Function func = method;
                chunk.write(Op.JUMP); chunk.write(0xff); int jumpIdx = chunk.code.size() - 1;
                int startAddress = chunk.code.size();
                
                beginScope();
                locals.clear();
                addLocal("this");
                
                for (int i = 0; i < func.params.size(); i++) {
                     addLocal(func.params.get(i).lexeme);
                }
                
                for (Stmt s : func.body) compile(s);
                chunk.write(Op.CONSTANT); chunk.write(chunk.addConstant(null)); chunk.write(Op.RETURN);

                int endAddress = chunk.code.size();
                chunk.code.set(jumpIdx, endAddress - jumpIdx - 1);
                
                JihllFunction methodFn = new JihllFunction(method.name.lexeme, method.params.size(), startAddress, this.chunk);
                chunk.write(Op.CONSTANT); chunk.write(chunk.addConstant(methodFn));
                chunk.write(Op.METHOD);
                chunk.write(methodIdx);
            }
            chunk.write(Op.POP); 

        } else if (stmt instanceof Stmt.Try) {
            Stmt.Try tryStmt = (Stmt.Try) stmt;
            chunk.write(Op.TRY_ENTER);
            chunk.write(0xff); int catchJump = chunk.code.size() - 1;
            
            compile(tryStmt.tryBlock);
            chunk.write(Op.TRY_EXIT); 
            chunk.write(Op.JUMP);
            chunk.write(0xff); int endJump = chunk.code.size() - 1;
            
            int catchAddr = chunk.code.size();
            chunk.code.set(catchJump, catchAddr - catchJump - 1);
            
            beginScope();
            addLocal(tryStmt.errorVar.lexeme); 
            
            compile(tryStmt.catchBlock);
            endScope();
            
            chunk.code.set(endJump, chunk.code.size() - 1 - endJump);

        } else if (stmt instanceof Stmt.Import) {
            Stmt.Import imp = (Stmt.Import) stmt;
            compile(imp.file);
            chunk.write(Op.IMPORT);
        } else if (stmt instanceof Stmt.Function) {
            Stmt.Function func = (Stmt.Function) stmt;
            chunk.write(Op.JUMP);
            chunk.write(0xff); 
            int jumpIdx = chunk.code.size() - 1;
            int startAddress = chunk.code.size();
            
            List<Local> savedLocals = new ArrayList<>(locals);
            locals.clear();
            
            for (int i = 0; i < func.params.size(); i++) {
                addLocal(func.params.get(i).lexeme); 
            }
            
            boolean hasReturn = false;
            for (int i = 0; i < func.body.size(); i++) {
                Stmt s = func.body.get(i);
                if (i == func.body.size() - 1) {
                    if (s instanceof Stmt.Expression) {
                        compile(((Stmt.Expression)s).expression);
                        chunk.write(Op.RETURN);
                        hasReturn = true;
                    } else if (s instanceof Stmt.If) {
                        compileIfExpression((Stmt.If) s);
                        chunk.write(Op.RETURN);
                        hasReturn = true;
                    } else {
                        compile(s);
                    }
                } else {
                    compile(s);
                }
            }
            
            if (!hasReturn) {
                chunk.write(Op.CONSTANT); chunk.write(chunk.addConstant(null)); chunk.write(Op.RETURN);
            }
            
            locals.clear();
            locals.addAll(savedLocals);

            int endAddress = chunk.code.size();
            chunk.code.set(jumpIdx, endAddress - jumpIdx - 1);

            JihllFunction fnObj = new JihllFunction(func.name.lexeme, func.params.size(), startAddress, this.chunk);
            int constIdx = chunk.addConstant(fnObj);
            chunk.write(Op.CONSTANT); chunk.write(constIdx);
            
            int nameIdx = chunk.addConstant(func.name.lexeme);
            chunk.write(Op.SET_GLOBAL); chunk.write(nameIdx); chunk.write(Op.POP); 

        } else if (stmt instanceof Stmt.If) {
            Stmt.If ifStmt = (Stmt.If) stmt;
            compile(ifStmt.condition);
            chunk.write(Op.JUMP_IF_FALSE); chunk.write(0xff); int elseJump = chunk.code.size() - 1;
            compile(ifStmt.thenBranch);
            chunk.write(Op.JUMP); chunk.write(0xff); int endJump = chunk.code.size() - 1;
            chunk.code.set(elseJump, chunk.code.size() - 1 - elseJump);
            if (ifStmt.elseBranch != null) compile(ifStmt.elseBranch);
            chunk.code.set(endJump, chunk.code.size() - 1 - endJump);
        } else if (stmt instanceof Stmt.While) {
            Stmt.While whileStmt = (Stmt.While) stmt;
            int loopStart = chunk.code.size();
            compile(whileStmt.condition);
            chunk.write(Op.JUMP_IF_FALSE); chunk.write(0xff); int exitJump = chunk.code.size() - 1;
            compile(whileStmt.body);
            chunk.write(Op.JUMP); chunk.write(loopStart - (chunk.code.size() + 1));
            chunk.code.set(exitJump, chunk.code.size() - 1 - exitJump);
        } else if (stmt instanceof Stmt.Return) {
            Stmt.Return ret = (Stmt.Return) stmt;
            if (ret.value != null) compile(ret.value);
            else {
                chunk.write(Op.CONSTANT); chunk.write(chunk.addConstant(null));
            }
            chunk.write(Op.RETURN);
        } else if (stmt instanceof Stmt.Block) {
             beginScope();
             for (Stmt s : ((Stmt.Block) stmt).statements) compile(s);
             endScope();
        }
    }

    private void compileIfExpression(Stmt.If ifStmt) {
        compile(ifStmt.condition);
        chunk.write(Op.JUMP_IF_FALSE); chunk.write(0xff); int elseJump = chunk.code.size() - 1;
        compileBlockValue((Stmt.Block) ifStmt.thenBranch);
        chunk.write(Op.JUMP); chunk.write(0xff); int endJump = chunk.code.size() - 1;
        chunk.code.set(elseJump, chunk.code.size() - 1 - elseJump);
        if (ifStmt.elseBranch != null) {
            compileBlockValue((Stmt.Block) ifStmt.elseBranch);
        } else {
            chunk.write(Op.CONSTANT); chunk.write(chunk.addConstant(null));
        }
        chunk.code.set(endJump, chunk.code.size() - 1 - endJump);
    }

    private void compileBlockValue(Stmt.Block block) {
        List<Stmt> stmts = block.statements;
        if (stmts.isEmpty()) {
            chunk.write(Op.CONSTANT); chunk.write(chunk.addConstant(null));
            return;
        }
        for (int i = 0; i < stmts.size(); i++) {
            Stmt s = stmts.get(i);
            if (i == stmts.size() - 1) {
                if (s instanceof Stmt.Expression) {
                    compile(((Stmt.Expression) s).expression);
                } else if (s instanceof Stmt.If) {
                    compileIfExpression((Stmt.If) s);
                } else {
                    compile(s);
                    chunk.write(Op.CONSTANT); chunk.write(chunk.addConstant(null));
                }
            } else {
                compile(s);
            }
        }
    }

    private void compile(Expr expr) {
        if (expr instanceof Expr.Assign) {
            Expr.Assign assign = (Expr.Assign) expr;
            compile(assign.value);
            
            int arg = resolveLocal(assign.name.lexeme);
            if (arg != -1) {
                chunk.write(Op.SET_LOCAL);
                chunk.write(arg);
            } else {
                int nameIdx = chunk.addConstant(assign.name.lexeme);
                chunk.write(Op.SET_GLOBAL);
                chunk.write(nameIdx);
            }
        } else if (expr instanceof Expr.Variable) {
            String name = ((Expr.Variable) expr).name.lexeme;
            int arg = resolveLocal(name);
            if (arg != -1) {
                chunk.write(Op.GET_LOCAL);
                chunk.write(arg);
            } else {
                int nameIdx = chunk.addConstant(name);
                chunk.write(Op.GET_GLOBAL);
                chunk.write(nameIdx);
            }
        } else if (expr instanceof Expr.Set) {
            Expr.Set set = (Expr.Set) expr;
            compile(set.object); 
            compile(set.value); 
            int nameIdx = chunk.addConstant(set.name.lexeme);
            chunk.write(Op.SET_PROPERTY);
            chunk.write(nameIdx);
        } else if (expr instanceof Expr.Get) {
            Expr.Get get = (Expr.Get) expr;
            compile(get.object);
            int nameIdx = chunk.addConstant(get.name.lexeme);
            chunk.write(Op.GET_PROPERTY);
            chunk.write(nameIdx);
        } else if (expr instanceof Expr.Spawn) {
            Expr inner = ((Expr.Spawn)expr).expression;
            if (inner instanceof Expr.Call) {
                Expr.Call call = (Expr.Call) inner;
                compile(call.callee);
                for (Expr arg : call.arguments) compile(arg);
                chunk.write(Op.SPAWN);
                chunk.write(call.arguments.size());
            } else { throw new RuntimeException("Spawn must call a function."); }
        } else if (expr instanceof Expr.Call) {
            Expr.Call call = (Expr.Call) expr;
            compile(call.callee);
            for (Expr arg : call.arguments) compile(arg);
            chunk.write(Op.CALL);
            chunk.write(call.arguments.size());
        } else if (expr instanceof Expr.Literal) {
            int index = chunk.addConstant(((Expr.Literal) expr).value);
            chunk.write(Op.CONSTANT); chunk.write(index);
        } else if (expr instanceof Expr.This) {
             chunk.write(Op.GET_LOCAL);
             chunk.write(0);
        } else if (expr instanceof Expr.Array) {
            Expr.Array array = (Expr.Array) expr;
            for (Expr element : array.elements) compile(element);
            chunk.write(Op.BUILD_LIST); chunk.write(array.elements.size());
        } else if (expr instanceof Expr.MapLiteral) {
            Expr.MapLiteral map = (Expr.MapLiteral) expr;
            for (int i = 0; i < map.keys.size(); i++) {
                compile(map.keys.get(i)); compile(map.values.get(i));
            }
            chunk.write(Op.BUILD_MAP); chunk.write(map.keys.size());
        } else if (expr instanceof Expr.Binary) {
            Expr.Binary binary = (Expr.Binary) expr;
            compile(binary.left); compile(binary.right);
            switch (binary.operator.type) {
                case PLUS:  chunk.write(Op.ADD); break;
                case MINUS: chunk.write(Op.SUBTRACT); break;
                case STAR:  chunk.write(Op.MULTIPLY); break;
                case SLASH: chunk.write(Op.DIVIDE); break;
                case LESS:  chunk.write(Op.LESS); break;
                case GREATER: chunk.write(Op.GREATER); break;
                case EQUAL_EQUAL: chunk.write(Op.EQUAL); break;
                case LESS_EQUAL: chunk.write(Op.LESS_EQUAL); break;
                case GREATER_EQUAL: chunk.write(Op.GREATER_EQUAL); break;
                case BANG_EQUAL: chunk.write(Op.NOT_EQUAL); break;
            }
        }
    }
}