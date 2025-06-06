import pandas as pd
from timetable.solver import solve_timetable
from timetable.visualization import (
    print_timetable, export_to_csv, export_to_excel,
    print_teacher_workload, generate_studentgroup_image_timetable
)

def main():
    teachers_courses_csv = "cse-1.csv"
    rooms_csv = "techlongue.csv"
    
    print("Starting timetable optimization...")
    solution = solve_timetable(teachers_courses_csv, rooms_csv)
    
    if solution:
        print("\nOptimization complete! Generating outputs...")
        print_timetable(solution)
        export_to_csv(solution)
        export_to_excel(solution)
        print_teacher_workload(solution)
        generate_studentgroup_image_timetable(solution)
    else:
        print("Optimization failed.")

if __name__ == "__main__":
    main()