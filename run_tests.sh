#!/bin/bash

# Compile first to be safe
echo "Compiling JIHLL..."
/usr/lib/jvm/java-11-openjdk-amd64/bin/javac -d bin ./src/com/jihll/*.java

if [ $? -ne 0 ]; then
    echo "Compilation Failed!"
    exit 1
fi

echo "--------------------------------------"
echo "Running Verification Suite"
echo "--------------------------------------"

# Define the Java Command
JAVA_CMD="/usr/lib/jvm/java-11-openjdk-amd64/bin/java -cp /home/elijahjorgensen/FocusNexus/bin com.jihll.JihllLanguage"

# Run each test
$JAVA_CMD tests/test_core.jihll
echo ""
$JAVA_CMD tests/test_functions.jihll
echo ""
$JAVA_CMD tests/test_data.jihll
echo ""
$JAVA_CMD tests/test_io.jihll
echo ""
$JAVA_CMD tests/test_concurrency.jihll
echo ""
$JAVA_CMD tests/test_modules.jihll

echo "--------------------------------------"
echo "Cleaning up..."
rm test_file.txt 2>/dev/null
echo "Done."