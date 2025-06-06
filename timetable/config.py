from datetime import time

DAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday']
TIME_SLOTS = [
    ("8:10", "9:00"),     # Period 1
    ("9:00", "9:50"),     # Period 2
    ("9:50", "10:10", True),  # Morning break (20 min)
    ("10:10", "11:00"),   # Period 3
    ("11:00", "11:50"),   # Period 4
    ("11:50", "12:40", True),  # Lunch break (50 min)
    ("12:40", "13:30"),   # Period 5
    ("13:30", "14:20"),   # Period 6
    ("14:20", "15:10")    # Period 7
]
MAX_TEACHER_HOURS = 21
CLASS_STRENGTH = 70
LAB_BATCH_SIZE = 35
LAB_DURATION_SLOTS = 2