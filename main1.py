import pandas as pd
from timetable.solver import solve_timetable
from timetable.visualization import (
    print_timetable, export_to_csv, export_to_excel,
    print_teacher_workload, generate_studentgroup_image_timetable,
    generate_enhanced_timetable_image, generate_teacher_timetable_images,
    generate_summary_dashboard, generate_all_visualizations
)

def main():
    teachers_courses_csv = "cse-1.csv"
    rooms_csv = "techlongue.csv"
    
    print("=" * 60)
    print("🎓 TIMETABLE OPTIMIZATION SYSTEM")
    print("=" * 60)
    print(f"📋 Teachers/Courses file: {teachers_courses_csv}")
    print(f"🏢 Rooms file: {rooms_csv}")
    print("\n🔄 Starting timetable optimization...")
    
    solution = solve_timetable(teachers_courses_csv, rooms_csv)
    
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
        
        # Option 1: Generate everything at once (recommended)
        print("\n🚀 Generating all visualizations and exports...")
        generate_all_visualizations(solution, "timetable_outputs")
        
        # Option 2: Generate individual components (if you want more control)
        # Uncomment the sections below if you prefer step-by-step generation
        
        """
        # Console output
        print("\n1️⃣ Displaying timetable in console...")
        print_timetable(solution)
        
        # Teacher workload analysis
        print("\n2️⃣ Analyzing teacher workload...")
        print_teacher_workload(solution)
        
        # Enhanced student group timetables
        print("\n3️⃣ Generating enhanced student group timetables...")
        generate_enhanced_timetable_image(solution, "enhanced_student_timetables")
        
        # Teacher timetables
        print("\n4️⃣ Generating teacher timetables...")
        generate_teacher_timetable_images(solution, "teacher_timetables")
        
        # Summary dashboard
        print("\n5️⃣ Generating summary dashboard...")
        generate_summary_dashboard(solution, "dashboard")
        
        # Export to CSV
        print("\n6️⃣ Exporting to CSV...")
        export_to_csv(solution, "timetable_result.csv")
        
        # Export to Excel
        print("\n7️⃣ Exporting to Excel...")
        export_to_excel(solution, "timetable_complete.xlsx")
        """
        
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

def main_simple():
    """Simplified version that generates basic outputs only."""
    teachers_courses_csv = "cse-1.csv"
    rooms_csv = "techlongue.csv"
    
    print("Starting timetable optimization...")
    solution = solve_timetable(teachers_courses_csv, rooms_csv)
    
    if solution:
        print("\nOptimization complete! Generating basic outputs...")
        print_timetable(solution)
        export_to_csv(solution)
        generate_enhanced_timetable_image(solution)
        print("✅ Basic outputs generated!")
    else:
        print("❌ Optimization failed.")

def main_custom():
    """Custom version where you can choose what to generate."""
    teachers_courses_csv = "cse-1.csv"
    rooms_csv = "techlongue.csv"
    
    print("Starting timetable optimization...")
    solution = solve_timetable(teachers_courses_csv, rooms_csv)
    
    if solution:
        print("\nOptimization complete!")
        
        # Customize what you want to generate
        generate_console_output = True
        generate_images = True
        generate_teacher_schedules = True
        generate_dashboard = True
        generate_exports = True
        
        if generate_console_output:
            print("\n📋 Console Output:")
            print_timetable(solution)
            print_teacher_workload(solution)
        
        if generate_images:
            print("\n🖼️ Generating student timetable images...")
            generate_enhanced_timetable_image(solution, "student_timetables")
        
        if generate_teacher_schedules:
            print("\n👨‍🏫 Generating teacher timetables...")
            generate_teacher_timetable_images(solution, "teacher_timetables")
        
        if generate_dashboard:
            print("\n📊 Generating summary dashboard...")
            generate_summary_dashboard(solution, "dashboard")
        
        if generate_exports:
            print("\n💾 Exporting data...")
            export_to_csv(solution, "timetable.csv")
            export_to_excel(solution, "timetable.xlsx")
        
        print("\n✅ Custom outputs generated!")
    else:
        print("❌ Optimization failed.")

if __name__ == "__main__":
    # Choose which main function to run:
    
    # Full featured version (recommended)
    main()
    
    # Simple version
    # main_simple()
    
    # Custom version
    # main_custom()