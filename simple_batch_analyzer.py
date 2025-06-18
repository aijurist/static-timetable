#!/usr/bin/env python3
"""
Simple Batch Analyzer for Timetable JSON
"""

import json
from collections import defaultdict

def analyze_batches():
    print("Loading timetable data...")
    
    with open('output/timetable.json', 'r') as f:
        data = json.load(f)
    
    lessons = data.get('lessons', [])
    lab_lessons = [l for l in lessons if l.get('type') == 'lab']
    
    print(f"Total lessons: {len(lessons)}")
    print(f"Lab lessons: {len(lab_lessons)}")
    
    # Group by course + group + room + time
    sessions = defaultdict(list)
    
    for lesson in lab_lessons:
        course = lesson.get('course')
        group = lesson.get('group')
        room = lesson.get('room')
        day = lesson.get('day')
        start_time = lesson.get('startTime')
        batch = lesson.get('batch')
        teacher = lesson.get('teacher')
        
        if not all([course, group, room, day, start_time]):
            continue
        
        key = f"{course}|{group}|{room}|{day}|{start_time}"
        sessions[key].append({
            'batch': batch,
            'teacher': teacher,
            'course': course,
            'group': group,
            'room': room,
            'day': day,
            'time': start_time
        })
    
    print(f"\nUnique lab sessions: {len(sessions)}")
    
    # Find combined sessions
    combined = []
    single = []
    large_lab_combined = []
    large_lab_single = []
    
    # Define large labs
    large_labs = ['TLGL1', 'TLGL2']
    
    for key, batch_list in sessions.items():
        room = batch_list[0]['room']
        is_large = any(lab in room for lab in large_labs)
        
        if len(batch_list) > 1:
            combined.append((key, batch_list))
            if is_large:
                large_lab_combined.append((key, batch_list))
        else:
            single.append((key, batch_list))
            if is_large:
                large_lab_single.append((key, batch_list))
    
    print(f"\n=== RESULTS ===")
    print(f"Combined batch sessions: {len(combined)}")
    print(f"Single batch sessions: {len(single)}")
    print(f"Large lab combined sessions: {len(large_lab_combined)}")
    print(f"Large lab single sessions: {len(large_lab_single)}")
    
    # Show combined sessions
    if combined:
        print(f"\n🎯 SUCCESSFULLY COMBINED BATCHES ({len(combined)}):")
        for i, (key, batch_list) in enumerate(combined[:10]):  # Show first 10
            session = batch_list[0]
            batches = [b['batch'] for b in batch_list if b['batch']]
            teachers = [b['teacher'] for b in batch_list]
            is_large = any(lab in session['room'] for lab in large_labs)
            
            print(f"\n{i+1}. Course: {session['course']}")
            print(f"   Group: {session['group']}")
            print(f"   Room: {session['room']} {'[LARGE LAB]' if is_large else '[small lab]'}")
            print(f"   Time: {session['day']} {session['time']}")
            print(f"   Batches: {', '.join(batches)}")
            print(f"   Teachers: {', '.join(teachers)}")
    
    # Show problematic large lab usage
    if large_lab_single:
        print(f"\n⚠️ LARGE LABS WITH SINGLE BATCHES ({len(large_lab_single)}):")
        for i, (key, batch_list) in enumerate(large_lab_single[:10]):  # Show first 10
            session = batch_list[0]
            
            print(f"\n{i+1}. Course: {session['course']}")
            print(f"   Group: {session['group']}")
            print(f"   Room: {session['room']} [LARGE LAB UNDERUTILIZED]")
            print(f"   Time: {session['day']} {session['time']}")
            print(f"   Batch: {session['batch']}")
            print(f"   Teacher: {session['teacher']}")
    
    # Statistics
    total_large_sessions = len(large_lab_combined) + len(large_lab_single)
    if total_large_sessions > 0:
        efficiency = (len(large_lab_combined) / total_large_sessions) * 100
        print(f"\n📊 LARGE LAB EFFICIENCY: {efficiency:.1f}%")
        print(f"   - Combined: {len(large_lab_combined)}")
        print(f"   - Single: {len(large_lab_single)}")
        print(f"   - Total: {total_large_sessions}")
    else:
        print(f"\n📊 NO LARGE LAB SESSIONS FOUND")

if __name__ == "__main__":
    analyze_batches() 