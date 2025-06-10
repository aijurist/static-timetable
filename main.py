import pandas as pd
from ortools.sat.python import cp_model
from datetime import datetime, time
import os
from collections import defaultdict

# Constants
DAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday']
THEORY_TIME_SLOTS = [
    time(8, 0), time(9, 0), time(10, 0), time(11, 0),
    time(12, 0), time(13, 0), time(14, 0), time(15, 0)
]
LAB_TIME_SLOTS = [
    time(8, 0), time(10, 0), time(12, 0), time(14, 0)
]

# Staff shift timings
STAFF_SHIFTS = {
    'MORNING': (time(8, 0), time(15, 0)),    # 8 AM to 3 PM
    'AFTERNOON': (time(10, 0), time(17, 0)),  # 10 AM to 5 PM
    'EVENING': (time(12, 0), time(19, 0))     # 12 PM to 7 PM
}

class TimeSlot:
    def __init__(self, day, start_time, end_time, is_lab=False):
        self.day = day
        self.start_time = start_time
        self.end_time = end_time
        self.is_lab = is_lab
        self.start_minutes = start_time.hour * 60 + start_time.minute
        self.end_minutes = end_time.hour * 60 + end_time.minute

class Room:
    def __init__(self, id, name, capacity, is_lab=False):
        self.id = id
        self.name = name
        self.capacity = capacity
        self.is_lab = is_lab

class Teacher:
    def __init__(self, id, name, email, max_hours=40):
        self.id = id
        self.name = name
        self.email = email
        self.max_hours = max_hours
        self.teaching_load = 0  # Number of classes they teach
        self.assigned_courses = []  # List of courses they teach

class Course:
    def __init__(self, id, name, dept, lecture_hours=3, tutorial_hours=1, practical_hours=2):
        self.id = id
        self.name = name
        self.dept = dept
        self.lecture_hours = lecture_hours
        self.tutorial_hours = tutorial_hours
        self.practical_hours = practical_hours

class StudentGroup:
    def __init__(self, id, name, size):
        self.id = id
        self.name = name
        self.size = size

class Assignment:
    def __init__(self, id, teacher, course, student_group, session_type, pattern=None, lab_batch=None):
        self.id = id
        self.teacher = teacher
        self.course = course
        self.student_group = student_group
        self.session_type = session_type
        self.pattern = pattern  # A1, A2, A3, TA1, TA2
        self.lab_batch = lab_batch
        self.timeslot = None
        self.room = None

def create_time_slots():
    time_slots = []
    # Theory slots (1 hour each)
    for day in DAYS:
        for start_time in THEORY_TIME_SLOTS:
            end_time = time(start_time.hour + 1, start_time.minute)
            time_slots.append(TimeSlot(day, start_time, end_time))
    
    # Lab slots (2 hours each)
    for day in DAYS:
        for start_time in LAB_TIME_SLOTS:
            end_time = time(start_time.hour + 2, start_time.minute)
            time_slots.append(TimeSlot(day, start_time, end_time, is_lab=True))
    
    return time_slots

def load_data(teachers_courses_csv, rooms_csv):
    # Load teachers and courses
    df = pd.read_csv(teachers_courses_csv)
    
    # Create teachers dictionary
    teachers = {}
    for _, row in df.drop_duplicates(subset=['teacher_id']).iterrows():
        teachers[row['teacher_id']] = Teacher(
            id=row['teacher_id'],
            name=f"{row['first_name']} {row['last_name']}",
            email=row['teacher_email']
        )
    
    # Create courses dictionary
    courses = {}
    for _, row in df.drop_duplicates(subset=['course_id']).iterrows():
        courses[row['course_id']] = Course(
            id=row['course_id'],
            name=row['course_name'],
            dept=row['course_dept']
        )
    
    # Create student groups A through F
    student_groups = {}
    for i in range(6):  # A to F
        group_id = chr(65 + i)  # 65 is ASCII for 'A'
        student_groups[group_id] = StudentGroup(
            id=group_id,
            name=f"Section {group_id}",
            size=70  # Assuming each section has 70 students
        )
    
    # Calculate teaching load for each teacher
    teacher_course_count = df.groupby('teacher_id')['course_id'].nunique()
    for teacher_id, count in teacher_course_count.items():
        teachers[teacher_id].teaching_load = count
    
    # Assign courses to teachers
    for _, row in df.iterrows():
        teacher = teachers[row['teacher_id']]
        course = courses[row['course_id']]
        if course not in teacher.assigned_courses:
            teacher.assigned_courses.append(course)
    
    # Load rooms
    rooms = {}
    df_rooms = pd.read_csv(rooms_csv)
    for _, row in df_rooms.iterrows():
        rooms[row['id']] = Room(
            id=row['id'],
            name=row['room_number'],
            capacity=row['room_max_cap'],
            is_lab=bool(row['is_lab'])
        )
    
    return teachers, courses, student_groups, rooms

def create_assignments(teachers, courses, student_groups, df):
    assignments = []
    assignment_id = 0
    
    # Sort teachers by teaching load (descending)
    sorted_teachers = sorted(teachers.values(), key=lambda t: t.teaching_load, reverse=True)
    
    for teacher in sorted_teachers:
        for course in teacher.assigned_courses:
            # Create assignments for all student groups (A through F)
            for group in student_groups.values():
                # Create theory assignments (A1, A2, A3)
                for i in range(3):
                    assignments.append(Assignment(
                        id=assignment_id,
                        teacher=teacher,
                        course=course,
                        student_group=group,
                        session_type='lecture',
                        pattern=f'A{i+1}'
                    ))
                    assignment_id += 1
                
                # Create lab assignments (TA1, TA2) if course has practical hours
                if course.practical_hours > 0:
                    for i in range(2):
                        assignments.append(Assignment(
                            id=assignment_id,
                            teacher=teacher,
                            course=course,
                            student_group=group,
                            session_type='lab',
                            pattern=f'TA{i+1}',
                            lab_batch=None
                        ))
                    assignment_id += 1
                    
    return assignments

def solve_timetable(teachers_courses_csv, rooms_csv):
    # Create time slots
    time_slots = create_time_slots()
    
    # Load data
    teachers, courses, student_groups, rooms = load_data(teachers_courses_csv, rooms_csv)
    
    # Load course data for assignments
    df = pd.read_csv(teachers_courses_csv)
    
    # Create assignments
    assignments = create_assignments(teachers, courses, student_groups, df)
    
    # Create the model
    model = cp_model.CpModel()
    
    # Create variables
    assignment_vars = {}
    for assignment in assignments:
        for timeslot in time_slots:
            for room in rooms.values():
                if (assignment.session_type == 'lab' and timeslot.is_lab and room.is_lab) or \
                   (assignment.session_type != 'lab' and not timeslot.is_lab and not room.is_lab):
                    var = model.NewBoolVar(f'assignment_{assignment.id}_timeslot_{timeslot.day}_{timeslot.start_time}_room_{room.id}')
                    assignment_vars[(assignment, timeslot, room)] = var
    
        # Each assignment must be scheduled exactly once
    for assignment in assignments:
        model.Add(sum(assignment_vars.get((assignment, t, r), 0) 
                     for t in time_slots 
                     for r in rooms.values()) == 1)
    
    # No teacher conflict
    for t in time_slots:
        for r in rooms.values():
            for teacher in teachers.values():
                teacher_assignments = [a for a in assignments if a.teacher == teacher]
                for i, a1 in enumerate(teacher_assignments):
                    for a2 in teacher_assignments[i+1:]:
                        if (a1, t, r) in assignment_vars and (a2, t, r) in assignment_vars:
                            model.Add(assignment_vars[(a1, t, r)] + assignment_vars[(a2, t, r)] <= 1)
    
    # No room conflict
    for t in time_slots:
        for r in rooms.values():
            for i, a1 in enumerate(assignments):
                for a2 in assignments[i+1:]:
                    if (a1, t, r) in assignment_vars and (a2, t, r) in assignment_vars:
                        model.Add(assignment_vars[(a1, t, r)] + assignment_vars[(a2, t, r)] <= 1)
    
    # No student group conflict
    for t in time_slots:
        for r in rooms.values():
            for group in student_groups.values():
                group_assignments = [a for a in assignments if a.student_group == group]
                for i, a1 in enumerate(group_assignments):
                    for a2 in group_assignments[i+1:]:
                        if (a1, t, r) in assignment_vars and (a2, t, r) in assignment_vars:
                            model.Add(assignment_vars[(a1, t, r)] + assignment_vars[(a2, t, r)] <= 1)
    
    # Lab sessions must be consecutive
            for assignment in assignments:
        if assignment.session_type == 'lab':
            lab_slots = [t for t in time_slots if t.is_lab]
            for i in range(len(lab_slots)-1):
                if lab_slots[i].day == lab_slots[i+1].day:
                    for r in rooms.values():
                        if r.is_lab:
                            if (assignment, lab_slots[i], r) in assignment_vars and \
                               (assignment, lab_slots[i+1], r) in assignment_vars:
                                model.Add(assignment_vars[(assignment, lab_slots[i], r)] == 
                                        assignment_vars[(assignment, lab_slots[i+1], r)])
    
    # Theory sessions (A1, A2, A3) must be on different days
    for teacher in teachers.values():
        for course in teacher.assigned_courses:
            for group in student_groups.values():
                theory_assignments = [a for a in assignments 
                                   if a.teacher == teacher and 
                                   a.course == course and 
                                   a.student_group == group and 
                                   a.session_type == 'lecture']
                if len(theory_assignments) == 3:  # A1, A2, A3
                    for day in DAYS:
                        day_slots = [t for t in time_slots if t.day == day and not t.is_lab]
                        for r in rooms.values():
                            if not r.is_lab:
                                model.Add(sum(assignment_vars.get((a, t, r), 0) 
                                            for a in theory_assignments 
                                            for t in day_slots) <= 1)
    
    # Solve the model
    solver = cp_model.CpSolver()
    solver.parameters.max_time_in_seconds = 300.0  # 5 minutes time limit
    status = solver.Solve(model)
    
    if status == cp_model.OPTIMAL or status == cp_model.FEASIBLE:
        # Process the solution
        solution = []
        for assignment in assignments:
            for timeslot in time_slots:
                for room in rooms.values():
                    if (assignment, timeslot, room) in assignment_vars:
                        if solver.Value(assignment_vars[(assignment, timeslot, room)]) == 1:
                            assignment.timeslot = timeslot
                            assignment.room = room
                            solution.append({
                                'Teacher': assignment.teacher.name,
                                'Course': assignment.course.name,
                                'Group': assignment.student_group.name,
                                'Type': assignment.session_type,
                                'Pattern': assignment.pattern,
                                'Day': timeslot.day,
                                'Start Time': timeslot.start_time.strftime('%H:%M'),
                                'End Time': timeslot.end_time.strftime('%H:%M'),
                                'Room': room.name
                            })
        
        # Save solution to CSV
        df_solution = pd.DataFrame(solution)
        df_solution.to_csv('timetable_result.csv', index=False)
        print(f"Solution found and saved to timetable_result.csv")
        print(f"Number of assignments scheduled: {len(solution)}")
    else:
        print("No solution found.")

def main():
    teachers_courses_csv = 'cse-1.csv'
    rooms_csv = 'techlongue.csv'

    print("Starting timetable optimization...")
    solve_timetable(teachers_courses_csv, rooms_csv)

if __name__ == "__main__":
    main() 