import pandas as pd
from datetime import time, datetime, timedelta
from optapy import (
    planning_entity, planning_variable, planning_solution, problem_fact,
    constraint_provider, solver_manager_create, planning_entity_collection_property, value_range_provider, problem_fact_collection_property, planning_score
)
from optapy.types import SolverConfig
from optapy.constraint import ConstraintFactory
from optapy.types import Joiners, HardSoftScore
from collections import defaultdict

# ====================== Problem Facts ======================

@problem_fact
class TimeSlot:
    def __init__(self, id, day, start_time, end_time, is_break=False):
        self.id = id
        self.day = day  # 0=Monday, 1=Tuesday, etc.
        self.start_time = start_time
        self.end_time = end_time
        self.is_break = is_break

    @property
    def start_minutes(self):
        return self.start_time.hour * 60 + self.start_time.minute

    @property
    def end_minutes(self):
        return self.end_time.hour * 60 + self.end_time.minute

    def __str__(self):
        return f"{['Mon', 'Tue', 'Wed', 'Thu', 'Fri'][self.day]} {self.start_time.strftime('%H:%M')}-{self.end_time.strftime('%H:%M')}"

    def __eq__(self, other):
        return (self.id == other.id if other else False)

    def __hash__(self):
        return hash(self.id)

@problem_fact
class Room:
    def __init__(self, id, room_number, block, is_lab, min_cap, max_cap):
        self.id = id
        self.room_number = room_number
        self.block = block
        self.is_lab = is_lab
        self.min_cap = min_cap
        self.max_cap = max_cap

    def __str__(self):
        return f"{self.block}-{self.room_number} ({'Lab' if self.is_lab else 'Classroom'})"

    def __eq__(self, other):
        return (self.id == other.id if other else False)

    def __hash__(self):
        return hash(self.id)

@problem_fact
class Teacher:
    def __init__(self, id, staff_code, first_name, last_name, email, max_hours=21):
        self.id = id if id != "Unknown" else "Unknown"
        self.staff_code = staff_code
        self.full_name = f"{first_name} {last_name}".strip()
        self.email = email
        self.max_hours = max_hours

    def __str__(self):
        return self.full_name

    def __eq__(self, other):
        return (self.id == other.id if other else False)

    def __hash__(self):
        return hash(self.id)

@problem_fact
class Course:
    def __init__(self, id, code, name, course_type, lecture_hours, practical_hours, tutorial_hours, credits, dept):
        self.id = id
        self.code = code
        self.name = name
        self.type = course_type
        self.lecture_hours = lecture_hours
        self.practical_hours = practical_hours
        self.tutorial_hours = tutorial_hours
        self.credits = credits
        self.dept = dept

    def __str__(self):
        return f"{self.code} - {self.name}"

    def __eq__(self, other):
        return (self.id == other.id if other else False)

    def __hash__(self):
        return hash(self.id)

@problem_fact
class StudentGroup:
    def __init__(self, id, name, strength=70):
        self.id = id
        self.name = name
        self.strength = strength

    def __str__(self):
        return self.name

    def __eq__(self, other):
        return (self.id == other.id if other else False)

    def __hash__(self):
        return hash(self.id)

# ====================== Planning Entity ======================

@planning_entity
class LectureAssignment:
    def __init__(self, id, course, teacher, student_group, session_type="lecture", lab_batch=None, parent_lab_id=None):
        self.id = id
        self.course = course
        self.teacher = teacher
        self.student_group = student_group
        self.session_type = session_type  # "lecture", "tutorial", or "lab"
        self.lab_batch = lab_batch  # 1 or 2 for lab batches, None for non-lab
        self.parent_lab_id = parent_lab_id # To link 2-hour lab sessions
        self.timeslot = None
        self.room = None

    @planning_variable(TimeSlot, ["timeslots"])
    def get_timeslot(self):
        return self.timeslot

    def set_timeslot(self, new_timeslot):
        self.timeslot = new_timeslot

    @planning_variable(Room, ["rooms"])
    def get_room(self):
        return self.room

    def set_room(self, new_room):
        self.room = new_room

    def duration_hours(self):
        return 1  # All individual sessions are 1 hour (50 mins)

    def required_capacity(self):
        if self.session_type == "lab":
            return 35  # Lab batch size
        return self.student_group.strength  # Full class for lectures/tutorials

    def is_lab(self):
        return self.session_type == "lab"

    def __str__(self):
        batch_info = f" (Batch {self.lab_batch})" if self.lab_batch else ""
        return f"{self.course.code} - {self.session_type.title()}{batch_info} - {self.student_group}"

# ====================== Planning Solution ======================

@planning_solution
class TimeTable:
    def __init__(self, lecture_assignments, timeslots, rooms, teachers, courses, student_groups, score=None):
        self.lecture_assignments = lecture_assignments
        self.timeslots = timeslots
        self.rooms = rooms
        self.teachers = teachers
        self.courses = courses
        self.student_groups = student_groups
        self.score = score

    @planning_score(HardSoftScore)
    def get_score(self):
        return self.score

    def set_score(self, score):
        self.score = score
    
    @planning_entity_collection_property(LectureAssignment)
    def get_lecture_assignments(self):
        return self.lecture_assignments

    @value_range_provider(range_id="timeslots")
    @problem_fact_collection_property(TimeSlot)
    def get_timeslot_list(self):
        return self.timeslots

    @value_range_provider(range_id="rooms")
    @problem_fact_collection_property(Room)
    def get_room_list(self):
        return self.rooms


# ====================== Constraints ======================

@constraint_provider
def timetable_constraints(constraint_factory: ConstraintFactory):
    return [
        # Hard constraints
        teacher_conflict(constraint_factory),
        room_conflict(constraint_factory),
        student_group_conflict(constraint_factory),
        teacher_max_hours(constraint_factory),
        lab_in_lab_room(constraint_factory),
        lecture_in_classroom(constraint_factory),
        room_capacity(constraint_factory),
        no_classes_during_breaks(constraint_factory),
        consecutive_lab_slots(constraint_factory), # New hard constraint for consecutive labs

        # Soft constraints
        minimize_teacher_gaps(constraint_factory),
        prefer_consecutive_classes(constraint_factory),
        prefer_same_room_for_course(constraint_factory),
        # distribute_lab_batches(constraint_factory), # Removed, handled by consecutive_lab_slots and prefer_different_lab_batches
        prefer_different_lab_batches(constraint_factory)
    ]

def teacher_conflict(constraint_factory):
    """Teacher cannot teach two classes at the same time"""
    return constraint_factory.for_each(LectureAssignment) \
        .join(
            LectureAssignment,
            Joiners.equal(lambda a: a.teacher),
            Joiners.equal(lambda a: a.timeslot),
            Joiners.less_than(lambda a: a.id)
        ) \
        .filter(lambda a1, a2:
                a1.timeslot is not None and a2.timeslot is not None) \
        .penalize("Teacher conflict", HardSoftScore.ONE_HARD)

def room_conflict(constraint_factory):
    """Room cannot host two classes at the same time"""
    return constraint_factory.for_each(LectureAssignment) \
        .join(
            LectureAssignment,
            Joiners.equal(lambda a: a.room),
            Joiners.equal(lambda a: a.timeslot),
            Joiners.less_than(lambda a: a.id)
        ) \
        .filter(lambda a1, a2:
                a1.room is not None and a2.room is not None and
                a1.timeslot is not None and a2.timeslot is not None) \
        .penalize("Room conflict", HardSoftScore.ONE_HARD)

def student_group_conflict(constraint_factory):
    """Student group cannot attend two classes at the same time"""
    return constraint_factory.for_each(LectureAssignment) \
        .join(
            LectureAssignment,
            Joiners.equal(lambda a: a.student_group),
            Joiners.equal(lambda a: a.timeslot),
            Joiners.less_than(lambda a: a.id)
        ) \
        .filter(lambda a1, a2:
                a1.timeslot is not None and a2.timeslot is not None) \
        .penalize("Student group conflict", HardSoftScore.ONE_HARD)

def teacher_max_hours(constraint_factory):
    """Teacher cannot exceed 21 hours per week"""
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda a: a.timeslot is not None) \
        .group_by(
            lambda a: a.teacher,
            lambda a: a.duration_hours()
        ) \
        .filter(lambda teacher, total_hours: total_hours > teacher.max_hours) \
        .penalize(
            "Teacher max hours exceeded",
            HardSoftScore.ONE_HARD,
            lambda teacher, total_hours: total_hours - teacher.max_hours
        )

def lab_in_lab_room(constraint_factory):
    """Labs must be conducted in lab rooms only"""
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda a:
                a.session_type == "lab" and 
                a.room is not None and 
                not a.room.is_lab) \
        .penalize("Lab not in lab room", HardSoftScore.ONE_HARD)

def lecture_in_classroom(constraint_factory):
    """Lectures and tutorials should be in classrooms (not labs)"""
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda a:
                a.session_type != "lab" and 
                a.room is not None and 
                a.room.is_lab) \
        .penalize("Lecture in lab room", HardSoftScore.ONE_HARD)

def room_capacity(constraint_factory):
    """Room capacity must not be exceeded"""
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda a:
                a.room is not None and 
                a.required_capacity() > a.room.max_cap) \
        .penalize("Room capacity exceeded", HardSoftScore.ONE_HARD)

def no_classes_during_breaks(constraint_factory):
    """No classes should be scheduled during break times"""
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda a:
                a.timeslot is not None and 
                a.timeslot.is_break) \
        .penalize("Class during break", HardSoftScore.ONE_HARD)

def consecutive_lab_slots(constraint_factory):
    """Labs must be scheduled in consecutive 2-hour slots for the same parent_lab_id and student group,
       with each lab batch filling one of the two slots."""
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda assignment: assignment.session_type == "lab" and assignment.parent_lab_id is not None) \
        .join(
            LectureAssignment,
            Joiners.equal(lambda a: a.parent_lab_id),
            Joiners.equal(lambda a: a.student_group),
            Joiners.equal(lambda a: a.teacher),
            Joiners.equal(lambda a: a.room), # Labs should ideally be in the same room
            Joiners.less_than(lambda a: a.id) # To avoid self-joining and duplicate pairs
        ) \
        .filter(lambda a1, a2:
                a1.timeslot is not None and a2.timeslot is not None and
                a1.timeslot.day == a2.timeslot.day and # Must be on the same day
                abs(a1.timeslot.start_minutes - a2.timeslot.start_minutes) != 50 # Not exactly one 50-min period apart
        ) \
        .penalize("Non-consecutive lab slots", HardSoftScore.ONE_HARD)

def prefer_different_lab_batches(constraint_factory):
    """Prefer different lab batches of the SAME COURSE for the SAME STUDENT GROUP to be scheduled in consecutive slots, not overlapping."""
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda a: a.session_type == "lab" and a.lab_batch is not None) \
        .join(
            LectureAssignment,
            Joiners.equal(lambda a: a.course),
            Joiners.equal(lambda a: a.student_group),
            Joiners.equal(lambda a: a.timeslot), # Check for overlap in timeslot
            Joiners.less_than(lambda a: a.id)
        ) \
        .filter(lambda a1, a2:
                a1.timeslot is not None and a2.timeslot is not None and
                a1.lab_batch != a2.lab_batch # Ensure they are different batches
        ) \
        .penalize("Overlapping different lab batches of same course", HardSoftScore.ONE_SOFT)

def minimize_teacher_gaps(constraint_factory):
    """Minimize gaps between teacher's classes on the same day"""
    return constraint_factory.for_each(LectureAssignment) \
        .join(
            LectureAssignment,
            Joiners.equal(lambda a1: a1.teacher, lambda a2: a2.teacher),
            Joiners.equal(
                lambda a1: a1.timeslot.day if a1.timeslot is not None else -1,
                lambda a2: a2.timeslot.day if a2.timeslot is not None else -1
            ),
            Joiners.less_than(
                lambda a1: a1.timeslot.start_minutes if a1.timeslot is not None else -1,
                lambda a2: a2.timeslot.start_minutes if a2.timeslot is not None else -1
            )
        ) \
        .filter(lambda a1, a2:
                a1.timeslot is not None and 
                a2.timeslot is not None and
                a1.timeslot.day == a2.timeslot.day and
                get_gap_between_slots(a1.timeslot, a2.timeslot) > 0
        ) \
        .penalize(
            "Teacher gap between classes",
            HardSoftScore.ONE_SOFT,
            lambda a1, a2: get_gap_between_slots(a1.timeslot, a2.timeslot)
        )

def prefer_consecutive_classes(constraint_factory):
    """Students prefer consecutive classes with minimal gaps"""
    return constraint_factory.for_each(LectureAssignment) \
        .join(
            LectureAssignment,
            Joiners.equal(lambda a1: a1.student_group, lambda a2: a2.student_group),
            Joiners.equal(
                lambda a1: a1.timeslot.day if a1.timeslot is not None else -1,
                lambda a2: a2.timeslot.day if a2.timeslot is not None else -1
            ),
            Joiners.less_than(
                lambda a1: a1.timeslot.start_minutes if a1.timeslot is not None else -1,
                lambda a2: a2.timeslot.start_minutes if a2.timeslot is not None else -1
            )
        ) \
        .filter(lambda a1, a2:
                a1.timeslot is not None and 
                a2.timeslot is not None and
                a1.timeslot.day == a2.timeslot.day and
                get_gap_between_slots(a1.timeslot, a2.timeslot) > 0
        ) \
        .penalize(
            "Student group gap between classes",
            HardSoftScore.ONE_SOFT,
            lambda a1, a2: get_gap_between_slots(a1.timeslot, a2.timeslot)
        )

def prefer_same_room_for_course(constraint_factory):
    """Prefer same room for different sessions of the same course"""
    return constraint_factory.for_each(LectureAssignment) \
        .join(
            LectureAssignment,
            Joiners.equal(lambda a1: a1.course),
            Joiners.equal(lambda a1: a1.student_group),
            Joiners.less_than(lambda a: a.id)
        ) \
        .filter(lambda a1, a2:
                a1.room is not None and 
                a2.room is not None and 
                a1.room != a2.room and 
                a1.session_type != "lab" and 
                a2.session_type != "lab"
        ) \
        .penalize("Course in different rooms", HardSoftScore.ONE_SOFT)

def get_gap_between_slots(slot1, slot2):
    """Calculate gap in periods between two time slots"""
    if slot1 is None or slot2 is None or slot1.day != slot2.day:
        return 0

    slot1_end = slot1.end_time.hour * 60 + slot1.end_time.minute
    slot2_start = slot2.start_time.hour * 60 + slot2.start_time.minute
    gap_minutes = slot2_start - slot1_end
    return max(0, gap_minutes // 50)  # 50-minute periods

# ====================== Data Loading ======================

def load_data(teachers_courses_csv, rooms_csv):
    """Load teachers, courses, and rooms data from CSV files"""
    # Load teachers and courses data
    df = pd.read_csv(teachers_courses_csv)
    
    # Create teachers dictionary
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
    
    # Create courses dictionary
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
    
    # Create 6 student groups (sections A-F)
    student_groups = []
    for i in range(6):
        student_groups.append(
            StudentGroup(
                i + 1,
                f"Section {chr(65 + i)}",  # Section A, B, C, D, E, F
                70
            )
        )
    
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

def create_time_slots():
    """Create time slots for Monday-Friday, 8:10 AM - 3:00 PM"""
    time_slots = []
    id_counter = 0
    
    for day in range(5):  # Monday to Friday
        # Period 1: 8:10-9:00
        time_slots.append(TimeSlot(
            id_counter, day, 
            time(8, 10), 
            time(9, 0), 
            False
        ))
        id_counter += 1
        
        # Period 2: 9:00-9:50
        time_slots.append(TimeSlot(
            id_counter, day,
            time(9, 0),
            time(9, 50),
            False
        ))
        id_counter += 1
        
        # Morning break: 9:50-10:10 (20 mins)
        time_slots.append(TimeSlot(
            id_counter, day,
            time(9, 50),
            time(10, 10),
            True  # Break time
        ))
        id_counter += 1
        
        # Period 3: 10:10-11:00
        time_slots.append(TimeSlot(
            id_counter, day,
            time(10, 10),
            time(11, 0),
            False
        ))
        id_counter += 1
        
        # Period 4: 11:00-11:50
        time_slots.append(TimeSlot(
            id_counter, day,
            time(11, 0),
            time(11, 50),
            False
        ))
        id_counter += 1
        
        # Lunch break: 11:50-12:40 (50 mins)
        time_slots.append(TimeSlot(
            id_counter, day,
            time(11, 50),
            time(12, 40),
            True  # Lunch break
        ))
        id_counter += 1
        
        # Period 5: 12:40-1:30
        time_slots.append(TimeSlot(
            id_counter, day,
            time(12, 40),
            time(13, 30),
            False
        ))
        id_counter += 1
        
        # Period 6: 1:30-2:20
        time_slots.append(TimeSlot(
            id_counter, day,
            time(13, 30),
            time(14, 20),
            False
        ))
        id_counter += 1
        
        # Period 7: 2:20-3:10 (extended day for labs if needed)
        time_slots.append(TimeSlot(
            id_counter, day,
            time(14, 20),
            time(15, 10),
            False
        ))
        id_counter += 1
    
    return time_slots

def create_lecture_assignments(df, teachers, courses, student_groups):
    """Assign all unique courses to every student group, with correct lecture, tutorial, and lab hours (labs split into 2 batches)."""
    assignments = []
    assignment_id = 0

    # Create a teacher lookup dictionary
    teacher_dict = {str(t.id): t for t in teachers}

    # For each course, find all teachers who can teach it (in order of appearance)
    course_teacher_map = {}
    for _, row in df.iterrows():
        course_id = str(row['course_id'])
        teacher_id = str(row['teacher_id']) if pd.notna(row['teacher_id']) else "Unknown"
        if course_id not in course_teacher_map:
            course_teacher_map[course_id] = []
        if teacher_id not in course_teacher_map[course_id]:
            course_teacher_map[course_id].append(teacher_id)

    # For each student group, assign all unique courses
    for student_group in student_groups:
        for course in courses:
            course_id = str(course.id)
            teacher_list = course_teacher_map.get(course_id, ["Unknown"])
            # Cycle through teachers for each group
            teacher_id = teacher_list[student_group.id % len(teacher_list)]
            teacher = teacher_dict.get(teacher_id, teacher_dict.get("Unknown"))

            # Create lecture assignments
            for _ in range(course.lecture_hours):
                assignments.append(
                    LectureAssignment(assignment_id, course, teacher, student_group, "lecture")
                )
                assignment_id += 1

            # Create tutorial assignments
            for _ in range(course.tutorial_hours):
                assignments.append(
                    LectureAssignment(assignment_id, course, teacher, student_group, "tutorial")
                )
                assignment_id += 1

            # Create lab assignments (each practical hour is 1-hour session, grouped into 2-hour blocks by constraint)
            # Each practical hour counts as one hour of lab content. If a lab is 4 practical hours,
            # it means 4 individual 1-hour sessions, but the constraint will ensure they are in 2-hour blocks.
            # We need to create parent_lab_id for linking 2-hour blocks.
            num_lab_sessions_per_batch = course.practical_hours
            if num_lab_sessions_per_batch > 0:
                # Generate a unique parent_lab_id for each 2-hour lab block
                # Since each practical_hour is 1 unit, for 'P' practical hours, we need P/2 2-hour blocks.
                # If P is odd, handle the last one as a 1-hour session or adjust the logic.
                # For simplicity here, we assume P is even for 2-hour blocks or handle leftovers.
                # For the given data: 4 practical hours -> 2 x 2-hour blocks. 2 practical hours -> 1 x 2-hour block.
                # We need to create 'P' 1-hour assignments for each batch.
                
                # Each 'practical_hour' from the CSV represents 1 hour of "content".
                # If we want 2-hour lab sessions, then for 'P' practical hours, we need P/2 blocks of 2 hours.
                # Each 2-hour block will consist of 2 consecutive 1-hour assignments.
                # So we need to create 'P' assignments for Batch 1 and 'P' assignments for Batch 2.
                # The 'parent_lab_id' will link the two 1-hour assignments that form a 2-hour block.

                lab_block_counter = 0
                for _ in range(course.practical_hours // 2): # Number of 2-hour blocks
                    # Assign an ID for this 2-hour lab block
                    current_parent_lab_id = f"LabBlock_{course.id}_{student_group.id}_{lab_block_counter}"
                    
                    # For Batch 1, create two 1-hour assignments for this block
                    assignments.append(
                        LectureAssignment(assignment_id, course, teacher, student_group, "lab", 1, current_parent_lab_id)
                    )
                    assignment_id += 1
                    assignments.append(
                        LectureAssignment(assignment_id, course, teacher, student_group, "lab", 1, current_parent_lab_id)
                    )
                    assignment_id += 1

                    # For Batch 2, create two 1-hour assignments for this block
                    assignments.append(
                        LectureAssignment(assignment_id, course, teacher, student_group, "lab", 2, current_parent_lab_id)
                    )
                    assignment_id += 1
                    assignments.append(
                        LectureAssignment(assignment_id, course, teacher, student_group, "lab", 2, current_parent_lab_id)
                    )
                    assignment_id += 1
                    lab_block_counter += 1
                
                # If there's an odd practical hour (e.g., 3 hours for a lab), handle the leftover.
                # This scenario is less common for fixed 2-hour labs, but good to consider.
                # For now, we'll assume practical_hours will always be even to form 2-hour blocks.
                # If practical_hours is 1, it means half a 2-hour block, which is problematic for current constraint.
                # For the given JNTUH R16 (4 or 2 practical hours), this will always yield 2-hour blocks.

    return assignments

# ====================== Solver Configuration ======================
def solve_timetable(teachers_courses_csv, rooms_csv):
    try:
        import jpype
        # Load data
        teachers, rooms, courses, student_groups = load_data(teachers_courses_csv, rooms_csv)
        df = pd.read_csv(teachers_courses_csv)
        
        print(f"Loaded: {len(teachers)} teachers, {len(rooms)} rooms, {len(courses)} courses, {len(student_groups)} student groups")
        
        # Create time slots
        time_slots = create_time_slots()
        print(f"Created {len(time_slots)} time slots")
        
        # Create lecture assignments
        lecture_assignments = create_lecture_assignments(df, teachers, courses, student_groups)
        print(f"Created {len(lecture_assignments)} lecture assignments")
        
        if not lecture_assignments:
            print("No lecture assignments created. Check your data.")
            return None
        
        # Create problem
        problem = TimeTable(lecture_assignments, time_slots, rooms, teachers, courses, student_groups)
        
        # Configure solver
        Duration = jpype.JClass("java.time.Duration")
        config = SolverConfig() \
            .withSolutionClass(TimeTable) \
            .withEntityClasses([LectureAssignment]) \
            .withConstraintProviderClass(timetable_constraints) \
            .withTerminationSpentLimit(Duration.parse("PT5M"))
            
        print("Starting optimization...")
        # Solve
        solver_manager = solver_manager_create(config)
        solver_job = solver_manager.solve("timetable", problem)
        solution = solver_job.getFinalBestSolution()
        
        print(f"Optimization completed with score: {solution.get_score()}")
        return solution
        
    except Exception as e:
        print(f"Error during optimization: {e}")
        import traceback
        traceback.print_exc()
        return None

# ====================== Result Visualization ======================

def print_timetable(solution):
    """Print the generated timetable"""
    if not solution:
        print("No solution to display")
        return
        
    print(f"\n=== FINAL TIMETABLE (Score: {solution.get_score()}) ===")
    
    # Group assignments by student group
    by_group = defaultdict(list)
    for assignment in solution.lecture_assignments:
        if assignment.timeslot is not None and assignment.room is not None and not assignment.timeslot.is_break:
            by_group[assignment.student_group].append(assignment)
    
    # Print timetable for each group
    for group, assignments in by_group.items():
        print(f"\n=== TIMETABLE FOR {group} ===")
        
        # Sort by day and time
        assignments.sort(key=lambda x: (x.timeslot.day, x.timeslot.start_time))
        
        current_day = None
        for assignment in assignments:
            if assignment.timeslot.day != current_day:
                current_day = assignment.timeslot.day
                print(f"\n{['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday'][current_day]}")
                print("-" * 60)
            
            batch_info = f" (Batch {assignment.lab_batch})" if assignment.lab_batch else ""
            duration_info = f" ({assignment.duration_hours()}h)" if assignment.duration_hours() > 1 else ""
            
            print(f"{assignment.timeslot.start_time.strftime('%H:%M')}-{assignment.timeslot.end_time.strftime('%H:%M')}: "
                  f"{assignment.course.code} - {assignment.session_type.title()}{batch_info}{duration_info}")
            print(f"  Teacher: {assignment.teacher}")
            print(f"  Room: {assignment.room}")
            print()

def export_to_csv(solution, filename="timetable_result.csv"):
    """Export the timetable to CSV"""
    if not solution:
        print("No solution to export")
        return
        
    data = []
    for assignment in solution.lecture_assignments:
        if assignment.timeslot is not None and assignment.room is not None and not assignment.timeslot.is_break:
            data.append({
                "Day": ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday'][assignment.timeslot.day],
                "Time": f"{assignment.timeslot.start_time.strftime('%H:%M')}-{assignment.timeslot.end_time.strftime('%H:%M')}",
                "Student Group": str(assignment.student_group),
                "Course Code": assignment.course.code,
                "Course Name": assignment.course.name,
                "Session Type": assignment.session_type.title(),
                "Lab Batch": assignment.lab_batch if assignment.lab_batch else "",
                "Teacher": str(assignment.teacher),
                "Room": str(assignment.room),
                "Duration (hours)": assignment.duration_hours()
            })
    
    if data:
        df_result = pd.DataFrame(data)
        df_result.to_csv(filename, index=False)
        print(f"Timetable exported to {filename}")
        
        # Print summary statistics
        print(f"\nSUMMARY:")
        print(f"Total scheduled sessions: {len(data)}")
        print(f"Sessions by type:")
        for session_type in df_result['Session Type'].unique():
            count = len(df_result[df_result['Session Type'] == session_type])
            print(f"  {session_type}: {count}")
    else:
        print("No scheduled assignments to export")

def print_teacher_workload(solution):
    """Print teacher workload summary"""
    if not solution:
        return
        
    teacher_hours = defaultdict(int)
    for assignment in solution.lecture_assignments:
        if assignment.timeslot is not None and not assignment.timeslot.is_break:
            teacher_hours[assignment.teacher] += assignment.duration_hours()
    
    print(f"\n=== TEACHER WORKLOAD SUMMARY ===")
    for teacher, hours in sorted(teacher_hours.items(), key=lambda x: x[1], reverse=True):
        status = "OVERLOADED" if hours > teacher.max_hours else "OK"
        print(f"{teacher}: {hours}/{teacher.max_hours} hours [{status}]")

def generate_studentgroup_html_timetable(solution, output_dir="studentgroup_timetables"):
    """Generate an HTML timetable for each student group"""
    import os
    if not solution:
        print("No solution to visualize")
        return
    os.makedirs(output_dir, exist_ok=True)
    days = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday']
    # Group assignments by student group
    by_group = defaultdict(list)
    for assignment in solution.lecture_assignments:
        if assignment.timeslot is not None and assignment.room is not None and not assignment.timeslot.is_break:
            by_group[assignment.student_group].append(assignment)
    for group, assignments in by_group.items():
        # Build a grid: day x time
        grid = defaultdict(lambda: defaultdict(list))
        for a in assignments:
            grid[a.timeslot.day][a.timeslot.start_time.strftime('%H:%M')] = a
        # Get all unique time slots (sorted)
        all_times = sorted({a.timeslot.start_time.strftime('%H:%M') for a in assignments})
        html = [f"<h2>Timetable for {group}</h2>", "<table border='1' style='border-collapse:collapse;'>"]
        # Header
        html.append("<tr><th>Day/Time</th>" + ''.join(f"<th>{t}</th>" for t in all_times) + "</tr>")
        for day in range(5):
            html.append(f"<tr><td>{days[day]}</td>")
            for t in all_times:
                a = grid[day].get(t)
                if a:
                    batch_info = f" (Batch {a.lab_batch})" if a.lab_batch else ""
                    html.append(f"<td>{a.course.code}<br>{a.session_type.title()}{batch_info}<br>{a.teacher}<br>{a.room}</td>")
                else:
                    html.append("<td></td>")
            html.append("</tr>")
        html.append("</table>")
        # Write to file
        filename = os.path.join(output_dir, f"timetable_{group.name.replace(' ', '_')}.html")
        with open(filename, "w", encoding="utf-8") as f:
            f.write('\n'.join(html))
    print(f"HTML timetables generated in '{output_dir}' directory.")

# ====================== Main Execution ======================

if __name__ == "__main__":
    # Replace with your actual CSV file paths
    teachers_courses_csv = "cse-1.csv"
    rooms_csv = "techlongue.csv"
    
    print("Starting timetable optimization...")
    solution = solve_timetable(teachers_courses_csv, rooms_csv)
    
    if solution:
        print("\nOptimization complete! Here are the results:")
        print_timetable(solution)
        export_to_csv(solution)
        print_teacher_workload(solution)
        generate_studentgroup_html_timetable(solution)
    else:
        print("Optimization failed. Please check your data and constraints.")