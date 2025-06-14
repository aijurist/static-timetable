#!/bin/bash

# Set the classpath to include all jars in the lib directory and our compiled classes
CLASSPATH="target/classes:lib/*"

# Run the department demo
java -cp "$CLASSPATH" org.timetable.DepartmentDemo 