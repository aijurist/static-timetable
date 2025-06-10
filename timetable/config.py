from datetime import time

DAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday']

# Theory time slots (11 slots)
THEORY_TIME_SLOTS = [
    ("8:00", "8:50"),     # Period 1
    ("9:00", "9:50"),     # Period 2
    ("10:00", "10:50"),   # Period 3
    ("11:00", "11:50"),   # Period 4
    ("12:00", "12:50"),   # Period 5
    ("13:00", "13:50"),   # Period 6
    ("14:00", "14:50"),   # Period 7
    ("15:00", "15:50"),   # Period 8
    ("16:00", "16:50"),   # Period 9
    ("17:00", "17:50"),   # Period 10
    ("18:00", "18:50")    # Period 11
]

# Lab time slots (12 slots with 2 continuous slots per lab)
LAB_TIME_SLOTS = [
    ("8:00", "9:40"),     # Lab 1 (2 slots)
    ("9:50", "11:30"),    # Lab 2 (2 slots)
    ("11:50", "13:30"),   # Lab 3 (2 slots)
    ("13:50", "15:30"),   # Lab 4 (2 slots)
    ("15:50", "17:30"),   # Lab 5 (2 slots)
    ("17:50", "19:30")    # Lab 6 (2 slots)
]

MAX_TEACHER_HOURS = 21
CLASS_STRENGTH = 70
LAB_BATCH_SIZE = 35
LAB_DURATION_SLOTS = 2  # Each lab takes 2 continuous slots

# Teacher shift configurations
SHIFTS = {
    'MORNING': (time(8, 0), time(15, 0)),    # 8 AM - 3 PM
    'AFTERNOON': (time(10, 0), time(17, 0)),  # 10 AM - 5 PM
    'EVENING': (time(12, 0), time(19, 0))     # 12 PM - 7 PM
}

SHIFT_PATTERNS = [
    # 2-2-1 patterns
    {'MORNING': 2, 'AFTERNOON': 2, 'EVENING': 1},
    {'MORNING': 2, 'EVENING': 2, 'AFTERNOON': 1},
    {'AFTERNOON': 2, 'EVENING': 2, 'MORNING': 1},
    # 2-1-2 patterns
    {'MORNING': 2, 'AFTERNOON': 1, 'EVENING': 2},
    {'MORNING': 2, 'EVENING': 1, 'AFTERNOON': 2},
    {'AFTERNOON': 2, 'MORNING': 1, 'EVENING': 2},
    # 1-2-2 patterns
    {'MORNING': 1, 'AFTERNOON': 2, 'EVENING': 2},
    {'MORNING': 1, 'EVENING': 2, 'AFTERNOON': 2},
    {'AFTERNOON': 1, 'EVENING': 2, 'MORNING': 2}
]# Each lab takes 2 continuous slots