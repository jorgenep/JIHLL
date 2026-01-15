# 🍎 macOS Setup Guide for JIHLL

Welcome to the JIHLL setup guide for macOS users.

## Prerequisites

You need the Java Development Kit (JDK) installed (version 11 or higher).

### 1. Check for Java
Open Terminal and run:
```bash
java -version
```

### 2. Install Java (if not installed)
The easiest way is using [Homebrew](https://brew.sh/).

```bash
brew install openjdk
```
*Note: You may need to symlink it as per Homebrew's post-install instructions.*

Alternatively, download a DMG installer from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [Eclipse Adoptium](https://adoptium.net/).

## Compilation

1. **Open Terminal** and navigate to the project root `FocusNexus`.

2. **Compile the source code:**
   ```bash
   javac -d bin ./src/com/jihll/*.java
   ```

## Verification

Start the interactive shell to ensure everything is working:
```bash
java -cp bin com.jihll.JihllLanguage
```
Expected output:
```text
JIHLL Shell (Type 'exit' to quit)
> 
```

You are ready! See [README_RUNNING.md](README_RUNNING.md) for how to write and run scripts.
