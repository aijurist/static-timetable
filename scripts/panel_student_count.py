#!/usr/bin/env python3
"""
Analyze timetable.json and report, for each day, how many student groups are scheduled in three time panels:
  • 8-3  (08:00 – 14:59)
  • 3-5  (15:00 – 16:59)
  • 5-7  (17:00 – 19:10)
The script prints a CSV table: Day, panel_8_3, panel_3_5, panel_5_7

Assumptions:
• timetable.json is at output/timetable.json relative to repo root.
• Each lesson record has keys: "day", "startTime", "group".
• A student group might appear multiple times within a panel (different lessons). We count the **unique groups** per panel per day to approximate student head-count. If group sizes are required later we can extend the script when that data is available.
"""
import json
import os
from collections import defaultdict
from datetime import time

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JSON_PATH = os.path.join(ROOT, "output", "timetable.json")

with open(JSON_PATH, "r", encoding="utf-8") as fh:
    data = json.load(fh)

# Helper: classify start time into panel label
def classify_panel(t: str) -> str:
    h, m = map(int, t.split(":"))
    t_obj = time(h, m)
    if t_obj < time(15, 0):  # before 15:00
        return "8-3"
    elif t_obj < time(17, 0):  # 15:00 – 16:59
        return "3-5"
    else:
        return "5-7"

panel_counts = defaultdict(lambda: defaultdict(set))  # day -> panel -> set(groups)

for lesson in data.get("lessons", []):
    day = lesson["day"].title()  # MONDAY -> Monday
    panel = classify_panel(lesson["startTime"])
    group = lesson["group"]
    panel_counts[day][panel].add(group)

# Prepare ordered days (Mon-Sat)
ORDER = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]
PANELS = ["8-3", "3-5", "5-7"]

# --- Unique group count report ---
print("Unique groups per panel (CSV)")
print("Day,panel_8_3,panel_3_5,panel_5_7")
for day in ORDER:
    counts = [len(panel_counts.get(day, {}).get(p, set())) for p in PANELS]
    print(f"{day},{counts[0]},{counts[1]},{counts[2]}")

# ---------------------------------------------------------------------------
# 2) Student head-count estimation
#    • lab  -> 35  students (batch size)
#    • lecture/tutorial -> 70 students (full group)
#    We simply sum across lessons in each panel.
# ---------------------------------------------------------------------------

def lesson_headcount(lesson: dict) -> int:
    return 35 if lesson["type"].lower() == "lab" else 70

# For each (day,panel) keep a dict of unique identifiers -> size to avoid double counting
unique_map = {day: {p: {} for p in PANELS} for day in ORDER}

for lesson in data.get("lessons", []):
    day = lesson["day"].title()
    panel = classify_panel(lesson["startTime"])

    # Identifier for uniqueness: group or group+batch
    group = lesson["group"]
    batch = lesson.get("batch", "").strip()
    uid = f"{group}|{batch}" if batch else group

    if uid not in unique_map[day][panel]:
        unique_map[day][panel][uid] = lesson_headcount(lesson)

# Now aggregate counts
student_counts = {day: {p: sum(unique_map[day][p].values()) for p in PANELS} for day in ORDER}

print("\nEstimated student head-count per panel (CSV)")
print("Day,panel_8_3,panel_3_5,panel_5_7")
for day in ORDER:
    c = student_counts[day]
    print(f"{day},{c['8-3']},{c['3-5']},{c['5-7']}")

# ---------------------------------------------------------------------------
# 3) Simplified estimate requested by user: unique groups in panel × 70
# ---------------------------------------------------------------------------

# simplified count uses number of distinct student groups (ignoring batches) per panel
simplified_counts = {
    day: {p: len(panel_counts.get(day, {}).get(p, set())) * 70 for p in PANELS}
    for day in ORDER
}

print("\nSimplified student head-count per panel (CSV)")
print("Day,panel_8_3,panel_3_5,panel_5_7")
for day in ORDER:
    c = simplified_counts[day]
    print(f"{day},{c['8-3']},{c['3-5']},{c['5-7']}") 