#!/bin/bash

# Set the classpath to include all jars in the lib directory and our compiled classes
CLASSPATH="target/classes:lib/*"

# Run the GUI timetable viewer
java -cp "$CLASSPATH" org.timetable.gui.TimetableViewer 