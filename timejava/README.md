# Timetable Generator

This is a Java-based timetable generator for educational institutions. It can generate timetables for students and teachers based on various constraints.

## Features

- Generate timetables for student groups and teachers
- Support for different types of lessons (theory, lab, tutorial)
- Support for different departments and years
- HTML output for easy viewing

## Prerequisites

- Java 11 or higher
- Maven (optional, for building from source)

## Quick Start

1. Compile the code:
   ```
   ./compile.sh
   ```

2. Run the standalone timetable generator:
   ```
   ./run-standalone-timetable.sh
   ```

3. Check the generated timetables in the `output` directory:
   - Student timetables: `output/student_timetables/`
   - Teacher timetables: `output/teacher_timetables/`

## Available Scripts

- `compile.sh`: Compiles the Java code
- `run-standalone-timetable.sh`: Runs the standalone timetable generator
- `run-timeslot-demo.sh`: Runs a demo showing the time slots
- `run-department-demo.sh`: Runs a demo showing the departments

## Project Structure

- `src/main/java/org/timetable/`: Main source code
  - `domain/`: Domain classes (TimeSlot, Room, Teacher, Course, StudentGroup, Lesson)
  - `solver/`: OptaPlanner solver classes
  - `util/`: Utility classes
  - `visualization/`: Timetable visualization classes
- `lib/`: External libraries
- `output/`: Generated timetables

## Customization

To customize the timetable generation, you can modify the following classes:

- `StandaloneTimetableGenerator.java`: Main class for generating timetables
- `TimetableConstants.java`: Constants for the timetable generation (days, times, departments)

## License

This project is licensed under the MIT License - see the LICENSE file for details. 