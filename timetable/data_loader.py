import pandas as pd
from datetime import time
from collections import defaultdict
from .data_models import TimeSlot, Room, Teacher, Course, StudentGroup, LectureAssignment
from .config import DAYS, THEORY_TIME_SLOTS, LAB_TIME_SLOTS, LAB_BATCH_SIZE, CLASS_STRENGTH, DEPARTMENT_DATA, DEPARTMENT_BLOCKS, LAB_BLOCKS
import os
import json

# Mapping from full department names to short codes
DEPT_NAME_TO_CODE = {
    "Computer Science & Design": "CSD",
    "Computer Science & Engineering": "CSE",
    "Computer Science & Engineering (Cyber Security)": "CSE-CS",
    "Computer Science & Business Systems": "CSBS",
    "Information Technology": "IT",
    "Artificial Intelligence & Machine Learning": "AIML",
    # "Artificial Intelligence & Data Science": "AIDS",
    # "Electronics & Communication Engineering": "ECE",
    # "Electrical & Electronics Engineering": "EEE",
    # "Aeronautical Engineering": "AERO",
    # "Automobile Engineering": "AUTO",
    # "Mechatronics Engineering": "MCT",
    # "Mechanical Engineering": "MECH",
    # "Biotechnology": "BT",
    # "Biomedical Engineering": "BME",
    # "Robotics & Automation": "R&A",
    # "Food Technology": "FT",
    # "Civil Engineering": "CIVIL",
    # "Chemical Engineering": "CHEM"
}

def load_all_rooms():
    rooms = []
    room_files = [
        "data/classroom/a_block.csv",
        "data/classroom/b_block.csv",
        # "data/classroom/c_block.csv",
        # "data/classroom/d_block.csv",
        "data/labs/j_block.csv",
        "data/labs/k_block.csv",
        "data/labs/techlongue.csv",
        # "data/labs/backup.csv"
    ]
    
    for file in room_files:
        try:
            room_df = pd.read_csv(file)
            for _, row in room_df.iterrows():
                rooms.append(Room(
                    int(row['id']),
                    str(row['room_number']),
                    str(row['block']).upper(),
                    bool(row['is_lab']),
                    int(row['room_min_cap']),
                    int(row['room_max_cap'])
                ))
        except FileNotFoundError:
            print(f"Warning: Room file not found - {file}")
    
    return rooms

def create_student_groups(df):
    student_groups = []
    group_id_counter = 1
    dept_year_groups = defaultdict(list)
    
    # Filter only odd semesters (3,5,7)
    df = df[df['semester'].isin([3,5,7])]
    
    # Map semester to academic year
    semester_to_year = {
        3: 2,  # 3rd sem -> 2nd year
        5: 3,  # 5th sem -> 3rd year
        7: 4   # 7th sem -> 4th year
    }
    
    # Create student groups
    for _, row in df.iterrows():
        dept_full = row['course_dept']
        dept = DEPT_NAME_TO_CODE.get(dept_full, dept_full)
        semester = row['semester']
        year = semester_to_year[semester]
        
        num_groups = DEPARTMENT_DATA.get(dept, {}).get(str(year), 0)
        existing = dept_year_groups.get((dept, year), [])
        
        # Create new groups if needed
        if len(existing) < num_groups:
            for i in range(len(existing), num_groups):
                section = chr(65 + i)  # A, B, C...
                group_name = f"{dept}-{year}{section}"
                group = StudentGroup(group_id_counter, group_name, CLASS_STRENGTH)
                student_groups.append(group)
                dept_year_groups[(dept, year)].append(group)
                group_id_counter += 1
    print(f"Created {len(student_groups)} student groups across {len(dept_year_groups)} department-year combinations")
    # print("Department year:", dept_year_groups)
    # print("Student group:", student_groups)
    return student_groups, dept_year_groups

def load_data(courses_csv):
    df = pd.read_csv(courses_csv)
    
    # Create teachers
    teachers = {}
    for _, row in df.iterrows():
        teacher_id = str(row['teacher_id'])
        if teacher_id not in teachers:
            teachers[teacher_id] = Teacher(
                teacher_id,
                str(row['staff_code']),
                str(row['first_name']),
                str(row['last_name']),
                str(row['teacher_email'])
            )
    
    # Create courses
    courses = {}
    for _, row in df.iterrows():
        course_id = str(row['course_id'])
        if course_id not in courses:
            dept_full = row['course_dept']
            dept = DEPT_NAME_TO_CODE.get(dept_full, dept_full)
            courses[course_id] = Course(
                course_id,
                str(row['course_code']),
                str(row['course_name']),
                str(row['course_type']),
                int(row['lecture_hours']),
                int(row['practical_hours']),
                int(row['tutorial_hours']),
                int(row['credits']),
                dept
            )
    
    # Create student groups
    student_groups, dept_year_groups = create_student_groups(df)
    
    # Load all rooms
    rooms = load_all_rooms()
    
    return list(teachers.values()), rooms, list(courses.values()), student_groups, dept_year_groups

def create_lecture_assignments(df, teachers, courses, student_groups, dept_year_groups):
    assignments = []
    assignment_id = 0
    teacher_dict = {t.id: t for t in teachers}

    # Create mapping of course to available teachers
    course_teacher_map = defaultdict(list)
    for _, row in df.iterrows():
        course_id = str(row['course_id'])
        teacher_id = str(row['teacher_id'])
        if teacher_id not in course_teacher_map[course_id]:
            course_teacher_map[course_id].append(teacher_id)

    # Save course_teacher_map as JSON
    with open('course_teacher_map.json', 'w', encoding='utf-8') as f:
        json.dump(course_teacher_map, f, indent=2)

    # Build course_groups: (dept, year) -> set of course_ids for that year only
    course_groups = defaultdict(set)
    semester_to_year = {3: 2, 5: 3, 7: 4}
    for _, row in df.iterrows():
        dept_full = row['course_dept']
        dept = DEPT_NAME_TO_CODE.get(dept_full, dept_full)
        semester = row['semester']
        if semester not in semester_to_year:
            continue
        year = semester_to_year[semester]
        course_id = str(row['course_id'])
        course_groups[(dept, year)].add(course_id)

    # Save course_groups as JSON
    with open('course_groups.json', 'w', encoding='utf-8') as f:
        json.dump({f"{dept}_{year}": list(ids) for (dept, year), ids in course_groups.items()}, f, indent=2)

    # Assign courses to appropriate student groups (only for correct year)
    assignments_details = []
    for (dept, year), course_id_set in course_groups.items():
        groups = dept_year_groups.get((dept, year), [])
        for group in groups:
            for course_id in course_id_set:
                course = next((c for c in courses if c.id == course_id), None)
                teacher_list = course_teacher_map.get(course_id, [])
                if not teacher_list:
                    continue
                teacher_idx = (group.id - 1) % len(teacher_list)
                teacher_id = teacher_list[teacher_idx]
                teacher = teacher_dict.get(teacher_id)
                if not teacher or not course:
                    continue
                # Record assignment details
                assignments_details.append({
                    'teacher_id': teacher.id,
                    'teacher_name': teacher.full_name,
                    'student_group': group.name,
                    'course_id': course.id,
                    'course_code': course.code,
                    'course_name': course.name
                })
                # Lectures
                for _ in range(course.lecture_hours):
                    assignments.append(LectureAssignment(
                        assignment_id, course, teacher, group, "lecture"
                    ))
                    assignment_id += 1

                # Tutorials
                for _ in range(course.tutorial_hours):
                    assignments.append(LectureAssignment(
                        assignment_id, course, teacher, group, "tutorial"
                    ))
                    assignment_id += 1

                # Labs
                if course.practical_hours > 0:
                    if course.practical_hours == 6:
                        # Unsplit for whole class (3 sessions of 2 hours each)
                        for session in range(3):
                            parent_id = f"Lab_{course.id}_{group.id}"
                            assignments.append(LectureAssignment(
                                assignment_id, course, teacher, group, "lab", None, parent_id
                            ))
                            assignment_id += 1
                    else:
                        # Split into batches (each batch has practical_hours/2 sessions)
                        num_sessions = course.practical_hours // 2
                        for batch in range(1, 3):  # Two batches
                            for session in range(num_sessions):
                                parent_id = f"Lab_{course.id}_{group.id}_B{batch}"
                                assignments.append(LectureAssignment(
                                    assignment_id, course, teacher, group, "lab", batch, parent_id
                                ))
                                assignment_id += 1
    # Save assignments_details as JSON
    with open('assignments_details.json', 'w', encoding='utf-8') as f:
        json.dump(assignments_details, f, indent=2)
    print(f"Created {len(assignments)} lecture assignments")
    print(f"Total unique assignments: {len(set(a.id for a in assignments))}")
    return assignments