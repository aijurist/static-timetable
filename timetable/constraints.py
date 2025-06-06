from optapy.constraint import ConstraintFactory
from optapy.types import Joiners, HardSoftScore
from .data_models import LectureAssignment # Assuming data_models is in the same directory or package
from optapy.constraint import ConstraintCollectors

def get_gap_between_slots(slot1, slot2):
    if slot1 is None or slot2 is None or slot1.day != slot2.day:
        return 0
    slot1_end = slot1.end_time.hour * 60 + slot1.end_time.minute
    slot2_start = slot2.start_time.hour * 60 + slot2.start_time.minute
    gap_minutes = slot2_start - slot1_end
    return max(0, gap_minutes // 50) # Assuming 50-minute slots

def teacher_conflict(constraint_factory):
    return constraint_factory.for_each(LectureAssignment) \
        .join(
            LectureAssignment,
            Joiners.equal(lambda a: a.teacher),
            Joiners.equal(lambda a: a.timeslot),
            Joiners.less_than(lambda a: a.id)
        ) \
        .filter(lambda a1, a2: a1.timeslot is not None and a2.timeslot is not None) \
        .penalize("Teacher conflict", HardSoftScore.ONE_HARD)

def room_conflict(constraint_factory):
    return constraint_factory.for_each(LectureAssignment) \
        .join(
            LectureAssignment,
            Joiners.equal(lambda a: a.room),
            Joiners.equal(lambda a: a.timeslot),
            Joiners.less_than(lambda a: a.id)
        ) \
        .filter(lambda a1, a2: 
               a1.room is not None and a2.room is not None and
               a1.timeslot is not None and a2.timeslot is not None
        ) \
        .penalize("Room conflict", HardSoftScore.ONE_HARD)

def student_group_conflict(constraint_factory):
    return constraint_factory.for_each(LectureAssignment) \
        .join(
            LectureAssignment,
            Joiners.equal(lambda a: a.student_group),
            Joiners.equal(lambda a: a.timeslot),
            Joiners.less_than(lambda a: a.id)
        ) \
        .filter(lambda a1, a2: a1.timeslot is not None and a2.timeslot is not None) \
        .penalize("Student group conflict", HardSoftScore.ONE_HARD)

def teacher_max_hours(constraint_factory):
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda a: a.timeslot is not None) \
        .group_by(
            lambda a: a.teacher,
            ConstraintCollectors.sum(lambda a: a.duration_hours())
        ) \
        .filter(lambda teacher, total_hours: total_hours > teacher.max_hours) \
        .penalize(
            "Teacher max hours exceeded",
            HardSoftScore.ONE_HARD,
            lambda teacher, total_hours: total_hours - teacher.max_hours
        )

def lab_in_lab_room(constraint_factory):
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda a:
               a.session_type == "lab" and 
               a.room is not None and 
               not a.room.is_lab
        ) \
        .penalize("Lab not in lab room", HardSoftScore.ONE_HARD)

def lecture_in_classroom(constraint_factory):
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda a:
               a.session_type != "lab" and 
               a.room is not None and 
               a.room.is_lab
        ) \
        .penalize("Lecture in lab room", HardSoftScore.ONE_HARD)

def room_capacity(constraint_factory):
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda a:
               a.room is not None and 
               a.required_capacity() > a.room.max_cap
        ) \
        .penalize("Room capacity exceeded", HardSoftScore.ONE_HARD)

def no_classes_during_breaks(constraint_factory):
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda a:
               a.timeslot is not None and 
               a.timeslot.is_break
        ) \
        .penalize("Class during break", HardSoftScore.ONE_HARD)

def is_valid_lab_block(timeslot):
    """Check if timeslot is in a valid lab block"""
    if timeslot is None:
        return False
    # Lab blocks: 
    # 1. 8:10-9:50 (slots 0-1)
    # 2. 10:10-11:50 (slots 3-4)
    # 3. 12:40-15:10 (slots 6-8)
    # This might need adjustment if 6-hour labs are single, continuous blocks
    # outside these typical smaller blocks.
    return timeslot.slot_index in (0, 1, 3, 4, 6, 7, 8) # Example slot indices

def lab_in_valid_time_block(constraint_factory):
    """Ensure labs are only scheduled in designated lab blocks"""
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda a: (
            a.session_type == "lab"
            and a.timeslot is not None
            and not is_valid_lab_block(a.timeslot)
        ) is True) \
        .penalize("Lab not in valid time block", HardSoftScore.ONE_HARD)

def consecutive_lab_slots(constraint_factory):
    """Ensure lab sessions are scheduled in consecutive 2-hour blocks within valid lab periods"""
    # This REWARDS consecutive slots. The non_consecutive_lab_penalty (HARD) below enforces it.
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda a: a.session_type == "lab" and a.parent_lab_id is not None) \
        .join(
            LectureAssignment,
            Joiners.equal(lambda a: a.parent_lab_id),
            Joiners.equal(lambda a: a.student_group),
            Joiners.equal(lambda a: a.teacher),
            Joiners.less_than(lambda a: a.id)
        ) \
        .filter(lambda a1, a2: (
            a1.timeslot is not None and a2.timeslot is not None and
            a1.timeslot.day == a2.timeslot.day and
            abs(a1.timeslot.slot_index - a2.timeslot.slot_index) == 1 and # Consecutive by slot index
            is_valid_lab_block(a1.timeslot) and # Both parts in valid lab block time
            is_valid_lab_block(a2.timeslot)
        ) is True) \
        .reward("Consecutive lab slots in valid period", HardSoftScore.ONE_SOFT)

# FIXED: Key improvement - properly handles unscheduled lab parts
def non_consecutive_lab_penalty(constraint_factory):
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda a: a.session_type == "lab" and a.parent_lab_id is not None) \
        .join(
            LectureAssignment,
            Joiners.equal(lambda a: a.parent_lab_id),
            Joiners.equal(lambda a: a.student_group),
            Joiners.equal(lambda a: a.course),
            Joiners.less_than(lambda a: a.id)
        ) \
        .filter(lambda a1, a2: (
            a1.timeslot is None or  # Penalize if either part is unscheduled
            a2.timeslot is None or
            a1.timeslot.day != a2.timeslot.day or  # Different days
            abs(a1.timeslot.slot_index - a2.timeslot.slot_index) != 1 or  # Not consecutive
            not is_valid_lab_block(a1.timeslot) or  # Invalid time block
            not is_valid_lab_block(a2.timeslot)
        )) \
        .penalize("Lab parts not scheduled consecutively in valid period", 
                 HardSoftScore.ONE_HARD)

def different_rooms_for_lab_batches(constraint_factory):
    """Ensure different lab batches of same course/group use different rooms if scheduled at the same time"""
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda a: a.session_type == "lab" and a.lab_batch is not None) \
        .join(
            LectureAssignment,
            Joiners.equal(lambda a: a.course),
            Joiners.equal(lambda a: a.student_group),
            Joiners.equal(lambda a: a.timeslot), # At the same timeslot
            Joiners.less_than(lambda a: a.id)    # Avoid duplicates
        ) \
        .filter(lambda a1, a2:
               a1.timeslot is not None and a2.timeslot is not None and # Redundant if Joiners.equal(timeslot)
               a1.lab_batch != a2.lab_batch and # Different batches
               a1.room == a2.room                 # But in the same room
        ) \
        .penalize("Same room for different lab batches at same time", HardSoftScore.ONE_HARD)

# NEW: Protect lab blocks from non-lab sessions
def protect_lab_blocks(constraint_factory):
    """Prevent non-lab sessions from being scheduled during lab blocks"""
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda a: a.session_type == "lab") \
        .join(LectureAssignment,
            Joiners.equal(lambda a: a.timeslot),
            Joiners.equal(lambda a: a.student_group),
            Joiners.less_than(lambda a: a.id)
        ) \
        .filter(lambda lab, other: 
            other.session_type != "lab" and  # Non-lab session
            lab.timeslot is not None and
            other.timeslot is not None
        ) \
        .penalize("Non-lab session during lab block", HardSoftScore.ONE_HARD)

def enforce_lecture_hours(constraint_factory):
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda a: a.session_type == "lecture") \
        .group_by(
            lambda a: (a.student_group, a.course),
            ConstraintCollectors.count() # Counts number of lecture assignments (assuming 1 hour each)
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
    """Ensure correct total practical hours per lab batch (or for whole class if not batched)"""
    # Assumes each LectureAssignment of type 'lab' is 1 hour.
    # If a 6 P.H. lab is not batched, lab_batch will be None.
    # If a 2 P.H. lab is batched, lab_batch will be 'B1' or 'B2'.
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda a: a.session_type == "lab") \
        .group_by(
            # Group by (student_group, course, lab_batch).
            # lab_batch can be None for unsplit 6 P.H. labs.
            lambda a: (a.student_group, a.course, a.lab_batch),
            ConstraintCollectors.count() # Counts number of lab assignments (1 hour each)
        ) \
        .filter(lambda group_course_batch_key, count: 
                count != group_course_batch_key[1].practical_hours # Compare with course's total practical_hours
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
            Joiners.equal(lambda a1: a1.teacher, lambda a2: a2.teacher),
            Joiners.equal(
                lambda a1: a1.timeslot.day if a1.timeslot is not None else -1,
                lambda a2: a2.timeslot.day if a2.timeslot is not None else -1
            ),
            Joiners.less_than( # Ensure a2 is after a1
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

def prefer_consecutive_classes(constraint_factory): # For student groups
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

def prefer_same_room_for_course(constraint_factory): # For lectures/tutorials
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
               a1.session_type != "lab" and # Only for non-lab sessions
               a2.session_type != "lab"
        ) \
        .penalize("Course (non-lab) in different rooms", HardSoftScore.ONE_SOFT)

def prefer_different_lab_batches(constraint_factory): # This seems to penalize if different batches are at same time (already handled by room_conflict with different_rooms_for_lab_batches)
    # This constraint, as written, penalizes if two *different* lab batches of the *same course* and *same group*
    # are scheduled in the *same timeslot*. This would only make sense if they are in different rooms.
    # If they are in the same room, `different_rooms_for_lab_batches` or `room_conflict` would fire.
    # If the goal is to spread out lab batches of the same course over time, this might need rethinking.
    # Original logic:
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda a: a.session_type == "lab" and a.lab_batch is not None) \
        .join(
            LectureAssignment,
            Joiners.equal(lambda a: a.course),
            Joiners.equal(lambda a: a.student_group),
            Joiners.equal(lambda a: a.timeslot), # Same timeslot
            Joiners.less_than(lambda a: a.id)
        ) \
        .filter(lambda a1, a2:
               a1.timeslot is not None and a2.timeslot is not None and
               a1.lab_batch != a2.lab_batch # Different batches
        ) \
        .penalize("Overlapping different lab batches of same course (soft)", HardSoftScore.ONE_SOFT) # This is soft

def no_lab_during_breaks(constraint_factory):
    """Prevent labs from being scheduled during break times"""
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda a:
               a.session_type == "lab" and
               a.timeslot is not None and
               a.timeslot.is_break
        ) \
        .penalize("Lab during break", HardSoftScore.ONE_HARD)

# CRITICAL: Prevent batching of 6 P.H. labs - they must be unsplit
def no_batching_for_6hr_labs(constraint_factory):
    """Prevent 6 P.H. labs from being split into batches - they must remain as whole class"""
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda a:
                a.session_type == "lab" and
                a.course.practical_hours == 6 and  # Course has 6 practical hours
                a.lab_batch is not None  # But it's been batched (this should not happen)
        ) \
        .penalize("6 P.H. lab incorrectly batched - must be whole class", HardSoftScore.ofHard(100))

# CRITICAL: Ensure unsplit 6 P.H. labs get large capacity rooms
def unsplit_6hr_practical_room_capacity(constraint_factory):
    """Ensure 6 P.H. labs (unsplit) are assigned to rooms with capacity >= 70"""
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda a:
                a.session_type == "lab" and
                a.course.practical_hours == 6 and  # Course is a 6 P.H. lab course
                a.lab_batch is None and  # Signifies an unsplit lab for the whole class
                a.room is not None and
                a.room.max_cap < 70  # Room capacity insufficient for whole class
        ) \
        .penalize("Unsplit 6hr lab not in room with capacity >= 70", HardSoftScore.ofHard(50))

# HIGH PRIORITY: Ensure 6 P.H. labs get scheduled first in valid blocks
def prioritize_6hr_labs_scheduling(constraint_factory):
    """Give highest priority to scheduling 6 P.H. labs in valid time blocks"""
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda a:
                a.session_type == "lab" and
                a.course.practical_hours == 6 and
                a.lab_batch is None and  # Unsplit lab
                a.timeslot is None  # Not yet scheduled
        ) \
        .penalize("6 P.H. lab not scheduled - highest priority", HardSoftScore.ofHard(200))

# Prevent other sessions from using large lab rooms when 6 P.H. labs need them
def reserve_large_labs_for_6hr_practicals(constraint_factory):
    """Reserve large capacity lab rooms (>=70) primarily for 6 P.H. unsplit labs"""
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda a:
                a.session_type == "lab" and
                a.course.practical_hours != 6 and  # Not a 6 P.H. course
                a.room is not None and
                a.room.is_lab and
                a.room.max_cap >= 70  # Using a large lab room
        ) \
        .join(LectureAssignment,
              Joiners.equal(lambda a: a.timeslot),  # Same timeslot
              Joiners.less_than(lambda a: a.id)
        ) \
        .filter(lambda small_lab, six_hr_lab:
                six_hr_lab.session_type == "lab" and
                six_hr_lab.course.practical_hours == 6 and
                six_hr_lab.lab_batch is None and
                six_hr_lab.timeslot is not None
        ) \
        .penalize("Large lab room should be reserved for 6 P.H. labs", HardSoftScore.ofHard(10))

# UPDATED: Constraint to ensure all parts of a multi-part lab session are in the same room
def same_room_for_all_parts_of_a_lab(constraint_factory):
    """Ensure all parts of a lab session use the same room"""
    return constraint_factory.for_each(LectureAssignment) \
        .filter(lambda la: la.session_type == "lab" and la.parent_lab_id is not None and la.room is not None) \
        .join(LectureAssignment,
              Joiners.equal(lambda la_join: la_join.parent_lab_id),
              Joiners.equal(lambda la_join: la_join.student_group), # Essential for correct pairing
              Joiners.equal(lambda la_join: la_join.course),         # Essential for correct pairing
              Joiners.equal(lambda la_join: la_join.lab_batch),      # Handles None == None for unsplit labs
              Joiners.less_than(lambda la_join: la_join.id)          # Avoid duplicate pairs and self-join
        ) \
        .filter(lambda la1, la2: la2.room is not None and la1.room != la2.room) \
        .penalize("Multipart lab session (same parent_lab_id) in different rooms", HardSoftScore.ONE_HARD)

from optapy import constraint_provider # Ensure this import is present

@constraint_provider
def timetable_constraints(constraint_factory: ConstraintFactory):
    return [
        # HARD CONSTRAINTS - Ordered by priority as per recommendations
        
        # Priority 1-3: Core conflict prevention
        teacher_conflict(constraint_factory),
        room_conflict(constraint_factory),
        student_group_conflict(constraint_factory),
        
        # Priority 4-7: Lab scheduling (FIXED)
        non_consecutive_lab_penalty(constraint_factory),  # FIXED - Now properly handles unscheduled parts
        enforce_practical_hours(constraint_factory),
        same_room_for_all_parts_of_a_lab(constraint_factory),
        lab_in_valid_time_block(constraint_factory),
        
        # Priority 8-9: Resource constraints
        teacher_max_hours(constraint_factory),
        room_capacity(constraint_factory),
        
        # Additional hard constraints
        lab_in_lab_room(constraint_factory),
        lecture_in_classroom(constraint_factory),
        no_classes_during_breaks(constraint_factory),
        different_rooms_for_lab_batches(constraint_factory),
        no_lab_during_breaks(constraint_factory),
        enforce_lecture_hours(constraint_factory),
        enforce_tutorial_hours(constraint_factory),
        unsplit_6hr_practical_room_capacity(constraint_factory),
        
        # NEW: Protect lab blocks from interference
        protect_lab_blocks(constraint_factory),
        
        # SOFT CONSTRAINTS - Optimization preferences
        consecutive_lab_slots(constraint_factory),  # Rewards proper lab scheduling
        minimize_teacher_gaps(constraint_factory),
        prefer_consecutive_classes(constraint_factory),
        prefer_same_room_for_course(constraint_factory),
        prefer_different_lab_batches(constraint_factory)
    ]