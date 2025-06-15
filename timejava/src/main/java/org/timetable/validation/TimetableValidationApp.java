package org.timetable.validation;

import org.timetable.domain.*;
import org.timetable.persistence.TimetableDataLoader;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Main application for running comprehensive timetable validation
 */
public class TimetableValidationApp {
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java TimetableValidationApp <solution_file> <output_dir> [courses_file] [data_dir]");
            System.exit(1);
        }
        
        String solutionFile = args[0];
        String outputDir = args[1];
        String coursesFile = args.length > 2 ? args[2] : "data/courses/cse_dept_red.csv";
        String dataDir = args.length > 3 ? args[3] : "data";
        
        try {
            System.out.println("========================================");
            System.out.println("TIMETABLE VALIDATION SYSTEM");
            System.out.println("========================================");
            System.out.println("Solution file: " + solutionFile);
            System.out.println("Output directory: " + outputDir);
            System.out.println("Courses file: " + coursesFile);
            System.out.println("Data directory: " + dataDir);
            System.out.println("========================================");
            
            System.out.println("Loading timetable problem from: " + coursesFile);
            TimetableProblem problem = TimetableDataLoader.loadProblem(coursesFile, dataDir);
            
            System.out.println("Problem loaded successfully:");
            System.out.println("  - Teachers: " + problem.getTeachers().size());
            System.out.println("  - Courses: " + problem.getCourses().size());
            System.out.println("  - Student Groups: " + problem.getStudentGroups().size());
            System.out.println("  - Rooms: " + problem.getRooms().size());
            System.out.println("  - Time Slots: " + problem.getTimeSlots().size());
            System.out.println("  - Lessons: " + problem.getLessons().size());
            
            System.out.println("\nRunning comprehensive validation...");
            TimetableValidator validator = new TimetableValidator(problem);
            ValidationReport report = validator.validate();
            
            System.out.println("\n" + report.toString());
            
            // Generate detailed reports
            generateDetailedReports(report, outputDir);
            
            // Generate HTML visualization report
            System.out.println("\nGenerating HTML visualization report...");
            HtmlReportGenerator.generateHtmlReport(outputDir);
            
            System.out.println("\n✓ Validation completed successfully!");
            System.out.println("Detailed reports generated in: " + outputDir);
            System.out.println("📊 Open the HTML report for interactive visualization!");
            
        } catch (Exception e) {
            System.err.println("❌ Error during validation: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void generateDetailedReports(ValidationReport report, String outputDir) throws IOException {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
        
        // Create output directory
        java.nio.file.Files.createDirectories(java.nio.file.Paths.get(outputDir));
        
        System.out.println("Generating detailed reports...");
        
        // Generate summary report
        generateSummaryReport(report, outputDir + "/validation_summary_" + timestamp + ".txt");
        System.out.println("  ✓ Summary report generated");
        
        // Generate detailed violation report
        generateViolationReport(report, outputDir + "/violations_" + timestamp + ".csv");
        System.out.println("  ✓ Violation report generated");
        
        // Generate resource utilization report
        generateResourceUtilizationReport(report, outputDir + "/resource_utilization_" + timestamp + ".csv");
        System.out.println("  ✓ Resource utilization report generated");
        
        // Generate teacher analytics report
        generateTeacherAnalyticsReport(report, outputDir + "/teacher_analytics_" + timestamp + ".csv");
        System.out.println("  ✓ Teacher analytics report generated");
        
        // Generate student group analytics report
        generateStudentGroupAnalyticsReport(report, outputDir + "/student_analytics_" + timestamp + ".csv");
        System.out.println("  ✓ Student group analytics report generated");
    }
    
    private static void generateSummaryReport(ValidationReport report, String filename) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("TIMETABLE VALIDATION SUMMARY REPORT\n");
            writer.write("Generated at: " + LocalDateTime.now() + "\n\n");
            
            writer.write("=== OVERVIEW ===\n");
            writer.write("Total Lessons: " + report.getTotalLessons() + "\n");
            writer.write("Assigned Lessons: " + report.getAssignedLessons() + 
                        " (" + String.format("%.1f%%", report.getAssignmentPercentage()) + ")\n");
            writer.write("Unassigned Lessons: " + report.getUnassignedLessons() + "\n");
            writer.write("Solution Feasible: " + (report.isFeasible() ? "YES" : "NO") + "\n\n");
            
            writer.write("=== CONSTRAINT VIOLATIONS ===\n");
            writer.write("Hard Violations: " + report.getHardViolationCount() + "\n");
            writer.write("Soft Violations: " + report.getSoftViolationCount() + "\n\n");
            
            if (!report.getViolationSummary().isEmpty()) {
                writer.write("=== VIOLATION BREAKDOWN ===\n");
                report.getViolationSummary().entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(entry -> {
                        try {
                            writer.write(entry.getKey() + ": " + entry.getValue() + "\n");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
            }
            
            writer.write("\n=== DETAILED VIOLATIONS ===\n");
            for (ConstraintViolation violation : report.getViolations()) {
                writer.write(violation.toString() + "\n");
            }
        }
    }
    
    private static void generateViolationReport(ValidationReport report, String filename) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("Type,Severity,Description,AffectedLessons\n");
            
            for (ConstraintViolation violation : report.getViolations()) {
                writer.write(String.format("%s,%s,\"%s\",%d\n",
                    violation.getType(),
                    violation.getSeverity(),
                    violation.getDescription().replace("\"", "\"\""),
                    violation.getAffectedLessons().size()));
            }
        }
    }
    
    private static void generateResourceUtilizationReport(ValidationReport report, String filename) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("Resource,Type,Name,Block,Capacity,IsLab,Usage\n");
            
            // Room utilization
            for (Map.Entry<Room, Integer> entry : report.getRoomUtilization().entrySet()) {
                Room room = entry.getKey();
                Integer usage = entry.getValue();
                writer.write(String.format("Room,%s,%s,%s,%d,%s,%d\n",
                    room.getId(),
                    room.getName(),
                    room.getBlock(),
                    room.getCapacity(),
                    room.isLab() ? "Yes" : "No",
                    usage));
            }
            
            // Time slot utilization
            for (Map.Entry<TimeSlot, Integer> entry : report.getTimeSlotUtilization().entrySet()) {
                TimeSlot slot = entry.getKey();
                Integer usage = entry.getValue();
                writer.write(String.format("TimeSlot,%s,%s,%s,%d,%s,%d\n",
                    slot.getId(),
                    slot.toString(),
                    slot.getDayOfWeek(),
                    slot.getDuration(),
                    slot.isLab() ? "Lab" : "Theory",
                    usage));
            }
        }
    }
    
    private static void generateTeacherAnalyticsReport(ValidationReport report, String filename) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("TeacherId,TeacherName,TotalLessons,TotalHours,DifferentCourses,MondayLessons,TuesdayLessons,WednesdayLessons,ThursdayLessons,FridayLessons\n");
            
            for (Map.Entry<Teacher, TeacherAnalytics> entry : report.getTeacherAnalytics().entrySet()) {
                Teacher teacher = entry.getKey();
                TeacherAnalytics analytics = entry.getValue();
                
                writer.write(String.format("%s,\"%s\",%d,%d,%d,%d,%d,%d,%d,%d\n",
                    teacher.getId(),
                    teacher.getName(),
                    analytics.getTotalLessons(),
                    analytics.getTotalHours(),
                    analytics.getDifferentCourses(),
                    analytics.getDailyLoad().getOrDefault(java.time.DayOfWeek.MONDAY, 0),
                    analytics.getDailyLoad().getOrDefault(java.time.DayOfWeek.TUESDAY, 0),
                    analytics.getDailyLoad().getOrDefault(java.time.DayOfWeek.WEDNESDAY, 0),
                    analytics.getDailyLoad().getOrDefault(java.time.DayOfWeek.THURSDAY, 0),
                    analytics.getDailyLoad().getOrDefault(java.time.DayOfWeek.FRIDAY, 0)));
            }
        }
    }
    
    private static void generateStudentGroupAnalyticsReport(ValidationReport report, String filename) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("GroupId,GroupName,TotalLessons,DifferentCourses,MondayLessons,TuesdayLessons,WednesdayLessons,ThursdayLessons,FridayLessons\n");
            
            for (Map.Entry<StudentGroup, StudentGroupAnalytics> entry : report.getStudentGroupAnalytics().entrySet()) {
                StudentGroup group = entry.getKey();
                StudentGroupAnalytics analytics = entry.getValue();
                
                writer.write(String.format("%s,\"%s\",%d,%d,%d,%d,%d,%d,%d\n",
                    group.getId(),
                    group.getName(),
                    analytics.getTotalLessons(),
                    analytics.getDifferentCourses(),
                    analytics.getDailyLoad().getOrDefault(java.time.DayOfWeek.MONDAY, 0),
                    analytics.getDailyLoad().getOrDefault(java.time.DayOfWeek.TUESDAY, 0),
                    analytics.getDailyLoad().getOrDefault(java.time.DayOfWeek.WEDNESDAY, 0),
                    analytics.getDailyLoad().getOrDefault(java.time.DayOfWeek.THURSDAY, 0),
                    analytics.getDailyLoad().getOrDefault(java.time.DayOfWeek.FRIDAY, 0)));
            }
        }
    }
} 