# Timetable Scheduler with OptaPlanner

This project is a Java implementation of a timetable scheduling system using OptaPlanner. It migrates the functionality from a Python-based solution using OR-Tools to a Java-based solution using OptaPlanner.

## Project Structure

The project follows the standard Maven directory structure:

```
timejava/
├── src/
│   └── main/
│       └── java/
│           └── org/
│               └── timetable/
│                   ├── domain/       # Domain classes
│                   ├── persistence/  # Data loading and exporting
│                   ├── solver/       # OptaPlanner constraint definitions
│                   └── TimetableApp.java  # Main application
├── pom.xml         # Maven configuration
└── README.md       # This file
```

## Requirements

- Java 11 or higher
- Maven 3.6 or higher

## Building the Project

To build the project, run the following command in the project root directory:

```
mvn clean package
```

This will create a JAR file with all dependencies in the `target` directory.

## Running the Application

There are several ways to run the application:

### Using Maven

If you have Maven installed, you can use the provided shell script:

```
./run.sh [courses_file] [data_dir]
```

### Using Makefile

If you don't have Maven but have Java installed, you can use the Makefile:

```
# Download required libraries
./download-libs.sh

# Run the application
make run
```

### Using Convenience Scripts

For convenience, we've provided two scripts:

```
# To run the main application
./run-app.sh

# To run a data loading test
./run-test.sh
```

## Parameters

- `[courses_file]` is the path to the CSV file containing course and teacher data (default: `data/courses/cse_dept_red.csv`)
- `[data_dir]` is the path to the directory containing all data files (default: `data`)

## Data Directory Structure

The application expects the following directory structure for data:

```
data/
├── classroom/     # CSV files containing classroom data
├── courses/       # CSV files containing course and teacher data
└── labs/          # CSV files containing lab room data
```

## Input Data Format

### Courses CSV

The courses CSV file should have the following columns:
- `teacher_id`: Unique identifier for the teacher
- `first_name`: Teacher's first name
- `last_name`: Teacher's last name
- `teacher_email`: Teacher's email address
- `course_id`: Unique identifier for the course
- `course_code`: Course code
- `course_name`: Course name
- `course_dept`: Department offering the course
- `lecture_hours`: Number of lecture hours per week
- `practical_hours`: Number of practical/lab hours per week
- `tutorial_hours`: Number of tutorial hours per week
- `credits`: Number of credits for the course

### Rooms CSV

The rooms CSV files should have the following columns:
- `id`: Unique identifier for the room
- `room_number`: Room number/name
- `block`: Building block where the room is located
- `description`: Description of the room
- `is_lab`: Whether the room is a lab (1) or a regular classroom (0)
- `room_max_cap`: Maximum capacity of the room

## Output

The application generates the following output files:
- `output/timetable_solution_YYYYMMDD_HHMMSS.csv`: Complete timetable solution
- `output/teacher_timetables/`: Individual timetables for each teacher
- `output/student_timetables/`: Individual timetables for each student group

## Implementation Details

This implementation uses OptaPlanner's Constraint Streams API to define constraints for the timetable problem. The main constraints include:

### Hard Constraints
- Room conflict: A room can accommodate at most one lesson at the same time
- Teacher conflict: A teacher can teach at most one lesson at the same time
- Student group conflict: A student group can attend at most one lesson at the same time
- Room capacity: A room's capacity should be sufficient for all lessons taught in it
- Lab session in lab room: Lab sessions should be held in lab rooms
- Theory session in theory room: Theory sessions should be held in theory rooms

### Soft Constraints
- Teacher room stability: A teacher should teach in the same room on the same day
- Theory sessions on different days: Theory sessions of the same course for the same student group should be on different days
- Avoid late classes: Avoid scheduling classes late in the day 