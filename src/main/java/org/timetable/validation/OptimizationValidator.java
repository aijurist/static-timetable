package org.timetable.validation;

import org.timetable.domain.*;
import org.timetable.config.TimetableConfig;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Validation utility for timetable quality and constraint compliance
 */
public class OptimizationValidator {

    /**
     * Comprehensive validation of the timetable solution
     */
    public static ValidationReport validateSolution(TimetableProblem solution) {
        ValidationReport report = new ValidationReport();
        
        // Validate constraint compliance
        validateConstraints(solution, report);
        
        // Validate optimization effectiveness
        validateOptimization(solution, report);
        
        return report;
    }
    
    /**
     * Validate general constraint compliance
     */
    private static void validateConstraints(TimetableProblem solution, ValidationReport report) {
        // Check hard constraints
        report.roomConflicts = countRoomConflicts(solution);
        report.teacherConflicts = countTeacherConflicts(solution);
        report.studentConflicts = countStudentConflicts(solution);
        report.capacityViolations = countCapacityViolations(solution);
        
        // Check assignment completeness
        long unassignedLessons = solution.getLessons().stream()
                .filter(lesson -> lesson.getTimeSlot() == null || lesson.getRoom() == null)
                .count();
        
        report.unassignedLessons = (int) unassignedLessons;
        report.totalLessons = solution.getLessons().size();
        report.assignmentRate = ((double) (report.totalLessons - report.unassignedLessons)) / report.totalLessons * 100.0;
    }
    
    /**
     * Validate optimization effectiveness
     */
    private static void validateOptimization(TimetableProblem solution, ValidationReport report) {
        // Teacher workload distribution
        Map<Teacher, Integer> teacherHours = solution.getLessons().stream()
                .filter(lesson -> lesson.getTimeSlot() != null)
                .collect(Collectors.groupingBy(
                    Lesson::getTeacher,
                    Collectors.summingInt(Lesson::getEffectiveHours)
                ));
        
        if (!teacherHours.isEmpty()) {
            report.avgTeacherHours = teacherHours.values().stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0.0);
            
            report.maxTeacherHours = teacherHours.values().stream()
                    .mapToInt(Integer::intValue)
                    .max()
                    .orElse(0);
        }
        
        // Time slot utilization
        Map<TimeSlot, Long> slotUtilization = solution.getLessons().stream()
                .filter(lesson -> lesson.getTimeSlot() != null)
                .collect(Collectors.groupingBy(
                    Lesson::getTimeSlot,
                    Collectors.counting()
                ));
        
        if (!slotUtilization.isEmpty()) {
            report.avgSlotUtilization = slotUtilization.values().stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0);
        }
        
        // Room utilization efficiency
        Map<Room, Long> roomUtilization = solution.getLessons().stream()
                .filter(lesson -> lesson.getRoom() != null && lesson.getTimeSlot() != null)
                .collect(Collectors.groupingBy(
                    Lesson::getRoom,
                    Collectors.counting()
                ));
        
        if (!roomUtilization.isEmpty()) {
            report.avgRoomUtilization = roomUtilization.values().stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0);
        }
    }
    
    // Helper methods for constraint checking
    private static int countRoomConflicts(TimetableProblem solution) {
        Map<String, List<Lesson>> roomTimeSlotMap = solution.getLessons().stream()
                .filter(lesson -> lesson.getRoom() != null && lesson.getTimeSlot() != null)
                .collect(Collectors.groupingBy(lesson -> 
                    lesson.getRoom().getId() + "_" + lesson.getTimeSlot().getId()));
        
        return (int) roomTimeSlotMap.values().stream()
                .filter(lessons -> lessons.size() > 1)
                .count();
    }
    
    private static int countTeacherConflicts(TimetableProblem solution) {
        Map<String, List<Lesson>> teacherTimeMap = solution.getLessons().stream()
                .filter(lesson -> lesson.getTeacher() != null && lesson.getTimeSlot() != null)
                .collect(Collectors.groupingBy(lesson -> 
                    lesson.getTeacher().getId() + "_" + lesson.getTimeSlot().getId()));
        
        return (int) teacherTimeMap.values().stream()
                .filter(lessons -> lessons.size() > 1)
                .count();
    }
    
    private static int countStudentConflicts(TimetableProblem solution) {
        Map<String, List<Lesson>> studentTimeMap = solution.getLessons().stream()
                .filter(lesson -> lesson.getStudentGroup() != null && lesson.getTimeSlot() != null)
                .collect(Collectors.groupingBy(lesson -> 
                    lesson.getStudentGroup().getId() + "_" + lesson.getTimeSlot().getId()));
        
        return (int) studentTimeMap.values().stream()
                .filter(lessons -> lessons.size() > 1)
                .count();
    }
    
    private static int countCapacityViolations(TimetableProblem solution) {
        return (int) solution.getLessons().stream()
                .filter(lesson -> lesson.getRoom() != null && 
                        lesson.getRoom().getCapacity() < lesson.getRequiredCapacity())
                .count();
    }
    
    /**
     * Validation report containing all metrics
     */
    public static class ValidationReport {
        // General constraint metrics
        public int roomConflicts = 0;
        public int teacherConflicts = 0;
        public int studentConflicts = 0;
        public int capacityViolations = 0;
        public int unassignedLessons = 0;
        public int totalLessons = 0;
        public double assignmentRate = 0.0;
        
        // Optimization metrics
        public double avgTeacherHours = 0.0;
        public int maxTeacherHours = 0;
        public double avgSlotUtilization = 0.0;
        public double avgRoomUtilization = 0.0;
        
        public void printReport() {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("📊 TIMETABLE VALIDATION REPORT");
            System.out.println("=".repeat(60));
            
            System.out.println("\n📋 ASSIGNMENT STATISTICS:");
            System.out.println("   Total lessons: " + totalLessons);
            System.out.println("   Assigned lessons: " + (totalLessons - unassignedLessons));
            System.out.println("   Unassigned lessons: " + unassignedLessons);
            System.out.println("   Assignment rate: " + String.format("%.1f%%", assignmentRate));
            
            System.out.println("\n🚨 CONSTRAINT VIOLATIONS:");
            System.out.println("   Room conflicts: " + roomConflicts);
            System.out.println("   Teacher conflicts: " + teacherConflicts);
            System.out.println("   Student conflicts: " + studentConflicts);
            System.out.println("   Capacity violations: " + capacityViolations);
            
            System.out.println("\n📈 OPTIMIZATION METRICS:");
            System.out.println("   Average teacher hours: " + String.format("%.1f", avgTeacherHours));
            System.out.println("   Maximum teacher hours: " + maxTeacherHours);
            System.out.println("   Average slot utilization: " + String.format("%.1f", avgSlotUtilization));
            System.out.println("   Average room utilization: " + String.format("%.1f", avgRoomUtilization));
            
            System.out.println("\n" + "=".repeat(60));
            System.out.println("🏆 OVERALL ASSESSMENT:");
            
            int totalViolations = roomConflicts + teacherConflicts + studentConflicts + capacityViolations;
            if (totalViolations == 0 && unassignedLessons == 0) {
                System.out.println("   ✅ PERFECT SOLUTION - All constraints satisfied!");
            } else if (totalViolations == 0) {
                System.out.println("   ✅ FEASIBLE SOLUTION - All hard constraints met");
                if (unassignedLessons > 0) {
                    System.out.println("   ⚠️ Some lessons remain unassigned");
                }
            } else {
                System.out.println("   ❌ INFEASIBLE SOLUTION - Constraint violations detected");
            }
            
            if (assignmentRate >= 95.0 && totalViolations <= 2) {
                System.out.println("   🎯 HIGH QUALITY - Good scheduling achieved");
            } else if (assignmentRate >= 80.0) {
                System.out.println("   ⚡ MODERATE QUALITY - Room for improvement");
            } else {
                System.out.println("   🔧 NEEDS IMPROVEMENT - Consider adjusting constraints");
            }
            
            System.out.println("=".repeat(60));
        }
    }
} 