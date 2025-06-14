#!/bin/bash

# Set the classpath to include all jars in the lib directory and our compiled classes
CLASSPATH="target/classes:lib/*"

# Run the timetable generator
java -cp "$CLASSPATH" org.timetable.TimetableGenerator 