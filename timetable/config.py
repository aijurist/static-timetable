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
    ("8:00", "9:40"),     
    ("9:50", "11:30"),    
    ("11:50", "13:30"),   
    ("13:50", "15:30"),   
    ("15:50", "17:30"),   
    ("17:50", "19:30")    
]

MAX_TEACHER_HOURS = 21
CLASS_STRENGTH = 70
LAB_BATCH_SIZE = 35
LAB_DURATION_SLOTS = 2 

# Teacher shift configurations
SHIFTS = {
    'MORNING': (time(8, 0), time(15, 0)),    
    'AFTERNOON': (time(10, 0), time(17, 0)), 
    'EVENING': (time(12, 0), time(19, 0))    
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
]

DEPARTMENT_DATA = {
    "CSE-CS": {
        "2": 2,
        "3": 1
    },
    "CSE": {
        "2": 10,
        "3": 6,
        "4": 5
    },
    "CSBS": {
        "2": 2,
        "3": 2,
        "4": 2
    },
    "CSD": {
        "2": 1,
        "3": 1,
        "4": 1
    },
    "IT": {
        "2": 5,
        "3": 4,
        "4": 3
    },
    "AIML": {
        "2": 4,
        "3": 3,
        "4": 3
    },
    "AIDS": {
        "2": 5,
        "3": 3,
        "4": 1
    },
    "ECE": {
        "2": 6,
        "3": 4,
        "4": 4
    },
    "EEE": {
        "2": 2,
        "3": 2,
        "4": 2
    },
    "AERO": {
        "2": 1,
        "3": 1,
        "4": 1
    },
    "AUTO": {
        "2": 1,
        "3": 1,
        "4": 1
    },
    "MCT": {
        "2": 1,
        "3": 1,
        "4": 1
    },
    "MECH": {
        "2": 2,
        "3": 2,
        "4": 2
    },
    "BT": {
        "2": 3,
        "3": 3,
        "4": 3
    },
    "BME": {
        "2": 2,
        "3": 2,
        "4": 2
    },
    "R&A": {
        "2": 1,
        "3": 1,
        "4": 1
    },
    "FT": {
        "2": 1,
        "3": 1,
        "4": 1
    },
    "CIVIL": {
        "2": 1,
        "3": 1,
        "4": 1
    },
    "CHEM": {
        "2": 1,
        "3": 1,
        "4": 1
    }
}
