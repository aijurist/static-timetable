# file: run.py

import logging
import os
from timetable.solver import solve_timetable
from timetable.visualization import generate_all_visualizations, print_timetable, print_teacher_workload

def configure_logging():
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(levelname)s - %(message)s',
        handlers=[
            logging.StreamHandler(),
            logging.FileHandler('timetable.log', 'w', encoding='utf-8') # 'w' to overwrite log on each run
        ]
    )

def main():
    configure_logging()
    logger = logging.getLogger(__name__)
    
    try:
        courses_csv = "data/courses/cse_dept_red.csv"
        output_dir = "timetable_outputs"
        time_limit_minutes = 5 # Set a reasonable time limit

        logger.info("=" * 60)
        logger.info("TIMETABLE OPTIMIZATION SYSTEM (using Google OR-Tools)")
        logger.info("=" * 60)
        logger.info(f"Courses file: {courses_csv}")
        logger.info(f"Time limit: {time_limit_minutes} minutes")
        
        os.makedirs(output_dir, exist_ok=True)
        
        logger.info("Starting timetable optimization...")
        solution = solve_timetable(courses_csv, time_limit_minutes=time_limit_minutes)
        
        if solution:
            score = solution.get_score()
            logger.info("Optimization complete!")
            logger.info(f"Final Score: Soft Penalty={score.softScore} (Lower is better)")
            
            scheduled = len([a for a in solution.lecture_assignments if a.timeslot and a.room])
            total = len(solution.lecture_assignments)
            
            logger.info(f"Sessions scheduled: {scheduled}/{total}")
            if scheduled < total:
                logger.warning(f"UNSCHEDULED SESSIONS: {total - scheduled}")
            
            logger.info("=" * 60)
            logger.info("GENERATING OUTPUTS")
            logger.info("=" * 60)
            
            try:
                generate_all_visualizations(solution, output_dir)
                logger.info("All visualizations and exports generated successfully.")
                logger.info("Output directory: " + os.path.abspath(output_dir))
                
            except Exception as e:
                logger.error(f"Error generating outputs: {e}", exc_info=True)
            
        else:
            logger.error("Optimization failed. The problem may be infeasible (too many constraints).")
            logger.error("Check timetable.log for details. Consider increasing the time limit or relaxing some constraints.")
            
    except Exception as e:
        logger.error(f"A fatal error occurred: {e}", exc_info=True)

if __name__ == "__main__":
    main()