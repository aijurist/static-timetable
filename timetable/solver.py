from optapy import solver_manager_create, solver_factory_create
from optapy.types import SolverConfig, Duration, HardSoftScore
from .data_models import TimeTable, LectureAssignment
from .data_loader import load_data, TimeSlot, create_lecture_assignments
from .constraints import timetable_constraints  # Make sure this import path is correct
import pandas as pd
import logging


def solve_timetable(courses_csv):
    try:
        logger = logging.getLogger(__name__)
        logger.info("Loading data...")
        teachers, rooms, courses, student_groups, dept_year_groups = load_data(courses_csv)
        time_slots = TimeSlot.create_time_slots()
        
        logger.info("Creating assignments...")
        df = pd.read_csv(courses_csv)
        lecture_assignments = create_lecture_assignments(df, teachers, courses, student_groups, dept_year_groups)
        
        logger.info(f"Created {len(lecture_assignments)} assignments")
        logger.info(f"Available rooms: {len(rooms)}")
        logger.info(f"Available time slots: {len(time_slots)}")
        
        problem = TimeTable(
            lecture_assignments,
            time_slots,
            rooms,
            teachers,
            courses,
            student_groups
        )
        
        solver_config = SolverConfig() \
            .withSolutionClass(TimeTable) \
            .withEntityClasses(LectureAssignment) \
            .withConstraintProviderClass(timetable_constraints) \
            .withTerminationSpentLimit(Duration.ofMinutes(30)) 
        solver = solver_factory_create(solver_config).buildSolver()
        solution = solver.solve(problem)
        
        if solution:
            logger.info(f"Solution found with score: {solution.get_score()}")
            return solution
        else:
            logger.error("No solution found")
            return None
            
    except Exception as e:
        logger.error(f"Error during optimization: {e}")
        import traceback
        logger.error(traceback.format_exc())
        return None