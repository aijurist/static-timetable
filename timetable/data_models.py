from optapy import (
    planning_entity, planning_variable, planning_solution, problem_fact,
    planning_entity_collection_property, value_range_provider, 
    problem_fact_collection_property, planning_score
)
from optapy.types import HardSoftScore
from datetime import time
from .config import TIME_SLOTS

@problem_fact
class TimeSlot:
    def __init__(self, id, day, start_time, end_time, is_break=False, slot_index=None):
        self.id = id
        self.day = day
        self.start_time = start_time
        self.end_time = end_time
        self.is_break = is_break
        self.slot_index = slot_index

    @property
    def start_minutes(self):
        return self.start_time.hour * 60 + self.start_time.minute

    @property
    def end_minutes(self):
        return self.end_time.hour * 60 + self.end_time.minute
    
    def create_time_slots():
        time_slots = []
        id_counter = 0

        for day in range(5):
            for slot_idx, slot in enumerate(TIME_SLOTS):
                if len(slot) == 2:
                    start_str, end_str = slot
                    is_break = False
                else:
                    start_str, end_str, is_break = slot

                start = time(*map(int, start_str.split(':')))
                end = time(*map(int, end_str.split(':')))
                time_slots.append(TimeSlot(
                    id_counter, day, start, end, is_break, slot_idx  # Add slot_index here
                ))
                id_counter += 1

        return time_slots

    def __str__(self):
        return f"{['Mon', 'Tue', 'Wed', 'Thu', 'Fri'][self.day]} {self.start_time.strftime('%H:%M')}-{self.end_time.strftime('%H:%M')}"

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
    def __init__(self, id, staff_code, first_name, last_name, email, max_hours=21):
        self.id = id if id != "Unknown" else "Unknown"
        self.staff_code = staff_code  # Fix typo here
        self.full_name = f"{first_name} {last_name}".strip()
        self.email = email
        self.max_hours = max_hours

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
    def __init__(self, id, name, strength=70):
        self.id = id
        self.name = name
        self.strength = strength

    def __str__(self):
        return self.name

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
        return 1

    def required_capacity(self):
        if self.session_type == "lab":
            return 35
        return self.student_group.strength

    def is_lab(self):
        return self.session_type == "lab"
    
    def is_lab_part(self):
        return self.session_type == "lab" and self.parent_lab_id is not None

    def is_first_lab_part(self):
        return self.is_lab_part() and self.id % 2 == 0

    def is_second_lab_part(self):
        return self.is_lab_part() and self.id % 2 == 1

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