import pandas as pd
from timetable.solver import solve_timetable
from timetable.visualization import (
    print_timetable, export_to_csv, export_to_excel,
    print_teacher_workload, generate_all_visualizations
)
import os

def main():
    # Input files - adjust these paths as needed
    courses_csv = "data/courses/cse_dept_red.csv"
    
    print("=" * 60)
    print("🎓 TIMETABLE OPTIMIZATION SYSTEM")
    print("=" * 60)
    print(f"📋 Courses file: {courses_csv}")
    print("\n🔄 Starting timetable optimization...")
    
    # Create output directory if it doesn't exist
    output_dir = "timetable_outputs"
    os.makedirs(output_dir, exist_ok=True)
    
    solution = solve_timetable(courses_csv)
    
    if solution:
        print("\n✅ Optimization complete!")
        score = solution.get_score()
        print(f"📊 Final Score: Hard={score.hardScore}, Soft={score.softScore}")
        
        # Count scheduled vs unscheduled
        scheduled = len([a for a in solution.lecture_assignments if a.timeslot and a.room])
        unscheduled = len([a for a in solution.lecture_assignments if not a.timeslot])
        total = scheduled + unscheduled
        
        print(f"📈 Sessions scheduled: {scheduled}/{total}")
        if unscheduled > 0:
            print(f"⚠️  Unscheduled sessions: {unscheduled}")
        
        print("\n" + "=" * 60)
        print("📋 GENERATING COMPREHENSIVE OUTPUT")
        print("=" * 60)
        
        # Generate all outputs
        print("\n🚀 Generating all visualizations and exports...")
        generate_all_visualizations(solution, output_dir)
        
        # Additional console outputs
        print("\n📝 Printing timetable summary to console...")
        print_timetable(solution)
        
        print("\n👨‍🏫 Analyzing teacher workload...")
        print_teacher_workload(solution)
        
        print("\n" + "=" * 60)
        print("🎉 ALL OUTPUTS GENERATED SUCCESSFULLY!")
        print("=" * 60)
        print("\n📁 Check these directories and files:")
        print(f"   📂 {output_dir}/student_timetables/ - Student group timetables")
        print(f"   📂 {output_dir}/teacher_timetables/ - Teacher timetables")
        print(f"   📂 {output_dir}/dashboard/ - Summary dashboard")
        print(f"   📄 {output_dir}/timetable_complete.csv - CSV export")
        print(f"   📄 {output_dir}/timetable_complete.xlsx - Excel export")
        
        if unscheduled == 0:
            print("\n🎯 Perfect scheduling achieved! All sessions scheduled successfully.")
        else:
            print(f"\n⚠️  Note: {unscheduled} sessions could not be scheduled.")
            print("   Consider adjusting constraints or adding more time slots/rooms.")
            
    else:
        print("\n❌ Optimization failed.")
        print("💡 Suggestions:")
        print("   - Check if input files exist and are properly formatted")
        print("   - Ensure there are enough time slots and rooms")
        print("   - Verify teacher and course data consistency")

if __name__ == "__main__":
    # Add JPype native access flag
    import jpype

    if not jpype.isJVMStarted():
        jpype.startJVM("--enable-native-access=ALL-UNNAMED")
    
    main()
    
    if jpype.isJVMStarted():
        jpype.shutdownJVM()