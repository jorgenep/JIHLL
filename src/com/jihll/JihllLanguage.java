package com.jihll;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Scanner;

public class JihllLanguage {
    private static final VM vm = new VM();

    public static void main(String[] args) throws IOException {
        // --- 1. SYSTEM ---
        vm.defineNative("clock", (nArgs) -> (double) System.currentTimeMillis() / 1000.0);
        vm.defineNative("print", (nArgs) -> { System.out.println(nArgs[0]); return null; });
        vm.defineNative("input", (nArgs) -> new Scanner(System.in).nextLine());
        vm.defineNative("sleep", (nArgs) -> {
            try { Thread.sleep(((Double)nArgs[0]).longValue()); } catch(Exception e){} return null;
        });

        // --- 2. MATH & LISTS ---
        vm.defineNative("sqrt", (nArgs) -> Math.sqrt((Double) nArgs[0]));
        vm.defineNative("len", (nArgs) -> {
            if (nArgs[0] instanceof String) return (double)((String)nArgs[0]).length();
            if (nArgs[0] instanceof List) return (double)((List)nArgs[0]).size();
            if (nArgs[0] instanceof java.util.Map) return (double)((java.util.Map)nArgs[0]).size();
            return 0.0;
        });

        // --- 3. FILE I/O ---
        // readFile("path.txt") -> String
        vm.defineNative("readFile", (nArgs) -> {
            try { return Files.readString(Paths.get(nArgs[0].toString())); } 
            catch (IOException e) { return null; }
        });

        // writeFile("path.txt", "content") -> boolean
        vm.defineNative("writeFile", (nArgs) -> {
            try { Files.writeString(Paths.get(nArgs[0].toString()), nArgs[1].toString()); return true; } 
            catch (IOException e) { return false; }
        });

        // appendFile("path.txt", "content") -> boolean
        vm.defineNative("appendFile", (nArgs) -> {
            try {
                Files.write(Paths.get(nArgs[0].toString()), nArgs[1].toString().getBytes(), StandardOpenOption.APPEND);
                return true;
            } catch (IOException e) {
                return false;
            }
        });

        // --- STARTUP ---
        if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        String source = Files.readString(Paths.get(path));
        run(source);
    }

    private static void runPrompt() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("JIHLL Shell (Type 'exit' to quit)");
        while (true) {
            System.out.print("> ");
            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine();
            if (line.equals("exit")) break;
            try { run(line); } catch (Exception e) { System.out.println("Error: " + e.getMessage()); }
        }
    }

    private static void run(String source) {
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();
        Chunk chunk = new Chunk();
        Compiler compiler = new Compiler(chunk);
        compiler.compile(statements);
        vm.interpret(chunk);
    }
}