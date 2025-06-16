#!/usr/bin/env python3
"""
Simple Student Distribution Analyzer

Analyzes timetable JSON to calculate approximate number of UNIQUE students
in time shifts: 8-3, 3-5, and 5-7.

Student capacity assumptions:
- Lab slots: 35 students
- Theory/Lecture slots: 70 students  
- Tutorial slots: 70 students
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
    # Lab batches (B1, B2, etc.) typically have 30-35 students
    # This is an approximation - ideally this would come from a groups CSV
    return 65  # Standard section size

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

def analyze_student_distribution(json_file="output/timetable.json"):
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
        'group_details': defaultdict(set)  # Track which groups are in each shift
    })
    
    # Process each lesson
    seen_keys = set()
    for lesson in lessons:
        key_dup = (lesson.get('day'), lesson.get('startTime'), lesson.get('groupId', 'Unknown'))
        if key_dup in seen_keys:
            continue  # Skip duplicate lessons (e.g., multiple lab batches at same time)
        seen_keys.add(key_dup)
        
        start_time = parse_time(lesson.get('startTime', ''))
        if start_time is None:
            continue
        
        shift = categorize_time_shift(start_time)
        lesson_type = lesson.get('type', 'lecture')
        group_name = lesson.get('group', 'Unknown')
        
        # Extract department from group name (first part before space)
        department = group_name.split()[0] if group_name != 'Unknown' else 'Unknown'
        
        # Use group_id as unique identifier for students
        unique_group_key = f"{lesson.get('groupId', 'Unknown')}_{group_name}"
        
        # Add group to shift (set automatically handles duplicates)
        shift_groups[shift].add(unique_group_key)
        
        # Update statistics
        shift_stats[shift]['lessons_count'] += 1
        shift_stats[shift]['lesson_types'][lesson_type] += 1
        shift_stats[shift]['departments'][department].add(unique_group_key)
        shift_stats[shift]['group_details'][department].add(group_name)
    
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
    
    # Generate simple CSV output
    csv_filename = "output/student_distribution_simple.csv"
    os.makedirs("output", exist_ok=True)
    
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

if __name__ == "__main__":
    import sys
    
    json_file = "output/timetable.json"
    if len(sys.argv) > 1:
        json_file = sys.argv[1]
    
    analyze_student_distribution(json_file) 