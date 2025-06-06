from optapy import solver_manager_create
from optapy.types import SolverConfig
import jpype
from .data_models import TimeTable
import pandas as pd
from timetable import LectureAssignment
from .config import TIME_SLOTS

def solve_timetable(teachers_courses_csv, rooms_csv):
    try:
        from .data_loader import load_data, create_time_slots, create_lecture_assignments
        from .constraints import timetable_constraints
        
        teachers, rooms, courses, student_groups = load_data(teachers_courses_csv, rooms_csv)
        time_slots = create_time_slots()
        
        # Add slot_index to time slots for lab sequencing
        for i, slot in enumerate(time_slots):
            slot.slot_index = i % len(TIME_SLOTS)  # Index within a day
            
        df = pd.read_csv(teachers_courses_csv)
        lecture_assignments = create_lecture_assignments(df, teachers, courses, student_groups)
        
        problem = TimeTable(lecture_assignments, time_slots, rooms, teachers, courses, student_groups)
        
        Duration = jpype.JClass("java.time.Duration")
        config = SolverConfig() \
            .withSolutionClass(TimeTable) \
            .withEntityClasses([LectureAssignment]) \
            .withConstraintProviderClass(timetable_constraints) \
            .withTerminationSpentLimit(Duration.ofMinutes(25))  # Increased time for better solution
            
        solver_manager = solver_manager_create(config)
        solver_job = solver_manager.solve("timetable", problem)
        return solver_job.getFinalBestSolution()
        
    except Exception as e:
        print(f"Error during optimization: {e}")
        import traceback
        traceback.print_exc()
        return None