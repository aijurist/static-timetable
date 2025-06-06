import pandas as pd
from datetime import time
from collections import defaultdict
from .data_models import TimeSlot, Room, Teacher, Course, StudentGroup, LectureAssignment
from .config import DAYS, TIME_SLOTS, LAB_BATCH_SIZE, CLASS_STRENGTH

def load_data(teachers_courses_csv, rooms_csv):
    df = pd.read_csv(teachers_courses_csv)
    
    teachers = {}
    for _, row in df.iterrows():
        teacher_id = str(row['teacher_id']) if pd.notna(row['teacher_id']) else "Unknown"
        if teacher_id not in teachers:
            teachers[teacher_id] = Teacher(
                teacher_id,
                str(row['staff_code']) if pd.notna(row['staff_code']) else "",
                str(row['first_name']) if pd.notna(row['first_name']) else "",
                str(row['last_name']) if pd.notna(row['last_name']) else "",
                str(row['teacher_email']) if pd.notna(row['teacher_email']) else ""
            )
    
    courses = {}
    for _, row in df.iterrows():
        course_id = str(row['course_id'])
        if course_id not in courses:
            courses[course_id] = Course(
                course_id,
                str(row['course_code']),
                str(row['course_name']),
                str(row['course_type']) if pd.notna(row['course_type']) else "",
                int(row['lecture_hours']) if pd.notna(row['lecture_hours']) else 0,
                int(row['practical_hours']) if pd.notna(row['practical_hours']) else 0,
                int(row['tutorial_hours']) if pd.notna(row['tutorial_hours']) else 0,
                int(row['credits']) if pd.notna(row['credits']) else 0,
                str(row['course_dept']) if pd.notna(row['course_dept']) else ""
            )
    
    # Create 6 student groups (Sections A-F)
    student_groups = []
    for i in range(6):
        student_groups.append(StudentGroup(i + 1, f"Section {chr(65 + i)}", CLASS_STRENGTH))
    
    rooms_df = pd.read_csv(rooms_csv)
    rooms = []
    for _, row in rooms_df.iterrows():
        rooms.append(Room(
            int(row['id']),
            str(row['room_number']),
            str(row['block']),
            bool(row['is_lab']),
            int(row['room_min_cap']),
            int(row['room_max_cap'])
        ))
    
    return list(teachers.values()), rooms, list(courses.values()), student_groups

def create_time_slots():
    time_slots = []
    id_counter = 0

    for day in range(5):
        for slot in TIME_SLOTS:
            if len(slot) == 2:
                start_str, end_str = slot
                is_break = False
            else:
                start_str, end_str, is_break = slot

            start = time(*map(int, start_str.split(':')))
            end = time(*map(int, end_str.split(':')))
            time_slots.append(TimeSlot(
                id_counter, day, start, end, is_break
            ))
            id_counter += 1

    return time_slots

def create_lecture_assignments(df, teachers, courses, student_groups):
    assignments = []
    assignment_id = 0
    teacher_dict = {str(t.id): t for t in teachers}

    # Create mapping of course to available teachers
    course_teacher_map = defaultdict(list)
    for _, row in df.iterrows():
        course_id = str(row['course_id'])
        teacher_id = str(row['teacher_id']) if pd.notna(row['teacher_id']) else "Unknown"
        if teacher_id not in course_teacher_map[course_id]:
            course_teacher_map[course_id].append(teacher_id)

    for student_group in student_groups:
        for course in courses:
            course_id = str(course.id)
            teacher_list = course_teacher_map.get(course_id, ["Unknown"])
            
            # Assign teachers in round-robin fashion
            teacher_idx = (student_group.id - 1) % len(teacher_list)
            teacher_id = teacher_list[teacher_idx]
            teacher = teacher_dict.get(teacher_id, teacher_dict.get("Unknown"))

            # Lectures
            for _ in range(course.lecture_hours):
                assignments.append(LectureAssignment(
                    assignment_id, course, teacher, student_group, "lecture"
                ))
                assignment_id += 1

            # Tutorials
            for _ in range(course.tutorial_hours):
                assignments.append(LectureAssignment(
                    assignment_id, course, teacher, student_group, "tutorial"
                ))
                assignment_id += 1

            # Labs - create pairs of assignments for each lab session
            lab_sessions_needed = course.practical_hours
            
            for lab_session in range(lab_sessions_needed):
                # Create parent ID to link the two lab parts
                parent_id = f"Lab_{course_id}_{student_group.id}_{lab_session}"
                
                # First part of lab (first 50 minutes)
                assignments.append(LectureAssignment(
                    assignment_id, course, teacher, student_group, "lab", 1, parent_id
                ))
                assignment_id += 1
                
                # Second part of lab (second 50 minutes)
                assignments.append(LectureAssignment(
                    assignment_id, course, teacher, student_group, "lab", 2, parent_id
                ))
                assignment_id += 1

    return assignments