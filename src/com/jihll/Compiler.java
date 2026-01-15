package com.jihll;

import java.util.List;

class Compiler {
    private final Chunk chunk;

    Compiler(Chunk chunk) { this.chunk = chunk; }

    void compile(List<Stmt> statements) {
        for (Stmt statement : statements) compile(statement);
        chunk.write(Op.RETURN); 
    }

    private void compile(Stmt stmt) {
        if (stmt instanceof Stmt.Print) {
            compile(((Stmt.Print) stmt).expression);
            chunk.write(Op.PRINT);
        } else if (stmt instanceof Stmt.Expression) {
            Expr expr = ((Stmt.Expression) stmt).expression;
            compile(expr);
            chunk.write(Op.POP); // Consume the value (or null from spawn)
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
            
            // Bind args
            for (int i = func.params.size() - 1; i >= 0; i--) {
                int paramNameIdx = chunk.addConstant(func.params.get(i).lexeme);
                chunk.write(Op.SET_GLOBAL); 
                chunk.write(paramNameIdx);
                chunk.write(Op.POP); 
            }
            
            // Implicit Return Logic
            boolean hasReturn = false;
            for (int i = 0; i < func.body.size(); i++) {
                Stmt s = func.body.get(i);
                boolean isLast = i == func.body.size() - 1;
                if (isLast && s instanceof Stmt.Expression) {
                    compile(((Stmt.Expression)s).expression);
                    chunk.write(Op.RETURN);
                    hasReturn = true;
                } else if (isLast && s instanceof Stmt.If) {
                    compileIfAsReturn((Stmt.If) s);
                    hasReturn = true;
                } else {
                    compile(s);
                }
            }
            
            if (!hasReturn) {
                chunk.write(Op.CONSTANT);
                chunk.write(chunk.addConstant(null));
                chunk.write(Op.RETURN);
            }

            int endAddress = chunk.code.size();
            chunk.code.set(jumpIdx, endAddress - jumpIdx - 1);

            JihllFunction fnObj = new JihllFunction(func.name.lexeme, func.params.size(), startAddress, chunk);
            int constIdx = chunk.addConstant(fnObj);
            chunk.write(Op.CONSTANT);
            chunk.write(constIdx);
            
            int nameIdx = chunk.addConstant(func.name.lexeme);
            chunk.write(Op.SET_GLOBAL); 
            chunk.write(nameIdx);
            chunk.write(Op.POP); 

        } else if (stmt instanceof Stmt.If) {
            Stmt.If ifStmt = (Stmt.If) stmt;
            compile(ifStmt.condition);
            chunk.write(Op.JUMP_IF_FALSE);
            chunk.write(0xff); 
            int elseJump = chunk.code.size() - 1;
            compile(ifStmt.thenBranch);
            chunk.write(Op.JUMP);
            chunk.write(0xff);
            int endJump = chunk.code.size() - 1;
            chunk.code.set(elseJump, chunk.code.size() - 1 - elseJump);
            if (ifStmt.elseBranch != null) compile(ifStmt.elseBranch);
            chunk.code.set(endJump, chunk.code.size() - 1 - endJump);
        } else if (stmt instanceof Stmt.While) {
            Stmt.While whileStmt = (Stmt.While) stmt;
            int loopStart = chunk.code.size();
            compile(whileStmt.condition);
            chunk.write(Op.JUMP_IF_FALSE);
            chunk.write(0xff);
            int exitJump = chunk.code.size() - 1;
            compile(whileStmt.body);
            chunk.write(Op.JUMP);
            chunk.write(loopStart - (chunk.code.size() + 1));
            chunk.code.set(exitJump, chunk.code.size() - 1 - exitJump);
        } else if (stmt instanceof Stmt.Return) {
            Stmt.Return ret = (Stmt.Return) stmt;
            if (ret.value != null) compile(ret.value);
            else {
                int nullIdx = chunk.addConstant(null);
                chunk.write(Op.CONSTANT);
                chunk.write(nullIdx);
            }
            chunk.write(Op.RETURN);
        } else if (stmt instanceof Stmt.Block) {
             for (Stmt s : ((Stmt.Block) stmt).statements) compile(s);
        }
    }

    private void compileIfAsReturn(Stmt.If ifStmt) {
        compile(ifStmt.condition);
        chunk.write(Op.JUMP_IF_FALSE);
        chunk.write(0xff);
        int elseJump = chunk.code.size() - 1;

        compileReturnBranch(ifStmt.thenBranch);

        chunk.write(Op.JUMP);
        chunk.write(0xff);
        int endJump = chunk.code.size() - 1;

        chunk.code.set(elseJump, chunk.code.size() - 1 - elseJump);

        if (ifStmt.elseBranch != null) {
            compileReturnBranch(ifStmt.elseBranch);
        } else {
            int nullIdx = chunk.addConstant(null);
            chunk.write(Op.CONSTANT);
            chunk.write(nullIdx);
            chunk.write(Op.RETURN);
        }

        chunk.code.set(endJump, chunk.code.size() - 1 - endJump);
    }

    private void compileReturnBranch(Stmt stmt) {
        if (stmt instanceof Stmt.Block) {
            List<Stmt> statements = ((Stmt.Block) stmt).statements;
            if (statements.isEmpty()) {
                int nullIdx = chunk.addConstant(null);
                chunk.write(Op.CONSTANT);
                chunk.write(nullIdx);
                chunk.write(Op.RETURN);
                return;
            }

            for (int i = 0; i < statements.size(); i++) {
                Stmt s = statements.get(i);
                boolean isLast = i == statements.size() - 1;
                if (isLast) {
                    compileReturnBranch(s);
                } else {
                    compile(s);
                }
            }
            return;
        }

        if (stmt instanceof Stmt.Expression) {
            compile(((Stmt.Expression) stmt).expression);
            chunk.write(Op.RETURN);
        } else if (stmt instanceof Stmt.Return) {
            compile(stmt);
        } else if (stmt instanceof Stmt.If) {
            compileIfAsReturn((Stmt.If) stmt);
        } else {
            compile(stmt);
            int nullIdx = chunk.addConstant(null);
            chunk.write(Op.CONSTANT);
            chunk.write(nullIdx);
            chunk.write(Op.RETURN);
        }
    }

    private void compile(Expr expr) {
        if (expr instanceof Expr.Assign) {
            Expr.Assign assign = (Expr.Assign) expr;
            compile(assign.value);
            int nameIdx = chunk.addConstant(assign.name.lexeme);
            chunk.write(Op.SET_GLOBAL);
            chunk.write(nameIdx);
        } else if (expr instanceof Expr.Spawn) {
            Expr inner = ((Expr.Spawn)expr).expression;
            if (inner instanceof Expr.Call) {
                Expr.Call call = (Expr.Call) inner;
                compile(call.callee);
                for (Expr arg : call.arguments) compile(arg);
                chunk.write(Op.SPAWN);
                chunk.write(call.arguments.size());
            } else {
                throw new RuntimeException("Spawn must call a function.");
            }
        } else if (expr instanceof Expr.Call) {
            Expr.Call call = (Expr.Call) expr;
            compile(call.callee);
            for (Expr arg : call.arguments) compile(arg);
            chunk.write(Op.CALL);
            chunk.write(call.arguments.size());
        } else if (expr instanceof Expr.Literal) {
            int index = chunk.addConstant(((Expr.Literal) expr).value);
            chunk.write(Op.CONSTANT);
            chunk.write(index);
        } else if (expr instanceof Expr.Variable) {
            int nameIdx = chunk.addConstant(((Expr.Variable) expr).name.lexeme);
            chunk.write(Op.GET_GLOBAL);
            chunk.write(nameIdx);
        } else if (expr instanceof Expr.Array) {
            Expr.Array array = (Expr.Array) expr;
            for (Expr element : array.elements) compile(element);
            chunk.write(Op.BUILD_LIST);
            chunk.write(array.elements.size());
        } else if (expr instanceof Expr.MapLiteral) {
            Expr.MapLiteral map = (Expr.MapLiteral) expr;
            for (int i = 0; i < map.keys.size(); i++) {
                compile(map.keys.get(i));
                compile(map.values.get(i));
            }
            chunk.write(Op.BUILD_MAP);
            chunk.write(map.keys.size());
        } else if (expr instanceof Expr.Binary) {
            Expr.Binary binary = (Expr.Binary) expr;
            compile(binary.left);
            compile(binary.right);
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