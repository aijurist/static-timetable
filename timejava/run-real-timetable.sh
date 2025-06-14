#!/bin/bash

# Set the classpath to include all jars in the lib directory and our compiled classes
CLASSPATH="target/classes:lib/*"

# Run the real timetable generator
java -cp "$CLASSPATH" org.timetable.RealTimetableGenerator 