#!/bin/bash

# Create the output directory if it doesn't exist
mkdir -p target/classes

# Find all jar files in the lib directory
CLASSPATH="lib/*"

# Find all Java files and compile them
find src/main/java -name "*.java" -print > sources.txt
javac -d target/classes -cp "$CLASSPATH" @sources.txt
rm sources.txt

# Copy resources to the target directory
mkdir -p target/classes/org/timetable/solver
cp -r src/main/resources/* target/classes/

echo "Compilation completed. Use run-compiled.sh to run the application." 