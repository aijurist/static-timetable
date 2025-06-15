#!/bin/bash

echo "=========================================="
echo "CS DEPARTMENT TIMETABLE GENERATOR"
echo "=========================================="
echo "Generating timetables for 6 CS departments with room/lab information:"
echo "- Computer Science & Design (CSD)"
echo "- Computer Science & Engineering (CSE)" 
echo "- Computer Science & Engineering (Cyber Security) (CSE-CS)"
echo "- Computer Science & Business Systems (CSBS)"
echo "- Information Technology (IT)"
echo "- Artificial Intelligence & Machine Learning (AIML)"
echo

# Clean previous CS timetables
echo "Cleaning previous CS timetables..."
rm -f output/student_timetables/timetable_CSD_*.html
rm -f output/student_timetables/timetable_CSE_*.html
rm -f output/student_timetables/timetable_CSE-CS_*.html
rm -f output/student_timetables/timetable_CSBS_*.html
rm -f output/student_timetables/timetable_IT_*.html
rm -f output/student_timetables/timetable_AIML_*.html

rm -f output/teacher_timetables/timetable_teacher_CSD_*.html
rm -f output/teacher_timetables/timetable_teacher_CSE_*.html
rm -f output/teacher_timetables/timetable_teacher_CSE-CS_*.html
rm -f output/teacher_timetables/timetable_teacher_CSBS_*.html
rm -f output/teacher_timetables/timetable_teacher_IT_*.html
rm -f output/teacher_timetables/timetable_teacher_AIML_*.html

# Compile the CS generator
echo "Compiling CS Timetable Generator..."
javac -d target/classes src/main/java/org/timetable/generator/CSETimetableGenerator.java

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

# Run the CS generator
echo "Running CS Timetable Generator..."
java -cp target/classes org.timetable.generator.CSETimetableGenerator

echo
echo "=========================================="
echo "CS TIMETABLE GENERATION COMPLETED!"
echo "=========================================="
echo "Generated timetables with room/lab information for:"
echo "- Student timetables: output/student_timetables/"
echo "- Teacher timetables: output/teacher_timetables/"
echo
echo "To view the timetables, run:"
echo "./run-department-gui.sh"
echo "==========================================" 