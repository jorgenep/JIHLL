package com.jihll;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.net.URI;
import java.net.http.*;

public class JihllLanguage {
    private static final VM vm = new VM();

    public static void main(String[] args) throws IOException {
        vm.defineNative("print", (a) -> { System.out.println(a[0]); return null; });
        vm.defineNative("clock", (a) -> (double) System.currentTimeMillis() / 1000.0);
        vm.defineNative("sleep", (a) -> { try{Thread.sleep(((Double)a[0]).longValue());}catch(Exception e){} return null; });
        vm.defineNative("len", (a) -> {
            if(a[0] instanceof String)return (double)((String)a[0]).length();
            if(a[0] instanceof List)return (double)((List)a[0]).size();
            return 0.0; 
        });

        vm.defineNative("jsonParse", (a) -> JsonUtils.parse((String)a[0]));
        vm.defineNative("jsonStringify", (a) -> JsonUtils.stringify(a[0]));

        vm.defineNative("split", (a) -> Arrays.asList(((String)a[0]).split((String)a[1])));
        vm.defineNative("replace", (a) -> ((String)a[0]).replace((String)a[1], (String)a[2]));
        vm.defineNative("trim", (a) -> ((String)a[0]).trim());

        vm.defineNative("httpGet", (a) -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(a[0].toString())).build();
                return client.send(req, HttpResponse.BodyHandlers.ofString()).body();
            } catch (Exception e) { return null; }
        });

        vm.defineNative("readFile", (a) -> {
            try {
                return Files.readString(Paths.get(a[0].toString()));
            } catch (IOException e) {
                throw new RuntimeException("Unable to read file: " + a[0]);
            }
        });
        vm.defineNative("appendFile", (a) -> {
            try {
                Files.writeString(Paths.get(a[0].toString()), a[1].toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                return true;
            } catch (IOException e) {
                return false;
            }
        });
        vm.defineNative("writeFile", (a) -> { try{Files.writeString(Paths.get(a[0].toString()), a[1].toString());return true;}catch(IOException e){return false;} });

        if (args.length == 1) runFile(args[0]); else runPrompt();
    }

    private static void runFile(String path) throws IOException { run(Files.readString(Paths.get(path))); }
    private static void runPrompt() {
        Scanner s = new Scanner(System.in);
        System.out.println("JillLanguage v2.0 (Stack VM)");
        while(true) { System.out.print("> "); if(!s.hasNextLine())break; run(s.nextLine()); }
    }
    private static void run(String source) {
        new VM().interpret(new Compiler(new Chunk()).chunk); 
        
        Lexer l = new Lexer(source);
        Parser p = new Parser(l.scanTokens());
        Chunk c = new Chunk();
        new Compiler(c).compile(p.parse());
        vm.interpret(c);
    }
    
    static class JsonUtils {
        static Object parse(String json) {
            json = json.trim();
            if (json.startsWith("{")) return new HashMap<>();
            if (json.startsWith("[")) return new ArrayList<>();
            return json;
        }
        
        static String stringify(Object obj) {
            if (obj instanceof Map) {
                StringBuilder sb = new StringBuilder("{");
                Map<?,?> m = (Map<?,?>)obj;
                int i=0;
                for(Map.Entry<?,?> e : m.entrySet()) {
                    if(i++ > 0) sb.append(", ");
                    sb.append(e.getKey()).append(": ").append(stringify(e.getValue()));
                }
                return sb.append("}").toString();
            }
            if (obj instanceof List) {
                return obj.toString();
            }
            return String.valueOf(obj);
        }
    }
}