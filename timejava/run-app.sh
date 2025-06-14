#!/bin/bash

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

# Run the application
echo "Running the timetable application..."
java -jar target/timejava-1.0-SNAPSHOT.jar data/courses/cse_dept_red.csv data 