# data_loader.py
import pandas as pd
from datetime import time
from collections import defaultdict
from .data_models import TimeSlot, Room, Teacher, Course, StudentGroup, LectureAssignment
from .config import DAYS, THEORY_TIME_SLOTS, LAB_TIME_SLOTS, LAB_BATCH_SIZE, CLASS_STRENGTH, DEPARTMENT_DATA, DEPARTMENT_BLOCKS

def load_data(teachers_courses_csv, rooms_csv):
    df = pd.read_csv(teachers_courses_csv)
    
    # Filter out semesters 1,2,4,6
    df = df[~df['semester'].isin([1, 2, 4, 6])]
    
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
    
    # Create student groups based on DEPARTMENT_DATA
    student_groups = []
    group_id_counter = 1
    for dept, dept_data in DEPARTMENT_DATA.items():
        for year_str, num_groups in dept_data.items():
            year = int(year_str)
            for i in range(num_groups):
                section_letter = chr(65 + i)
                group_name = f"{dept}-{year}{section_letter}"
                student_groups.append(StudentGroup(
                    group_id_counter, group_name, dept, year, CLASS_STRENGTH
                ))
                group_id_counter += 1

    # print(student_groups)
    
    # Load rooms
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
    course_dict = {c.id: c for c in courses}

    # Group by course_dept and academic_year
    grouped = df.groupby(['course_dept', 'academic_year'])
    
    for (dept, year), group_df in grouped:
        # Find target student groups
        target_groups = [g for g in student_groups if g.department == dept and g.year == year]
        
        if not target_groups:
            continue
            
        for _, row in group_df.iterrows():
            course_id = str(row['course_id'])
            course = course_dict.get(course_id)
            if not course:
                continue
                
            # Get available teachers for this course
            teacher_list = []
            for _, teacher_row in group_df[group_df['course_id'] == int(course_id)].iterrows():
                teacher_id = str(teacher_row['teacher_id'])
                if teacher_id in teacher_dict:
                    teacher_list.append(teacher_id)
            
            if not teacher_list:
                continue
                
            for student_group in target_groups:
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