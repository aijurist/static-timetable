# constraints.py
from optapy.constraint import ConstraintFactory
from optapy.types import Joiners, HardSoftScore
from .data_models import LectureAssignment, Teacher, StudentGroup
from optapy.constraint import ConstraintCollectors
from optapy.annotations import constraint_provider
from .config import DEPARTMENT_BLOCKS

def get_gap_between_slots(slot1, slot2):
    if slot1 is None or slot2 is None or slot1.day != slot2.day:
        return 0
    slot1_end = slot1.end_time.hour * 60 + slot1.end_time.minute
    slot2_start = slot2.start_time.hour * 60 + slot2.start_time.minute
    gap_minutes = slot2_start - slot1_end
    return max(0, gap_minutes // 50)  # Assuming 50-minute theory slots

def timeslot_overlap(slot1, slot2):
    """Return True if two TimeSlot objects overlap in time on the same day."""
    if slot1 is None or slot2 is None or slot1.day != slot2.day:
        return False
    start1 = slot1.start_minutes
    end1   = slot1.end_minutes
    start2 = slot2.start_minutes
    end2   = slot2.end_minutes
    return (start1 < end2) and (start2 < end1)

# —— Hard time‐overlap constraints ——

def teacher_time_overlap(constraint_factory: ConstraintFactory):
    return (
        constraint_factory
        .for_each(LectureAssignment)
        .join(
            LectureAssignment,
            [Joiners.equal(lambda a: a.teacher),
             Joiners.less_than(lambda a: a.id)]
        )
        .filter(lambda a1, a2:
            a1.timeslot is not None
            and a2.timeslot is not None
            and timeslot_overlap(a1.timeslot, a2.timeslot)
        )
        .penalize("Teacher time overlap", HardSoftScore.ONE_HARD)
    )

def student_group_time_overlap(constraint_factory: ConstraintFactory):
    return (
        constraint_factory
        .for_each(LectureAssignment)
        .join(
            LectureAssignment,
            [Joiners.equal(lambda a: a.student_group),
             Joiners.less_than(lambda a: a.id)]
        )
        .filter(lambda a1, a2:
            a1.timeslot is not None
            and a2.timeslot is not None
            and timeslot_overlap(a1.timeslot, a2.timeslot)
        )
        .penalize("Student group time overlap", HardSoftScore.ONE_HARD)
    )

def room_time_overlap(constraint_factory: ConstraintFactory):
    return (
        constraint_factory
        .for_each(LectureAssignment)
        .join(
            LectureAssignment,
            [Joiners.equal(lambda a: a.room),
             Joiners.less_than(lambda a: a.id)]
        )
        .filter(lambda a1, a2:
            a1.room is not None
            and a2.room is not None
            and a1.timeslot is not None
            and a2.timeslot is not None
            and timeslot_overlap(a1.timeslot, a2.timeslot)
        )
        .penalize("Room time overlap", HardSoftScore.ONE_HARD)
    )

def teacher_max_hours(constraint_factory):
    return (
        constraint_factory
        .for_each(LectureAssignment)
        .filter(lambda a: a.timeslot is not None)
        .group_by(
            lambda a: a.teacher,
            ConstraintCollectors.sum(lambda a: a.duration_hours())
        )
        .filter(lambda teacher, total_hours: total_hours > teacher.max_hours)
        .penalize(
            "Teacher max hours exceeded",
            HardSoftScore.ONE_HARD,
            lambda teacher, total_hours: total_hours - teacher.max_hours
        )
    )

def lab_in_lab_room(constraint_factory):
    return (
        constraint_factory.for_each(LectureAssignment)
        .filter(lambda a:
            a.session_type == "lab" and 
            a.room is not None and 
            not a.room.is_lab
        )
        .penalize("Lab not in lab room", HardSoftScore.ONE_HARD)
    )

def lecture_in_classroom(constraint_factory):
    return (
        constraint_factory.for_each(LectureAssignment)
        .filter(lambda a:
            a.session_type != "lab" and 
            a.room is not None and 
            a.room.is_lab
        )
        .penalize("Lecture in lab room", HardSoftScore.ONE_HARD)
    )

def room_capacity(constraint_factory):
    return (
        constraint_factory.for_each(LectureAssignment)
        .filter(lambda a:
            a.room is not None and 
            a.required_capacity() > a.room.max_cap
        )
        .penalize("Room capacity exceeded", HardSoftScore.ONE_HARD)
    )

def lab_in_valid_time_block(constraint_factory):
    return (
        constraint_factory.for_each(LectureAssignment)
        .filter(lambda a: (
            a.session_type == "lab" and
            a.timeslot is not None and
            not a.timeslot.is_lab
        ))
        .penalize("Lab not in valid time block", HardSoftScore.ONE_HARD)
    )

def lecture_in_valid_time_block(constraint_factory):
    return (
        constraint_factory.for_each(LectureAssignment)
        .filter(lambda a: (
            a.session_type != "lab" and
            a.timeslot is not None and
            a.timeslot.is_lab
        ))
        .penalize("Lecture in lab time block", HardSoftScore.ONE_HARD)
    )

def consecutive_lab_parts(constraint_factory):
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda a: a.session_type == "lab" and a.parent_lab_id is not None) \
        .join(
            LectureAssignment,
            [Joiners.equal(lambda a: a.parent_lab_id),
             Joiners.equal(lambda a: a.student_group),
             Joiners.equal(lambda a: a.course),
             Joiners.less_than(lambda a: a.id)]
        ) \
        .filter(lambda a1, a2: (
            a1.timeslot is None or 
            a2.timeslot is None or
            a1.timeslot.day != a2.timeslot.day or
            abs(a1.timeslot.slot_index - a2.timeslot.slot_index) != 1
        )) \
        .penalize("Lab parts not consecutive", HardSoftScore.ONE_HARD)

def different_rooms_for_lab_batches(constraint_factory):
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda a: a.session_type == "lab" and a.lab_batch is not None) \
        .join(
            LectureAssignment,
            [Joiners.equal(lambda a: a.course),
             Joiners.equal(lambda a: a.student_group),
             Joiners.equal(lambda a: a.timeslot),
             Joiners.less_than(lambda a: a.id)]
        ) \
        .filter(lambda a1, a2:
               a1.timeslot is not None and a2.timeslot is not None and
               a1.lab_batch != a2.lab_batch and
               a1.room == a2.room
        ) \
        .penalize("Same room for different lab batches at same time", HardSoftScore.ONE_HARD)

def enforce_lecture_hours(constraint_factory):
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda a: a.session_type == "lecture") \
        .group_by(
            lambda a: (a.student_group, a.course),
            ConstraintCollectors.count()
        ) \
        .filter(lambda group_course_key, count: count != group_course_key[1].lecture_hours) \
        .penalize(
            "Incorrect lecture hours",
            HardSoftScore.ONE_HARD,
            lambda group_course_key, count: abs(count - group_course_key[1].lecture_hours)
        )

def enforce_tutorial_hours(constraint_factory):
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda a: a.session_type == "tutorial") \
        .group_by(
            lambda a: (a.student_group, a.course),
            ConstraintCollectors.count()
        ) \
        .filter(lambda group_course_key, count: count != group_course_key[1].tutorial_hours) \
        .penalize(
            "Incorrect tutorial hours",
            HardSoftScore.ONE_HARD,
            lambda group_course_key, count: abs(count - group_course_key[1].tutorial_hours)
        )

def enforce_practical_hours(constraint_factory):
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda a: a.session_type == "lab") \
        .group_by(
            lambda a: (a.student_group, a.course, a.lab_batch),
            ConstraintCollectors.count()
        ) \
        .filter(lambda group_course_batch_key, count: 
                count != group_course_batch_key[1].practical_hours
        ) \
        .penalize(
            "Incorrect practical hours per batch/unsplit lab",
            HardSoftScore.ONE_HARD,
            lambda group_course_batch_key, count: abs(count - group_course_batch_key[1].practical_hours)
        )

def minimize_teacher_gaps(constraint_factory):
    return constraint_factory.for_each(LectureAssignment) \
        .join(
            LectureAssignment,
            [Joiners.equal(lambda a1: a1.teacher),
             Joiners.equal(
                lambda a1: a1.timeslot.day if a1.timeslot is not None else -1,
                lambda a2: a2.timeslot.day if a2.timeslot is not None else -1
            ),
             Joiners.less_than(
                lambda a1: a1.timeslot.start_minutes if a1.timeslot is not None else -1,
                lambda a2: a2.timeslot.start_minutes if a2.timeslot is not None else -1
            )]
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
    return constraint_factory.for_each(LectureAssignment) \
        .join(
            LectureAssignment,
            [Joiners.equal(lambda a1: a1.student_group),
             Joiners.equal(
                lambda a1: a1.timeslot.day if a1.timeslot is not None else -1,
                lambda a2: a2.timeslot.day if a2.timeslot is not None else -1
            ),
             Joiners.less_than(
                lambda a1: a1.timeslot.start_minutes if a1.timeslot is not None else -1,
                lambda a2: a2.timeslot.start_minutes if a2.timeslot is not None else -1
            )]
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

def teacher_shift_constraint(constraint_factory):
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda a: a.timeslot is not None) \
        .filter(lambda a: not a.teacher.is_valid_time_for_day(
            a.timeslot.day, a.timeslot.start_time, a.timeslot.end_time)) \
        .penalize("Teacher shift violation", HardSoftScore.ONE_HARD)

def no_batching_for_6hr_labs(constraint_factory):
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda a:
                a.session_type == "lab" and
                a.course.practical_hours == 6 and
                a.lab_batch is not None
        ) \
        .penalize("6 P.H. lab incorrectly batched", HardSoftScore.ofHard(100))

def unsplit_6hr_practical_room_capacity(constraint_factory):
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda a:
                a.session_type == "lab" and
                a.course.practical_hours == 6 and
                a.lab_batch is None and
                a.room is not None and
                a.room.max_cap < 70
        ) \
        .penalize("Unsplit 6hr lab not in room with capacity >= 70", HardSoftScore.ofHard(50))

def student_group_break_violation(constraint_factory):
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda a: a.timeslot is not None) \
        .filter(lambda a: 
            a.timeslot.start_minutes < a.student_group.break_end_minutes and 
            a.timeslot.end_minutes > a.student_group.break_start_minutes
        ) \
        .penalize("Student group break violation", HardSoftScore.ONE_HARD)

def room_block_preference(constraint_factory):
    def get_preferred_block(dept):
        return DEPARTMENT_BLOCKS.get(dept)
    
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda a: a.room is not None) \
        .penalize("Room not in preferred block", HardSoftScore.ONE_SOFT,
                  lambda a: 0 if (get_preferred_block(a.course.dept) is None or 
                                 a.room.block == get_preferred_block(a.course.dept)) 
                            else 1)

@constraint_provider
def timetable_constraints(constraint_factory: ConstraintFactory):
    return [
        # Hard constraints
        teacher_time_overlap(constraint_factory),
        student_group_time_overlap(constraint_factory),
        room_time_overlap(constraint_factory),
        teacher_max_hours(constraint_factory),
        lab_in_lab_room(constraint_factory),
        lecture_in_classroom(constraint_factory),
        room_capacity(constraint_factory),
        lab_in_valid_time_block(constraint_factory),
        lecture_in_valid_time_block(constraint_factory),
        consecutive_lab_parts(constraint_factory),
        different_rooms_for_lab_batches(constraint_factory),
        enforce_lecture_hours(constraint_factory),
        enforce_tutorial_hours(constraint_factory),
        enforce_practical_hours(constraint_factory),
        teacher_shift_constraint(constraint_factory),
        no_batching_for_6hr_labs(constraint_factory),
        unsplit_6hr_practical_room_capacity(constraint_factory),
        student_group_break_violation(constraint_factory),

        # Soft constraints
        minimize_teacher_gaps(constraint_factory),
        prefer_consecutive_classes(constraint_factory),
        room_block_preference(constraint_factory),
    ]