import pandas as pd
from datetime import time
from collections import defaultdict
from .data_models import TimeSlot, Room, Teacher, Course, StudentGroup, LectureAssignment
from .config import DAYS, THEORY_TIME_SLOTS, LAB_TIME_SLOTS, LAB_BATCH_SIZE, CLASS_STRENGTH

def load_data(teachers_courses_csv, rooms_csv):
    df = pd.read_csv(teachers_courses_csv)
    
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
    
    courses = {}
    for _, row in df.iterrows():
        course_id = str(row['course_id'])
        if course_id not in courses:
            courses[course_id] = Course(
                course_id,
                str(row['course_code']),
                str(row['course_name']),
                str(row['course_type']),
                int(row['lecture_hours']),
                int(row['practical_hours']),
                int(row['tutorial_hours']),
                int(row['credits']),
                str(row['course_dept'])
            )
    
    # Create 6 student groups (Sections A-F)
    student_groups = []
    for i in range(6):
        student_groups.append(StudentGroup(i + 1, f"Section {chr(65 + i)}"))
    
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

def create_lecture_assignments(df, teachers, courses, student_groups):
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

    for student_group in student_groups:
        for course in courses:
            course_id = str(course.id)
            teacher_list = course_teacher_map.get(course_id, [])
            
            if not teacher_list:
                continue
                
            # Assign teachers in round-robin fashion
            teacher_idx = (student_group.id - 1) % len(teacher_list)
            teacher_id = teacher_list[teacher_idx]
            teacher = teacher_dict.get(teacher_id)
            
            if not teacher:
                continue

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

            # Labs
            if course.practical_hours > 0:
                if course.practical_hours == 6:
                    # Unsplit for whole class (3 sessions of 2 hours each)
                    for session in range(3):
                        parent_id = f"Lab_{course_id}_{student_group.id}"
                        assignments.append(LectureAssignment(
                            assignment_id, course, teacher, student_group, "lab", None, parent_id
                        ))
                        assignment_id += 1
                else:
                    # Split into batches (each batch has practical_hours/2 sessions)
                    num_sessions = course.practical_hours // 2
                    for batch in range(1, 3):  # Two batches
                        for session in range(num_sessions):
                            parent_id = f"Lab_{course_id}_{student_group.id}_B{batch}"
                            assignments.append(LectureAssignment(
                                assignment_id, course, teacher, student_group, "lab", batch, parent_id
                            ))
                            assignment_id += 1

    return assignments