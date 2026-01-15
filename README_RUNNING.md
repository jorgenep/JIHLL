# 🏃 Running JIHLL

This guide covers the two modes of operation for the JIHLL language: **Interactive Mode (REPL)** and **File Execution**.

> **Note:** All commands assume you are in the project root and have compiled the project (see Setup guides).

## 1. Interactive Editor (REPL)

The Read-Eval-Print Loop (REPL) allows you to type code and see results immediately. It's great for testing small snippets.

**Command:**
```bash
java -cp bin com.jihll.JihllLanguage
```

**Usage:**
Once inside the shell, you can type JIHLL code.
```text
> print "Hello, World!"
Hello, World!
> x = 10
> print x * 2
20
```
Type `exit` to quit the shell.

## 2. Running Files

To run a JIHLL script file (conventionally `.jihll` extension, though any text file works), pass the file path as an argument.

**Command:**
```bash
java -cp bin com.jihll.JihllLanguage <path_to_script>
```

**Example:**

1. Create a file named `hello.jihll`:
   ```javascript
   # hello.jihll
      name = "Developer"
      print "Hello, " + name
   ```

2. Run it:
   ```bash
   java -cp bin com.jihll.JihllLanguage hello.jihll
   ```

## Native Functions

The language comes with built-in native functions you can use in either mode:

- `clock()`: Returns the current time in seconds.
- `sqrt(n)`: Returns the square root of number `n`.
- `len(x)`: Length of string/list/map.
- `sleep(ms)`: Sleep for milliseconds.
- `readFile(path)`, `writeFile(path, content)`, `appendFile(path, content)`.

```javascript
print clock();
print sqrt(16); // Prints 4.0
```
