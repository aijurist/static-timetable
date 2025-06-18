#!/usr/bin/env python3
"""
Batch Combining Analyzer for Timetable JSON
Analyzes which lab batches are combined in the same room at the same time
"""

import json
import sys
from collections import defaultdict
from typing import Dict, List, Tuple

def load_timetable(file_path: str) -> Dict:
    """Load timetable data from JSON file"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            return json.load(f)
    except FileNotFoundError:
        print(f"Error: File {file_path} not found")
        sys.exit(1)
    except json.JSONDecodeError as e:
        print(f"Error: Invalid JSON in {file_path}: {e}")
        sys.exit(1)

def analyze_batch_combining(timetable_data: Dict) -> None:
    """Analyze batch combining in labs"""
    
    lessons = timetable_data.get('lessons', [])
    
    # Group lessons by course + student group + room + time slot (only labs)
    combined_sessions = defaultdict(list)
    
    for lesson in lessons:
        # Only analyze lab sessions
        if lesson.get('type') != 'lab':
            continue
            
        # Skip if missing required fields
        room = lesson.get('room')
        day = lesson.get('day')
        start_time = lesson.get('startTime')
        end_time = lesson.get('endTime')
        course = lesson.get('course')
        student_group = lesson.get('group')
        lab_batch = lesson.get('batch')
        
        if not all([room, day, start_time, end_time, course, student_group]):
            continue
            
        # Create unique key for same course + group + room + time
        key = (
            course,  # course name
            student_group,  # group name
            room,  # room name
            day,  # day of week
            start_time,  # start time
            end_time  # end time
        )
        
        combined_sessions[key].append({
            'lesson_id': lesson.get('teacherId'),  # use teacherId as identifier
            'lab_batch': lab_batch,
            'teacher': lesson.get('teacher', 'Unknown'),
            'room_capacity': 35  # default capacity, we'll need to get this from room data
        })
    
    # Find sessions with multiple batches
    print("=" * 80)
    print("BATCH COMBINING ANALYSIS - LABS ONLY")
    print("=" * 80)
    
    combined_count = 0
    total_sessions = 0
    large_lab_combined = 0
    large_lab_total = 0
    
    print("\n🎯 SUCCESSFULLY COMBINED BATCHES:")
    print("-" * 50)
    
    for key, batch_list in combined_sessions.items():
        total_sessions += 1
        course, group_name, room_name, day, start_time, end_time = key
        
        # Check if this is a large lab (70+ capacity)
        # Define large labs based on room names from techlongue.csv
        large_lab_names = ['TLGL1', 'TLGL2']
        is_large_lab = any(large_name in room_name for large_name in large_lab_names)
        room_capacity = 70 if is_large_lab else 35
        
        if is_large_lab:
            large_lab_total += 1
        
        # Only show sessions with multiple batches (combined)
        if len(batch_list) > 1:
            combined_count += 1
            if is_large_lab:
                large_lab_combined += 1
            
            batches = [b['lab_batch'] for b in batch_list if b['lab_batch']]
            teachers = list(set(b['teacher'] for b in batch_list))
            
            print(f"\n✅ Course: {course}")
            print(f"   Group: {group_name}")
            print(f"   Room: {room_name} [Capacity: {room_capacity}]")
            print(f"   Time: {day} {start_time}-{end_time}")
            print(f"   Batches: {', '.join(batches) if batches else 'No batch info'}")
            print(f"   Teachers: {', '.join(teachers)}")
            
            if is_large_lab:
                print(f"   🏆 LARGE LAB EFFICIENTLY USED!")
    
    print("\n" + "=" * 80)
    print("SUMMARY STATISTICS")
    print("=" * 80)
    
    print(f"\n📊 Overall Lab Sessions:")
    print(f"   Total unique lab sessions: {total_sessions}")
    print(f"   Sessions with combined batches: {combined_count}")
    if total_sessions > 0:
        print(f"   Combination rate: {(combined_count/total_sessions*100):.1f}%")
    else:
        print("   Combination rate: N/A (no lab sessions found)")
    
    print(f"\n🏗️ Large Lab Utilization (70+ capacity):")
    print(f"   Total large lab sessions: {large_lab_total}")
    print(f"   Large lab sessions with combined batches: {large_lab_combined}")
    if large_lab_total > 0:
        print(f"   Large lab combination rate: {(large_lab_combined/large_lab_total*100):.1f}%")
    else:
        print("   Large lab combination rate: N/A (no large lab sessions)")
    
    # Show problematic cases (large labs with single batches)
    print(f"\n⚠️ POTENTIAL INEFFICIENCIES:")
    print("-" * 50)
    
    single_batch_large_labs = 0
    
    for key, batch_list in combined_sessions.items():
        course, group_name, room_name, day, start_time, end_time = key
        large_lab_names = ['TLGL1', 'TLGL2']
        is_large_lab = any(large_name in room_name for large_name in large_lab_names)
        room_capacity = 70 if is_large_lab else 35
        
        # Show large labs with only single batches
        if is_large_lab and len(batch_list) == 1:
            single_batch_large_labs += 1
            batch_info = batch_list[0]
            
            print(f"\n⚠️  Course: {course}")
            print(f"    Group: {group_name}")
            print(f"    Room: {room_name} [Capacity: {room_capacity}]")
            print(f"    Time: {day} {start_time}-{end_time}")
            print(f"    Batch: {batch_info['lab_batch'] or 'No batch info'}")
            print(f"    Teacher: {batch_info['teacher']}")
            print(f"    🔍 LARGE LAB UNDERUTILIZED (only 1 batch)")
    
    print(f"\n📈 Efficiency Metrics:")
    print(f"   Single-batch large lab sessions: {single_batch_large_labs}")
    print(f"   Combined-batch large lab sessions: {large_lab_combined}")
    
    if large_lab_total > 0:
        efficiency = (large_lab_combined / large_lab_total) * 100
        print(f"   Large lab efficiency: {efficiency:.1f}%")
        
        if efficiency >= 80:
            print("   🎉 EXCELLENT large lab utilization!")
        elif efficiency >= 60:
            print("   👍 GOOD large lab utilization")
        elif efficiency >= 40:
            print("   📊 MODERATE large lab utilization")
        else:
            print("   📉 POOR large lab utilization - needs improvement")

def main():
    """Main function"""
    file_path = "output/timetable.json"
    
    if len(sys.argv) > 1:
        file_path = sys.argv[1]
    
    print(f"Analyzing timetable data from: {file_path}")
    
    try:
        timetable_data = load_timetable(file_path)
        print(f"Successfully loaded timetable data")
        print(f"Keys in timetable data: {list(timetable_data.keys())}")
        
        lessons = timetable_data.get('lessons', [])
        print(f"Total lessons found: {len(lessons)}")
        
        # Count lab sessions
        lab_sessions = [l for l in lessons if l.get('type') == 'lab']
        print(f"Lab sessions found: {len(lab_sessions)}")
        
        analyze_batch_combining(timetable_data)
    except Exception as e:
        print(f"Error occurred: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main() 