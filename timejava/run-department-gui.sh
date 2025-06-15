#!/bin/bash

echo "Starting Department Timetable Viewer..."
echo "This GUI organizes timetables by department in sorted order"
echo

# Compile the GUI if needed
if [ ! -f "target/classes/org/timetable/gui/DepartmentTimetableViewer.class" ]; then
    echo "Compiling Department Timetable Viewer..."
    javac -d target/classes src/main/java/org/timetable/gui/DepartmentTimetableViewer.java
    if [ $? -ne 0 ]; then
        echo "Compilation failed!"
        exit 1
    fi
fi

# Check if timetables exist
if [ ! -d "output/student_timetables" ] && [ ! -d "output/teacher_timetables" ]; then
    echo "No timetables found. Generating timetables first..."
    java -cp target/classes org.timetable.generator.StandaloneTimetableGenerator
fi

# Run the GUI
echo "Launching Department Timetable Viewer..."
java -cp target/classes org.timetable.gui.DepartmentTimetableViewer 