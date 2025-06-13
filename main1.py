# main.py
from timetable.solver import solve_timetable
from timetable.visualization import (
    print_timetable, export_to_csv, export_to_excel,
    print_teacher_workload, generate_enhanced_timetable_image,
    generate_teacher_timetable_images, generate_summary_dashboard,
    generate_all_visualizations
)

def main():
    teachers_courses_csv = "data/courses/cse_dept_red.csv"
    rooms_csv = "data/backup.csv"
    
    print("=" * 60)
    print("🎓 TIMETABLE OPTIMIZATION SYSTEM")
    print("=" * 60)
    print(f"📋 Teachers/Courses file: {teachers_courses_csv}")
    print(f"🏢 Rooms file: {rooms_csv}")
    print("\n🔄 Starting parallel timetable optimization...")
    
    solution = solve_timetable(teachers_courses_csv, rooms_csv)
    
    if solution:
        print("\n✅ Optimization complete!")
        score = solution.get_score()
        # print(f"📊 Final Score: Hard={score.hardScore}, Soft={score.softScore}")
        
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
        
        print("\n🚀 Generating all visualizations and exports...")
        generate_all_visualizations(solution, "timetable_outputs")
        
        print("\n" + "=" * 60)
        print("🎉 ALL OUTPUTS GENERATED SUCCESSFULLY!")
        print("=" * 60)
        print("\n📁 Check these directories and files:")
        print("   📂 timetable_outputs/student_timetables/ - Student group timetables")
        print("   📂 timetable_outputs/teacher_timetables/ - Teacher timetables")
        print("   📂 timetable_outputs/dashboard/ - Summary dashboard")
        print("   📄 timetable_outputs/timetable_complete.csv - CSV export")
        print("   📄 timetable_outputs/timetable_complete.xlsx - Excel export")
        
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
    main()