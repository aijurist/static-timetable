#!/bin/bash

# ==========================================
# ADVANCED TIMETABLE APPLICATION LAUNCHER
# ==========================================
# Enhanced argument parser for department selection
# ==========================================

set -e

# Default values
SOLVER_MINUTES=${SOLVER_MINUTES:-20}
DEPARTMENTS=""
CUSTOM_FILES=()
SHOW_HELP=false

# Function to show usage
show_help() {
    cat << EOF
TIMETABLE SOLVER - Department Selection Tool

USAGE:
    $0 [OPTIONS] [DEPARTMENT_TYPE]

DEPARTMENT TYPES:
    core        Run only core departments (ECE, EEE, MECH, BME, AUTO, AERO, MCT, R&A, BT, CHEM, CIVIL, FT)
    cse         Run only computer science departments (CSE, IT, AIDS, CSBS, CSD, AIML)
    both        Run both core and computer science departments (default)
    custom      Use custom CSV files (specify with -f option)

OPTIONS:
    -m, --minutes MINUTES    Set solver time limit in minutes (default: 20)
    -f, --file FILE         Add custom CSV file (can be used multiple times)
    -h, --help              Show this help message

EXAMPLES:
    $0 core                 # Run only core departments
    $0 cse                  # Run only CSE departments  
    $0 both                 # Run both (default)
    $0 -m 30 core          # Run core departments for 30 minutes
    $0 custom -f data/courses/my_custom.csv
    $0 custom -f file1.csv -f file2.csv

ENVIRONMENT VARIABLES:
    SOLVER_MINUTES          Override default solver time (same as -m option)

EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -m|--minutes)
            if [[ -n $2 && $2 =~ ^[0-9]+$ ]]; then
                SOLVER_MINUTES="$2"
                shift 2
            else
                echo "[ERROR] Invalid minutes value: $2"
                echo "Use --help for usage information"
                exit 1
            fi
            ;;
        -f|--file)
            if [[ -n $2 ]]; then
                CUSTOM_FILES+=("$2")
                shift 2
            else
                echo "[ERROR] File path required after -f/--file"
                exit 1
            fi
            ;;
        core|cse|both|custom)
            if [[ -n $DEPARTMENTS ]]; then
                echo "[ERROR] Department type already specified: $DEPARTMENTS"
                echo "Use --help for usage information"
                exit 1
            fi
            DEPARTMENTS="$1"
            shift
            ;;
        -*)
            echo "[ERROR] Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
        *)
            echo "[ERROR] Unknown argument: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Set default department type if not specified
if [[ -z $DEPARTMENTS ]]; then
    DEPARTMENTS="both"
fi

# Validate custom files if custom department type is selected
if [[ $DEPARTMENTS == "custom" ]]; then
    if [[ ${#CUSTOM_FILES[@]} -eq 0 ]]; then
        echo "[ERROR] Custom department type requires at least one file specified with -f option"
        echo "Use --help for usage information"
        exit 1
    fi
    COURSE_FILES=("${CUSTOM_FILES[@]}")
else
    # Determine course files based on department type
    case $DEPARTMENTS in
        core)
            COURSE_FILES=("data/courses/core_dept_red.csv")
            ;;
        cse)
            COURSE_FILES=("data/courses/cse_dept_red.csv")
            ;;
        both)
            COURSE_FILES=("data/courses/cse_dept_red.csv" "data/courses/core_dept_red.csv")
            ;;
    esac
fi

# Validate that course files exist
for file in "${COURSE_FILES[@]}"; do
    if [[ ! -f "$file" ]]; then
        echo "[ERROR] Course file not found: $file"
        exit 1
    fi
done

# Join course files with comma for Java app
COURSE_FILES_STR=$(IFS=,; echo "${COURSE_FILES[*]}")

echo "=========================================="
echo "OPTIMIZED TIMETABLE SOLVER"
echo "Department type: $DEPARTMENTS"
echo "Course files   : ${COURSE_FILES_STR}"
echo "Solver minutes : $SOLVER_MINUTES"
echo "=========================================="

echo "[INFO] Checking Maven…"
if ! command -v mvn &> /dev/null; then
    echo "[ERROR] Maven is not installed. Please install Maven first."
    exit 1
fi

if [[ ! -d "lib" ]] || [[ -z "$(ls -A lib 2>/dev/null)" ]]; then
    echo "[INFO] Downloading libraries…"
    ./download-libs.sh
fi

echo "[INFO] Building project (may take a moment)…"
mvn -q clean package

echo "[INFO] Starting timetable solver…"
java -Dsolver.minutes="$SOLVER_MINUTES" -jar target/timejava-1.0-SNAPSHOT.jar "$COURSE_FILES_STR" data 