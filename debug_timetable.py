#!/usr/bin/env python3

import json

def debug_timetable():
    with open('output/timetable.json', 'r') as f:
        data = json.load(f)
    
    lessons = data.get('lessons', [])
    print(f"Total lessons: {len(lessons)}")
    
    # Check different session types
    types = {}
    for lesson in lessons:
        lesson_type = lesson.get('type', 'Unknown')
        if lesson_type not in types:
            types[lesson_type] = 0
        types[lesson_type] += 1
    
    print(f"\nLesson types distribution:")
    for lesson_type, count in types.items():
        print(f"  {lesson_type}: {count}")
    
    # Find lab sessions
    lab_lessons = [l for l in lessons if l.get('type') == 'lab']
    print(f"\nLab lessons found: {len(lab_lessons)}")
    
    if lab_lessons:
        print(f"\nFirst lab lesson structure:")
        first_lab = lab_lessons[0]
        for key, value in first_lab.items():
            print(f"  {key}: {value}")
        
        # Check for batch information
        print(f"\nBatch information in lab lessons:")
        batch_fields = ['labBatch', 'lab_batch', 'batch', 'batchId', 'batch_id']
        for field in batch_fields:
            if field in first_lab:
                print(f"  Found {field}: {first_lab[field]}")
        
        # Look for batch info across multiple lab lessons
        batch_values = set()
        for lesson in lab_lessons[:20]:  # Check first 20 lab lessons
            for field in batch_fields:
                if field in lesson and lesson[field]:
                    batch_values.add(f"{field}: {lesson[field]}")
        
        print(f"\nBatch values found:")
        for batch_val in batch_values:
            print(f"  {batch_val}")

if __name__ == "__main__":
    debug_timetable() 