# 🔮 JIHLL (Java Interpreted High-Level Language)

> **A modern, robust interpreted programming language built simply and efficiently.**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()
[![Java Version](https://img.shields.io/badge/java-11%2B-blue)](https://www.java.com)

---

## ✨ Overview

**JIHLL** (Java Interpreted High-Level Language) is a dynamically typed, feature-rich scripting language running on the Java Virtual Machine. Designed for clarity and ease of implementation, it offers a powerful set of tools for developers looking to explore language design or simply script efficiently.

The implementation includes a handwritten **Scanner**, **Recursive Descent Parser**, and a **Virtual Machine** (VM) for execution.

## 🚀 Features

JIHLL supports a wide array of modern programming constructs:

- **📦 Variables**: Dynamic typing with assignment (no `var` keyword).
- **🖨️ I/O**: Simple built-in `print` statements.
- **🔄 Control Flow**: `if`, `else`, `while` blocks using `:` and `.`.
- **⚡ Functions**: First-class functions with `fun` and `return`.
- **🧮 Native Methods**: Built-ins like `clock()`, `sqrt(n)`, `len(x)`, `sleep(ms)`, and file I/O helpers.
- **📝 Comments**: `#` line comments.

## 🛠️ Getting Started

We have dedicated setup guides for every major platform. Choose yours to get started in minutes!

| Platform | Setup Guide |
|----------|-------------|
| 🐧 **Linux** | [View Linux Setup](README_LINUX.md) |
| 🍎 **macOS** | [View macOS Setup](README_MAC.md) |
| 🪟 **Windows** | [View Windows Setup](README_WINDOWS.md) |

## 📖 Usage

Once installed, there are two ways to use JIHLL.

### 1. Interactive Shell (REPL)
Dive straight in and test your ideas.
```bash
$ java -cp bin com.jihll.JihllLanguage
> print "Hello World"
Hello World
```

### 2. Script Execution
Write your logic in a file and run it.
```javascript
# fib.jihll
fun fib n:
  if n <= 1:
    n
  else:
    fib(n - 2) + fib(n - 1)
  .
.

print fib(10)
```

For more details, check out the [**Running Guide**](README_RUNNING.md).

## 📂 Project Structure

- `src/com/jihll` - The core language implementation.
  - `Lexer.java` - Tokenizes source code.
  - `Parser.java` - Parses tokens into AST.
  - `Compiler.java` *&Ref;* `VM.java` - Compiles to bytecode and executes.

---
*Built with ❤️ by Elijah Jorgensen*
