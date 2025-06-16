# Student Distribution Analyzer

This repository contains Python scripts to analyze student distribution across different time shifts from the generated timetable JSON.

## Scripts Available

### 1. `simple_student_analyzer.py` (Recommended)
- **Simple and lightweight** - no external dependencies
- Basic analysis with clear output
- Generates basic CSV export

### 2. `student_distribution_analyzer_basic.py` 
- **Full-featured** - no external dependencies  
- Detailed analysis with comprehensive breakdown
- Multiple CSV exports with department data
- Command-line options for customization

## Student Capacity Assumptions

The scripts use the following capacity assumptions:
- **Lab slots**: 35 students
- **Theory/Lecture slots**: 70 students  
- **Tutorial slots**: 70 students

## Time Shift Definitions

- **Morning (8:00-15:00)**: 8 AM to 3 PM
- **Afternoon (15:00-17:00)**: 3 PM to 5 PM  
- **Evening (17:00-19:00)**: 5 PM to 7 PM

## Usage Examples

### Quick Analysis (Recommended)
```bash
python simple_student_analyzer.py
```

### Detailed Analysis with CSV Export
```bash
python student_distribution_analyzer_basic.py --detailed --export
```

### Using Custom JSON File
```bash
python simple_student_analyzer.py path/to/your/timetable.json
```

### Command Line Options (Basic Analyzer)
```bash
python student_distribution_analyzer_basic.py [options]

Options:
  --json=FILE, -j=FILE    Path to timetable JSON file
  --detailed, -d          Show detailed analysis by department
  --export, -e            Export results to CSV files
  --help, -h              Show help message
```

## Output Files Generated

When using the `--export` option, the following CSV files are created in the `output/` directory:

1. **`student_distribution_summary.csv`**
   - Summary of student counts by time shift
   - Breakdown by lesson type (lab, lecture, tutorial)
   - Percentage distribution

2. **`department_distribution_by_shift.csv`**  
   - Department-wise student distribution across shifts
   - Useful for understanding which departments are active in each shift

3. **`student_distribution_simple.csv`** (from simple analyzer)
   - Basic summary with key metrics

## Sample Output

```
================================================================================
STUDENT DISTRIBUTION ANALYSIS SUMMARY
================================================================================
Total Students across all shifts: 88,235
Total Lessons analyzed: 1,563

Morning (8:00-15:00):
  Students: 70,420 (79.8%)
  Lessons: 1,254
  Avg students per lesson: 56.2

Afternoon (15:00-17:00):
  Students: 11,795 (13.4%)
  Lessons: 203
  Avg students per lesson: 58.1

Evening (17:00-19:00):
  Students: 6,020 (6.8%)
  Lessons: 106
  Avg students per lesson: 56.8
```

## Key Insights from Analysis

Based on the current timetable:

- **79.8%** of students are in **Morning shift** (8 AM - 3 PM)
- **13.4%** of students are in **Afternoon shift** (3 PM - 5 PM)
- **6.8%** of students are in **Evening shift** (5 PM - 7 PM)

The morning shift carries the highest load with over 70,000 students, while evening shift is the lightest with about 6,000 students.

## Requirements

- Python 3.6 or higher
- No external dependencies required
- Works with the JSON output from the timetable application

## Input File Format

The scripts expect a JSON file with the following structure:
```json
{
  "groups": [
    {"id": "1", "name": "GROUP_NAME"}
  ],
  "lessons": [
    {
      "startTime": "HH:MM",
      "endTime": "HH:MM", 
      "type": "lab|lecture|tutorial",
      "group": "GROUP_NAME",
      "course": "COURSE_NAME",
      "day": "DAY_NAME",
      "teacher": "TEACHER_NAME",
      "room": "ROOM_NAME",
      "batch": "BATCH_ID"
    }
  ]
}
``` 