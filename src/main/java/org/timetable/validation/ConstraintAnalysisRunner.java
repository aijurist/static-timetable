package org.timetable.validation;

import org.timetable.domain.TimetableProblem;
import org.timetable.persistence.TimetableDataLoader;

/**
 * Utility class to run constraint violation analysis on a solved timetable.
 * This demonstrates how to use the ConstraintViolationAnalyzer after solving.
 */
public class ConstraintAnalysisRunner {
    
    /**
     * Example usage: Analyze a timetable for constraint violations
     */
    public static void main(String[] args) {
        try {
            // Load your timetable problem (replace with your actual data files)
            String coursesFile = "data/courses/core_dept_red.csv";
            String roomsDir = "data";
            
            TimetableProblem problem = TimetableDataLoader.loadProblem(coursesFile, roomsDir);
            
            // After your solver has run and assigned time slots and rooms to lessons,
            // you can analyze the solution for violations
            analyzeAndReport(problem);
            
        } catch (Exception e) {
            System.err.println("Error analyzing timetable: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Analyze a timetable problem and print detailed violation reports
     */
    public static void analyzeAndReport(TimetableProblem problem) {
        System.out.println("🔍 Starting constraint violation analysis...");
        
        // Run the analysis
        ConstraintViolationAnalyzer.ViolationReport report = ConstraintViolationAnalyzer.analyze(problem);
        
        // Print summary report
        report.printSummary();
        
        // If there are violations, print detailed report
        if (report.getTotalViolations() > 0) {
            System.out.println("\n" + "⚠".repeat(5) + " VIOLATIONS DETECTED " + "⚠".repeat(5));
            report.printDetailedReport();
            
            // Show some statistics
            printViolationStatistics(report);
        } else {
            System.out.println("\n✅ PERFECT! No constraint violations found.");
            System.out.println("The timetable is completely feasible.");
        }
    }
    
    /**
     * Print additional statistics about violations
     */
    private static void printViolationStatistics(ConstraintViolationAnalyzer.ViolationReport report) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("                        VIOLATION STATISTICS");
        System.out.println("=".repeat(80));
        
        // Count by severity
        long criticalCount = report.getViolations().stream()
                .filter(v -> "CRITICAL".equals(v.getSeverity()))
                .count();
        long hardCount = report.getViolations().stream()
                .filter(v -> "HARD".equals(v.getSeverity()))
                .count();
        
        System.out.println(String.format("📊 Violation Breakdown:"));
        System.out.println(String.format("   🔴 Critical: %d violations", criticalCount));
        System.out.println(String.format("   🟠 Hard:     %d violations", hardCount));
        System.out.println(String.format("   📈 Total:    %d violations", report.getTotalViolations()));
        
        // Show most problematic constraint
        if (!report.getViolationCountsByConstraint().isEmpty()) {
            var mostProblematic = report.getViolationCountsByConstraint().entrySet().stream()
                    .max(java.util.Map.Entry.comparingByValue())
                    .orElse(null);
            
            if (mostProblematic != null) {
                System.out.println(String.format("\n🎯 Most Problematic Constraint: %s (%d violations)", 
                        mostProblematic.getKey(), mostProblematic.getValue()));
            }
        }
        
        // Show lessons with violations
        long lessonsWithViolations = report.getViolations().stream()
                .filter(v -> v.getLesson() != null)
                .map(v -> v.getLesson().getId())
                .distinct()
                .count();
        
        System.out.println(String.format("\n📋 Affected Lessons: %d lessons have constraint violations", lessonsWithViolations));
        
        // Feasibility assessment
        if (report.hasCriticalViolations()) {
            System.out.println("\n🚨 FEASIBILITY STATUS: INFEASIBLE");
            System.out.println("   Critical violations make this timetable impossible to implement.");
        } else if (report.hasHardViolations()) {
            System.out.println("\n⚠️  FEASIBILITY STATUS: PROBLEMATIC");
            System.out.println("   Hard constraint violations need to be resolved.");
        } else {
            System.out.println("\n✅ FEASIBILITY STATUS: FEASIBLE");
            System.out.println("   All hard constraints are satisfied.");
        }
    }
    
    /**
     * Quick analysis that returns just the summary information
     */
    public static String getQuickSummary(TimetableProblem problem) {
        ConstraintViolationAnalyzer.ViolationReport report = ConstraintViolationAnalyzer.analyze(problem);
        
        if (report.getTotalViolations() == 0) {
            return "✅ FEASIBLE: No constraint violations";
        } else {
            long criticalCount = report.getViolations().stream()
                    .filter(v -> "CRITICAL".equals(v.getSeverity()))
                    .count();
            long hardCount = report.getViolations().stream()
                    .filter(v -> "HARD".equals(v.getSeverity()))
                    .count();
            
            return String.format("⚠️ VIOLATIONS: %d total (%d critical, %d hard)", 
                    report.getTotalViolations(), criticalCount, hardCount);
        }
    }
    
    /**
     * Filter violations by specific constraint name
     */
    public static void analyzeSpecificConstraint(TimetableProblem problem, String constraintName) {
        ConstraintViolationAnalyzer.ViolationReport report = ConstraintViolationAnalyzer.analyze(problem);
        
        var specificViolations = report.getViolationsByConstraint().get(constraintName);
        
        if (specificViolations == null || specificViolations.isEmpty()) {
            System.out.println(String.format("✅ No violations found for constraint: %s", constraintName));
            return;
        }
        
        System.out.println(String.format("\n🔍 ANALYZING CONSTRAINT: %s", constraintName));
        System.out.println("=".repeat(60));
        System.out.println(String.format("Found %d violations:", specificViolations.size()));
        
        for (ConstraintViolationAnalyzer.ConstraintViolation violation : specificViolations) {
            System.out.println(String.format("\n• %s", violation.getDescription()));
            if (violation.getLesson() != null) {
                var lesson = violation.getLesson();
                System.out.println(String.format("  Lesson: %s | Course: %s | Group: %s", 
                    lesson.getId(),
                    lesson.getCourse() != null ? lesson.getCourse().getCode() : "N/A",
                    lesson.getStudentGroup() != null ? lesson.getStudentGroup().getName() : "N/A"));
                System.out.println(String.format("  Time: %s | Room: %s",
                    lesson.getTimeSlot() != null ? lesson.getTimeSlot().toString() : "UNASSIGNED",
                    lesson.getRoom() != null ? lesson.getRoom().getName() : "UNASSIGNED"));
            }
        }
    }
} 