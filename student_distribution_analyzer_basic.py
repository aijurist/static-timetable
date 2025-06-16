#!/usr/bin/env python3
"""
Student Distribution Analyzer (Basic Version)

Analyzes timetable JSON to calculate UNIQUE student distribution across time shifts.
No external dependencies required.
"""

import json
import os
from datetime import datetime, time
from collections import defaultdict

def parse_time(time_str):
    """Parse time string to time object."""
    try:
        return datetime.strptime(time_str, "%H:%M").time()
    except ValueError:
        try:
            return datetime.strptime(time_str, "%H:%S").time()
        except ValueError:
            print(f"Warning: Could not parse time: {time_str}")
            return None

def get_group_size(group_name):
    """Get approximate student count for a group based on naming convention."""
    # Most groups seem to have around 60 students per section
    # This is an approximation - ideally this would come from a groups CSV
    return 60  # Standard section size

def categorize_time_shift(start_time):
    """Categorize a lesson into a time shift."""
    if start_time is None:
        return "Unknown"
    
    # Time shift definitions
    morning_start = time(8, 0)
    afternoon_start = time(15, 0)  # 3 PM
    evening_start = time(17, 0)    # 5 PM
    evening_end = time(19, 0)      # 7 PM
    
    if morning_start <= start_time < afternoon_start:
        return "Morning (8:00-15:00)"
    elif afternoon_start <= start_time < evening_start:
        return "Afternoon (15:00-17:00)"
    elif evening_start <= start_time < evening_end:
        return "Evening (17:00-19:00)"
    else:
        return "Outside Normal Hours"

def analyze_student_distribution(json_file="output/timetable.json", detailed=False, export_csv=False):
    """Main analysis function - counts unique students per shift."""
    
    # Check if file exists
    if not os.path.exists(json_file):
        print(f"Error: Timetable JSON file not found: {json_file}")
        return
    
    print(f"Loading timetable data from: {json_file}")
    
    # Load JSON data
    with open(json_file, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    lessons = data.get('lessons', [])
    groups = {group['id']: group['name'] for group in data.get('groups', [])}
    
    print(f"Loaded {len(lessons)} lessons and {len(groups)} student groups")
    
    # Track unique groups (students) per time shift
    shift_groups = defaultdict(set)  # Set to avoid duplicates
    shift_stats = defaultdict(lambda: {
        'lessons_count': 0,
        'lesson_types': defaultdict(int),
        'departments': defaultdict(set),  # Use set for unique groups per dept
        'details': []
    })
    
    # Process each lesson
    for lesson in lessons:
        start_time = parse_time(lesson.get('startTime', ''))
        if start_time is None:
            continue
        
        shift = categorize_time_shift(start_time)
        lesson_type = lesson.get('type', 'lecture')
        group_name = lesson.get('group', 'Unknown')
        group_id = lesson.get('groupId', 'Unknown')
        
        # Extract department from group name (first part before space)
        department = group_name.split()[0] if group_name != 'Unknown' else 'Unknown'
        
        # Use group_id as unique identifier for students
        unique_group_key = f"{group_id}_{group_name}"
        
        # Add group to shift (set automatically handles duplicates)
        shift_groups[shift].add(unique_group_key)
        
        # Update statistics
        shift_stats[shift]['lessons_count'] += 1
        shift_stats[shift]['lesson_types'][lesson_type] += 1
        shift_stats[shift]['departments'][department].add(unique_group_key)
        
        # Store lesson details if detailed analysis requested
        if detailed:
            shift_stats[shift]['details'].append({
                'course': lesson.get('course', 'Unknown'),
                'group': group_name,
                'group_id': group_id,
                'department': department,
                'type': lesson_type,
                'start_time': lesson.get('startTime'),
                'end_time': lesson.get('endTime'),
                'day': lesson.get('day'),
                'teacher': lesson.get('teacher', 'Unknown'),
                'room': lesson.get('room', 'Unknown'),
                'batch': lesson.get('batch', '')
            })
    
    # Calculate student counts
    print("\n" + "="*80)
    print("STUDENT DISTRIBUTION ANALYSIS SUMMARY")
    print("="*80)
    
    total_unique_groups = len(set().union(*shift_groups.values()))
    total_students = total_unique_groups * get_group_size("standard")
    total_lessons = sum(data['lessons_count'] for data in shift_stats.values())
    
    print(f"Total Unique Student Groups: {total_unique_groups}")
    print(f"Estimated Total Students: {total_students:,} (assuming ~{get_group_size('standard')} students per group)")
    print(f"Total Lessons analyzed: {total_lessons:,}")
    print()
    
    # Print summary by shift
    for shift, unique_groups in shift_groups.items():
        if not unique_groups:
            continue
            
        students_in_shift = len(unique_groups) * get_group_size("standard")
        percentage = (students_in_shift / total_students * 100) if total_students > 0 else 0
        data = shift_stats[shift]
        
        print(f"{shift}:")
        print(f"  Unique Groups: {len(unique_groups)}")
        print(f"  Estimated Students: {students_in_shift:,} ({percentage:.1f}%)")
        print(f"  Total Lessons: {data['lessons_count']:,}")
        if data['lessons_count'] > 0:
            print(f"  Avg lessons per group: {data['lessons_count']/len(unique_groups):.1f}")
        
        # Show lesson type breakdown
        print("  Lesson types:")
        for lesson_type, count in sorted(data['lesson_types'].items()):
            print(f"    {lesson_type.title()}: {count} lessons")
        
        # Show departments with unique group counts
        print("  Departments (unique groups):")
        dept_group_counts = [(dept, len(groups)) for dept, groups in data['departments'].items()]
        sorted_depts = sorted(dept_group_counts, key=lambda x: x[1], reverse=True)
        for dept, group_count in sorted_depts[:5]:
            students = group_count * get_group_size("standard")
            print(f"    {dept}: {group_count} groups (~{students:,} students)")
        
        print()
    
    # Detailed analysis
    if detailed:
        print("\n" + "="*80)
        print("DETAILED ANALYSIS BY SHIFT")
        print("="*80)
        
        for shift, unique_groups in shift_groups.items():
            if not unique_groups:
                continue
                
            students_in_shift = len(unique_groups) * get_group_size("standard")
            data = shift_stats[shift]
            
            print(f"\n{shift.upper()}:")
            print(f"Unique Groups: {len(unique_groups)}")
            print(f"Estimated Students: {students_in_shift:,}")
            print(f"Total Lessons: {data['lessons_count']:,}")
            
            print(f"\nDepartments (unique groups):")
            dept_group_counts = [(dept, len(groups)) for dept, groups in data['departments'].items()]
            sorted_depts = sorted(dept_group_counts, key=lambda x: x[1], reverse=True)
            for dept, group_count in sorted_depts:
                students = group_count * get_group_size("standard")
                print(f"  {dept}: {group_count} groups (~{students:,} students)")
    
    # Export CSV if requested
    if export_csv:
        os.makedirs("output", exist_ok=True)
        
        # Summary CSV
        csv_filename = "output/student_distribution_summary.csv"
        with open(csv_filename, 'w', encoding='utf-8') as f:
            f.write("Time_Shift,Unique_Groups,Estimated_Students,Total_Lessons,Percentage,Lab_Lessons,Lecture_Lessons,Tutorial_Lessons\n")
            
            for shift, unique_groups in shift_groups.items():
                if not unique_groups:
                    continue
                    
                students_in_shift = len(unique_groups) * get_group_size("standard")
                percentage = (students_in_shift / total_students * 100) if total_students > 0 else 0
                data = shift_stats[shift]
                
                lab_count = data['lesson_types'].get('lab', 0)
                lecture_count = data['lesson_types'].get('lecture', 0)
                tutorial_count = data['lesson_types'].get('tutorial', 0)
                
                f.write(f'"{shift}",{len(unique_groups)},{students_in_shift},{data["lessons_count"]},{percentage:.1f},{lab_count},{lecture_count},{tutorial_count}\n')
        
        print(f"Summary CSV exported to: {csv_filename}")
        
        # Department breakdown CSV
        dept_csv_filename = "output/department_distribution_by_shift.csv"
        with open(dept_csv_filename, 'w', encoding='utf-8') as f:
            # Get all shifts and departments
            all_shifts = list(shift_groups.keys())
            all_departments = set()
            for data in shift_stats.values():
                all_departments.update(data['departments'].keys())
            
            # Write header
            header = "Department," + ",".join(f'"{shift}"' for shift in all_shifts) + ",Total\n"
            f.write(header)
            
            # Write department data
            for dept in sorted(all_departments):
                row = [dept]
                total = 0
                for shift in all_shifts:
                    group_count = len(shift_stats[shift]['departments'].get(dept, set()))
                    students = group_count * get_group_size("standard")
                    row.append(str(students))
                    total += students
                row.append(str(total))
                f.write(",".join(row) + "\n")
        
        print(f"Department distribution CSV exported to: {dept_csv_filename}")
    
    # Show detailed group breakdown
    print(f"\nDETAILED GROUP ANALYSIS:")
    print("="*50)
    all_groups_seen = set()
    for shift, groups in shift_groups.items():
        print(f"\n{shift}: {len(groups)} unique groups")
        all_groups_seen.update(groups)
    
    print(f"\nTotal unique groups across all shifts: {len(all_groups_seen)}")
    print(f"Some groups may appear in multiple shifts if they have lessons in different time periods.")
    
    return shift_groups, shift_stats

def main():
    import sys
    
    json_file = "output/timetable.json"
    detailed = False
    export_csv = False
    
    # Simple argument parsing
    if len(sys.argv) > 1:
        for arg in sys.argv[1:]:
            if arg.startswith('--json=') or arg.startswith('-j='):
                json_file = arg.split('=', 1)[1]
            elif arg in ['--detailed', '-d']:
                detailed = True
            elif arg in ['--export', '-e']:
                export_csv = True
            elif arg in ['--help', '-h']:
                print("Usage: python student_distribution_analyzer_basic.py [options]")
                print("Options:")
                print("  --json=FILE, -j=FILE    Path to timetable JSON file")
                print("  --detailed, -d          Show detailed analysis")
                print("  --export, -e            Export results to CSV files")
                print("  --help, -h              Show this help message")
                return
            elif not arg.startswith('-'):
                json_file = arg
    
    analyze_student_distribution(json_file, detailed, export_csv)

if __name__ == "__main__":
    main() 