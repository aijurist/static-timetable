# Makefile for the timetable project

# Directories
SRC_DIR = src/main/java
RESOURCES_DIR = src/main/resources
BUILD_DIR = build
LIB_DIR = lib
CLASSES_DIR = $(BUILD_DIR)/classes

# Java compiler and flags
JAVAC = javac
JAVA = java
JAVAC_FLAGS = -d $(CLASSES_DIR) -cp "$(CLASSPATH)"

# Classpath including all libraries
CLASSPATH = $(CLASSES_DIR):$(LIB_DIR)/*

# Main classes
MAIN_CLASS = org.timetable.TimetableApp
TEST_CLASS = org.timetable.DataLoadTest

# Default target
all: compile

# Create necessary directories
$(CLASSES_DIR):
	mkdir -p $(CLASSES_DIR)

# Compile all Java files
compile: $(CLASSES_DIR)
	@echo "Compiling Java files..."
	@find $(SRC_DIR) -name "*.java" > sources.txt
	@$(JAVAC) $(JAVAC_FLAGS) @sources.txt
	@rm sources.txt
	@echo "Compilation complete."

# Run the main application
run: compile
	@echo "Running the timetable application..."
	@$(JAVA) -cp "$(CLASSPATH)" $(MAIN_CLASS) data/courses/cse_dept_red.csv data

# Run the data load test
test-data: compile
	@echo "Running the data load test..."
	@$(JAVA) -cp "$(CLASSPATH)" $(TEST_CLASS) data/courses/cse_dept_red.csv data

# Clean build artifacts
clean:
	@echo "Cleaning build artifacts..."
	@rm -rf $(BUILD_DIR)
	@echo "Clean complete."

.PHONY: all compile run test-data clean 