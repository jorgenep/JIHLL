# 🐧 Linux Setup Guide for JIHLL

Welcome to the JIHLL setup guide for Linux users. Follow these steps to get your environment ready for running and developing with JIHLL.

## Prerequisites

Ensure you have the Java Development Kit (JDK) installed (version 11 or higher).

### 1. Check for Java
Open your terminal and run:
```bash
java -version
```
If you see output like `openjdk version "11..."` (or higher), you are good to go.

### 2. Install Java (if not installed)
If java is not found, install it using your package manager.

**Debian/Ubuntu:**
```bash
sudo apt update
sudo apt install default-jdk
```

**Fedora:**
```bash
sudo dnf install java-latest-openjdk
```

**Arch Linux:**
```bash
sudo pacman -S jdk-openjdk
```

## Compilation

1. **Stay in the project root:**
   ```bash
   cd /path/to/JIHLL
   ```

2. **Compile the source code:**
   ```bash
   /usr/lib/jvm/java-11-openjdk-amd64/bin/javac -d bin ./src/com/jihll/*.java
   ```
   This will compile all `.java` files in the `com/jihll` package.

## Verification

To verify the installation, run the interactive editor:
```bash
java -cp bin com.jihll.JihllLanguage
```
You should see:
```text
JIHLL Shell (Type 'exit' to quit)
> 
```

You are now ready to use JIHLL on Linux! See [README_RUNNING.md](README_RUNNING.md) for usage instructions.
