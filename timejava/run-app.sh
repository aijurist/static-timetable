#!/bin/bash

echo "==========================================
OPTIMIZED TIMETABLE APPLICATION
==========================================
Using: OptimizedTimetableConstraintProvider
Features: Enhanced constraints with advanced scheduling
Solver Time: 10 minutes
=========================================="

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "Maven is not installed. Please install Maven first."
    exit 1
fi

# Check if libraries are downloaded
if [ ! -d "lib" ] || [ -z "$(ls -A lib)" ]; then
    echo "Libraries not found. Downloading required libraries..."
    ./download-libs.sh
fi

# Build the project
echo "Building the project..."
mvn clean package

# Run the application with optimized constraints
echo "Running the OPTIMIZED timetable application..."
echo "This will use OptimizedTimetableConstraintProvider with enhanced constraints."
java -jar target/timejava-1.0-SNAPSHOT.jar data/courses/cse_dept_red.csv data 

echo "==========================================
OPTIMIZED TIMETABLE COMPLETED
Output files are in: output/
Check output/timetable_solution_*.csv for results
==========================================" 