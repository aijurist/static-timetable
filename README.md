# Timetable Scheduler with OptaPlanner


GIST: https://gist.github.com/aijurist/f6af8384bb6c6c21f96a02c72a1b397c
This project is a Java implementation of a timetable scheduling system using OptaPlanner. It uses a simplified configuration with the OptimizedTimetableConstraintProvider for efficient scheduling.

## Project Structure

The project follows the standard Maven directory structure:

```
timejava/
├── src/
│   └── main/
│       └── java/
│           └── org/
│               └── timetable/
│                   ├── domain/       # Domain classes (Lesson, Teacher, Course, etc.)
│                   ├── persistence/  # Data loading and exporting
│                   ├── solver/       # OptaPlanner constraint definitions
│                   ├── validation/   # Solution validation utilities
│                   ├── config/       # Configuration classes
│                   └── TimetableApp.java  # Main application
├── data/           # Input data files
├── config/         # Configuration files
├── pom.xml         # Maven configuration
└── README.md       # This file
```

## Requirements

- Java 11 or higher
- Maven 3.6 or higher

## Building the Project

To build the project, run the following command in the project root directory:

```bash
mvn clean package
```

This will create a JAR file with all dependencies in the `target` directory.

## Running the Application

### Quick start via helper script

For **Unix / macOS / WSL** users:
```bash
./run-app.sh [OPTIONS] [core|cse|both|custom]
```
For **Windows (PowerShell)** users:
```powershell
./run-app.ps1 [OPTIONS] [core|cse|both|custom]
```

Department selectors:
* `core`  – run only the core-department CSV (`data/courses/core_dept_red.csv`)
* `cse`   – run only the computer-science CSV (`data/courses/cse_dept_red.csv`)
* `both`  – run both CSVs (default)
* `custom` – supply one or more `-f/--file <csv>` arguments and the solver will use exactly those files.

Additional options (both scripts):
* `-m | --minutes <N>` – time limit in minutes (default 20 or `$SOLVER_MINUTES` env-var)
* `-f | --file <csv>`  – add a course CSV (only in `custom` mode)
* `-h | --help`        – show full usage

Examples:
```bash
# Core depts for 30 min
./run-app.sh core -m 30

# CSE only (Windows)
./run-app.ps1 cse

# Custom mix
./run-app.sh custom -f my_dept.csv -f electives.csv -m 10
```

### Running manually
You can still execute the fat-jar directly after building:
```bash
java -Dsolver.minutes=20 -jar target/timejava-1.0-SNAPSHOT.jar "data/courses/cse_dept_red.csv,data/courses/core_dept_red.csv" data
```

---

## Data Directory Layout (v1.1)
```
data/
├── courses/           # course CSVs (one per department mix)
├── labs/              # *lab* room CSVs (now include lab_type column)
├── classroom/         # regular classroom CSVs
└── config/
    └── course_lab_mapping.csv   # explicit course→lab mapping (optional)
```

### `lab_type` column
All lab CSVs now end with a `lab_type` column:
* `core`      – specialised core-department labs
* `computer`  – computer labs for CSE/IT

The scheduler enforces that:
1. Computer-dept courses must be placed in `computer` labs.
2. Core-dept courses prefer `core` labs, and may use `computer` labs only if the mapping file allows it.

### Course-lab mapping CSV
`data/config/course_lab_mapping.csv` (header shown below) lets you restrict **specific courses** to **specific lab descriptions**:
```
course_code,course_name,department,total_labs,lab_1,lab_2,lab_3
EE23521,Control and Instrumentation Laboratory,EEE,1,Control and Instrumentation Lab,,
AT19721,Vehicle Maintenance Laboratory,AUTO,1,Vehicle Maintenance Lab,,
...
```
If a course appears here the solver treats the list as *hard* constraints (cannot place the course elsewhere).

---

## Output
Same as before, but room labels for *core labs* are exported using their **description** (e.g. `"Vehicle Maintenance Lab"`) instead of the numeric `room_number` for better readability in CSV/JSON.

---

## Constraint Highlights (v1.1)
New hard constraints:
* **courseLabMustMatchMapping** – Courses listed in the mapping CSV must be in an allowed lab.
* **computerDepartmentsMustUseComputerLabs** – CSE/IT/AIDS/CSBS labs only.
* **coreDepartmentsMustUseCoreOrComputerLabs** – Core departments: must use `core` labs unless mapping permits `computer`.

These are on top of the existing room/teacher/student conflicts, capacity checks, and teacher-shift patterns.

## Configuration

The application uses a basic SolverConfig with:
- **OptimizedTimetableConstraintProvider**: Enhanced constraint provider with advanced scheduling logic
- **5-minute time limit**: Stops solving after 5 minutes
- **Early termination**: Stops when a feasible solution is found
- **A105 Pre-allocation**: Optimizes solver performance with pre-allocated lessons

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
- `student_count`: Number of students in the course
- `academic_year`: Academic year (e.g., 1, 2, 3, 4)
- `semester`: Semester number

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
- `output/timetable.json`: JSON format for visualization tools

## Constraint Implementation

This implementation uses OptaPlanner's Constraint Streams API with the OptimizedTimetableConstraintProvider that includes:

### Hard Constraints
- Room conflict: A room can accommodate at most one lesson at the same time
- Teacher conflict: A teacher can teach at most one lesson at the same time
- Student group conflict: A student group can attend at most one lesson at the same time
- Room capacity: A room's capacity should be sufficient for all lessons taught in it
- Lab session in lab room: Lab sessions should be held in lab rooms
- Theory session in theory room: Theory sessions should be held in theory rooms
- A105 room allocation: Special handling for A105 room assignments

### Soft Constraints
- Teacher room stability: A teacher should teach in the same room on the same day
- Theory sessions on different days: Theory sessions of the same course should be spread across different days
- Avoid late classes: Prefer earlier time slots over later ones
- Optimized teacher scheduling: Minimize teacher travel between rooms
- Capacity optimization: Prefer rooms that better match class sizes 