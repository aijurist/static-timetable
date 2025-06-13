# data_models.py
from optapy import (
    planning_entity, planning_variable, planning_solution, problem_fact,
    planning_entity_collection_property, value_range_provider, 
    problem_fact_collection_property, planning_score
)
from optapy.types import HardSoftScore
from datetime import time
from .config import DAYS, THEORY_TIME_SLOTS, LAB_TIME_SLOTS, CLASS_STRENGTH, SHIFT_PATTERNS, MAX_TEACHER_HOURS, SHIFTS, LAB_BATCH_SIZE, DEPARTMENT_BLOCKS, LUNCH_BREAK_SLOTS
import random

@problem_fact
class TimeSlot:
    def __init__(self, id, day, start_time, end_time, is_lab=False, slot_index=None):
        self.id = id
        self.day = day
        self.start_time = start_time
        self.end_time = end_time
        self.is_lab = is_lab
        self.slot_index = slot_index

    @property
    def start_minutes(self):
        return self.start_time.hour * 60 + self.start_time.minute

    @property
    def end_minutes(self):
        return self.end_time.hour * 60 + self.end_time.minute

    def __str__(self):
        return f"{DAYS[self.day]} {self.start_time.strftime('%H:%M')}-{self.end_time.strftime('%H:%M')}"

    @staticmethod
    def create_time_slots():
        time_slots = []
        id_counter = 0

        # Create theory time slots
        for day in range(5):
            for slot_idx, slot in enumerate(THEORY_TIME_SLOTS):
                start_str, end_str = slot
                start = time(*map(int, start_str.split(':')))
                end = time(*map(int, end_str.split(':')))
                time_slots.append(TimeSlot(
                    id_counter, day, start, end, False, slot_idx
                ))
                id_counter += 1

        # Create lab time slots
        for day in range(5):
            for slot_idx, slot in enumerate(LAB_TIME_SLOTS):
                start_str, end_str = slot
                start = time(*map(int, start_str.split(':')))
                end = time(*map(int, end_str.split(':')))
                time_slots.append(TimeSlot(
                    id_counter, day, start, end, True, slot_idx
                ))
                id_counter += 1

        return time_slots

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

@problem_fact
class Teacher:
    def __init__(self, id, staff_code, first_name, last_name, email, max_hours=MAX_TEACHER_HOURS):
        self.id = id
        self.staff_code = staff_code
        self.full_name = f"{first_name} {last_name}".strip()
        self.email = email
        self.max_hours = max_hours
        self.shift_pattern = random.choice(SHIFT_PATTERNS)
        self.day_shifts = {}  # {day_index: shift_name}
        self.assigned_courses = set()
        self.assign_shift_pattern()

    def assign_shift_pattern(self):
        """Assign shift pattern to specific days following 2-2-1, 2-1-2, or 1-2-2 combinations"""
        days = list(range(5))
        random.shuffle(days)
        
        current_day = 0
        for shift, count in self.shift_pattern.items():
            for _ in range(count):
                if current_day < len(days):
                    self.day_shifts[days[current_day]] = shift
                    current_day += 1

    def get_shift_for_day(self, day):
        return self.day_shifts.get(day)

    def is_valid_time_for_day(self, day, start_time, end_time):
        shift = self.get_shift_for_day(day)
        if shift is None:
            return False
        shift_start, shift_end = SHIFTS[shift]
        start_minutes = start_time.hour * 60 + start_time.minute
        end_minutes = end_time.hour * 60 + end_time.minute
        shift_start_minutes = shift_start.hour * 60 + shift_start.minute
        shift_end_minutes = shift_end.hour * 60 + shift_end.minute
        return shift_start_minutes <= start_minutes and end_minutes <= shift_end_minutes

    def __str__(self):
        return self.full_name

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

@problem_fact
class StudentGroup:
    def __init__(self, id, name, department, year, strength=CLASS_STRENGTH):
        self.id = id
        self.name = name
        self.department = department
        self.year = year
        self.strength = strength
        self.break_slot_index = 3 + (id % 3)  # 3,4,5 for lunch breaks
        self.break_start_minutes = self.get_break_minutes(LUNCH_BREAK_SLOTS[self.break_slot_index - 3][0])
        self.break_end_minutes = self.get_break_minutes(LUNCH_BREAK_SLOTS[self.break_slot_index - 3][1])
    
    def get_break_minutes(self, time_str):
        hour, minute = map(int, time_str.split(':'))
        return hour * 60 + minute

    def __str__(self):
        return self.name

# ======================== Planning Entity ========================
@planning_entity
class LectureAssignment:
    def __init__(self, id, course, teacher, student_group, session_type="lecture", lab_batch=None, parent_lab_id=None):
        self.id = id
        self.course = course
        self.teacher = teacher
        self.student_group = student_group
        self.session_type = session_type
        self.lab_batch = lab_batch
        self.parent_lab_id = parent_lab_id
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
        if self.session_type == "lab":
            return 2  # Labs are 2 hours long
        return 1

    def required_capacity(self):
        if self.session_type == "lab":
            if self.course.practical_hours == 6:
                return self.student_group.strength  # Whole class for 6-hour labs
            return LAB_BATCH_SIZE  # Batched labs
        return self.student_group.strength

    def is_lab(self):
        return self.session_type == "lab"
    
    def get_lab_batch_display(self):
        if self.session_type == "lab":
            return f"Lab Batch {self.lab_batch}" if self.lab_batch else "Lab"
        return ""

# ======================== Planning Solution ========================
@planning_solution
class TimeTable:
    def __init__(self, lecture_assignments, timeslots, rooms, teachers, courses, student_groups, score=None):
        self.lecture_assignments = lecture_assignments
        self.timeslots = timeslots
        self.rooms = rooms
        self.teachers = teachers
        self.courses = courses
        self.student_groups = student_groups
        self.score = score or HardSoftScore.ZERO

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