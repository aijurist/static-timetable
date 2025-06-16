#!/usr/bin/env python3
"""
Student Shift Analyzer

Analyzes timetable JSON to determine which time shift each student group
primarily belongs to based on where they have the majority of their lessons.

Time Shifts:
- Morning (8:00-15:00): 8 AM to 3 PM
- Afternoon (15:00-17:00): 3 PM to 5 PM  
- Evening (17:00-19:00): 5 PM to 7 PM
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
        return "Morning"
    elif afternoon_start <= start_time < evening_start:
        return "Afternoon"
    elif evening_start <= start_time < evening_end:
        return "Evening"
    else:
        return "Outside Hours"

def analyze_student_shifts(json_file="output/timetable.json"):
    """Analyze which shift each student group primarily belongs to."""
    
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
    
    # Track lessons per group per shift
    group_lessons_by_shift = defaultdict(lambda: defaultdict(int))
    group_info = {}
    
    # Process each lesson
    for lesson in lessons:
        start_time = parse_time(lesson.get('startTime', ''))
        if start_time is None:
            continue
        
        shift = categorize_time_shift(start_time)
        group_name = lesson.get('group', 'Unknown')
        group_id = lesson.get('groupId', 'Unknown')
        
        # Extract department from group name
        department = group_name.split()[0] if group_name != 'Unknown' else 'Unknown'
        
        # Track lessons per shift for this group
        group_key = f"{group_id}_{group_name}"
        group_lessons_by_shift[group_key][shift] += 1
        
        # Store group info
        if group_key not in group_info:
            group_info[group_key] = {
                'name': group_name,
                'id': group_id,
                'department': department
            }
    
    # Determine primary shift for each group
    group_primary_shifts = {}
    shift_assignments = defaultdict(list)
    
    for group_key, shifts in group_lessons_by_shift.items():
        # Find the shift with the most lessons
        primary_shift = max(shifts.items(), key=lambda x: x[1])[0]
        group_primary_shifts[group_key] = {
            'primary_shift': primary_shift,
            'lesson_counts': dict(shifts),
            'total_lessons': sum(shifts.values())
        }
        
        shift_assignments[primary_shift].append(group_key)
    
    # Calculate statistics
    print("\n" + "="*80)
    print("STUDENT SHIFT ASSIGNMENT ANALYSIS")
    print("="*80)
    
    total_groups = len(group_primary_shifts)
    students_per_group = 60  # Assumption
    total_students = total_groups * students_per_group
    
    print(f"Total Student Groups: {total_groups}")
    print(f"Estimated Total Students: {total_students:,} (assuming ~{students_per_group} students per group)")
    print()
    
    # Print summary by shift
    shift_order = ["Morning", "Afternoon", "Evening", "Outside Hours"]
    
    for shift in shift_order:
        if shift not in shift_assignments:
            continue
            
        groups_in_shift = shift_assignments[shift]
        students_in_shift = len(groups_in_shift) * students_per_group
        percentage = (students_in_shift / total_students * 100) if total_students > 0 else 0
        
        print(f"{shift} Shift ({shift_time_range(shift)}):")
        print(f"  Groups: {len(groups_in_shift)}")
        print(f"  Estimated Students: {students_in_shift:,} ({percentage:.1f}%)")
        
        # Department breakdown
        dept_counts = defaultdict(int)
        total_lessons = 0
        
        for group_key in groups_in_shift:
            dept = group_info[group_key]['department']
            dept_counts[dept] += 1
            total_lessons += group_primary_shifts[group_key]['total_lessons']
        
        print(f"  Total Lessons: {total_lessons}")
        print(f"  Avg lessons per group: {total_lessons/len(groups_in_shift):.1f}")
        
        print("  Departments:")
        for dept, count in sorted(dept_counts.items(), key=lambda x: x[1], reverse=True):
            students = count * students_per_group
            print(f"    {dept}: {count} groups (~{students:,} students)")
        
        print()
    
    # Show groups with mixed schedules
    print("GROUPS WITH MIXED SCHEDULES:")
    print("="*50)
    
    mixed_schedule_count = 0
    for group_key, shifts_data in group_primary_shifts.items():
        lesson_counts = shifts_data['lesson_counts']
        if len(lesson_counts) > 1:  # Group has lessons in multiple shifts
            mixed_schedule_count += 1
            group_name = group_info[group_key]['name']
            primary_shift = shifts_data['primary_shift']
            total_lessons = shifts_data['total_lessons']
            
            print(f"{group_name} (Primary: {primary_shift}, Total: {total_lessons} lessons):")
            for shift, count in sorted(lesson_counts.items()):
                percentage = (count / total_lessons * 100)
                print(f"  {shift}: {count} lessons ({percentage:.1f}%)")
            print()
    
    print(f"Groups with mixed schedules: {mixed_schedule_count} out of {total_groups}")
    
    # Export CSV
    csv_filename = "output/student_shift_assignments.csv"
    os.makedirs("output", exist_ok=True)
    
    with open(csv_filename, 'w', encoding='utf-8') as f:
        f.write("Group_Name,Group_ID,Department,Primary_Shift,Total_Lessons,Morning_Lessons,Afternoon_Lessons,Evening_Lessons,Outside_Hours_Lessons\n")
        
        for group_key in sorted(group_primary_shifts.keys()):
            group_data = group_info[group_key]
            shifts_data = group_primary_shifts[group_key]
            lesson_counts = shifts_data['lesson_counts']
            
            row = [
                f'"{group_data["name"]}"',
                group_data['id'],
                group_data['department'],
                shifts_data['primary_shift'],
                str(shifts_data['total_lessons']),
                str(lesson_counts.get('Morning', 0)),
                str(lesson_counts.get('Afternoon', 0)),
                str(lesson_counts.get('Evening', 0)),
                str(lesson_counts.get('Outside Hours', 0))
            ]
            f.write(",".join(row) + "\n")
    
    print(f"Detailed group assignments exported to: {csv_filename}")
    
    # Summary CSV
    summary_csv = "output/shift_summary.csv"
    with open(summary_csv, 'w', encoding='utf-8') as f:
        f.write("Shift,Time_Range,Groups,Estimated_Students,Percentage,Total_Lessons,Avg_Lessons_Per_Group\n")
        
        for shift in shift_order:
            if shift not in shift_assignments:
                continue
                
            groups_in_shift = shift_assignments[shift]
            students_in_shift = len(groups_in_shift) * students_per_group
            percentage = (students_in_shift / total_students * 100) if total_students > 0 else 0
            total_lessons = sum(group_primary_shifts[gk]['total_lessons'] for gk in groups_in_shift)
            avg_lessons = total_lessons / len(groups_in_shift) if groups_in_shift else 0
            
            f.write(f"{shift},{shift_time_range(shift)},{len(groups_in_shift)},{students_in_shift},{percentage:.1f},{total_lessons},{avg_lessons:.1f}\n")
    
    print(f"Summary exported to: {summary_csv}")
    
    return shift_assignments, group_primary_shifts

def shift_time_range(shift):
    """Get time range string for a shift."""
    ranges = {
        "Morning": "8:00-15:00",
        "Afternoon": "15:00-17:00", 
        "Evening": "17:00-19:00",
        "Outside Hours": "Other times"
    }
    return ranges.get(shift, "Unknown")

if __name__ == "__main__":
    import sys
    
    json_file = "output/timetable.json"
    if len(sys.argv) > 1:
        json_file = sys.argv[1]
    
    analyze_student_shifts(json_file) 