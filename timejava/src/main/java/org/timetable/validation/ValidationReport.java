package org.timetable.validation;

import org.timetable.domain.*;

import java.util.*;

/**
 * Comprehensive validation report containing all constraint violations and analytics
 */
public class ValidationReport {
    
    private int totalLessons;
    private int assignedLessons;
    private int unassignedLessons;
    
    private List<ConstraintViolation> violations = new ArrayList<>();
    private Map<String, Integer> violationSummary = new HashMap<>();
    
    private Map<Room, Integer> roomUtilization = new HashMap<>();
    private Map<TimeSlot, Integer> timeSlotUtilization = new HashMap<>();
    private Map<Teacher, TeacherAnalytics> teacherAnalytics = new HashMap<>();
    private Map<StudentGroup, StudentGroupAnalytics> studentGroupAnalytics = new HashMap<>();
    
    // Getters and Setters
    public int getTotalLessons() { return totalLessons; }
    public void setTotalLessons(int totalLessons) { this.totalLessons = totalLessons; }
    
    public int getAssignedLessons() { return assignedLessons; }
    public void setAssignedLessons(int assignedLessons) { this.assignedLessons = assignedLessons; }
    
    public int getUnassignedLessons() { return unassignedLessons; }
    public void setUnassignedLessons(int unassignedLessons) { this.unassignedLessons = unassignedLessons; }
    
    public List<ConstraintViolation> getViolations() { return violations; }
    public void addViolation(ConstraintViolation violation) { 
        violations.add(violation);
        violationSummary.merge(violation.getType(), 1, Integer::sum);
    }
    
    public Map<String, Integer> getViolationSummary() { return violationSummary; }
    
    public Map<Room, Integer> getRoomUtilization() { return roomUtilization; }
    public void setRoomUtilization(Map<Room, Integer> roomUtilization) { 
        this.roomUtilization = roomUtilization; 
    }
    
    public Map<TimeSlot, Integer> getTimeSlotUtilization() { return timeSlotUtilization; }
    public void setTimeSlotUtilization(Map<TimeSlot, Integer> timeSlotUtilization) { 
        this.timeSlotUtilization = timeSlotUtilization; 
    }
    
    public Map<Teacher, TeacherAnalytics> getTeacherAnalytics() { return teacherAnalytics; }
    public void setTeacherAnalytics(Map<Teacher, TeacherAnalytics> teacherAnalytics) { 
        this.teacherAnalytics = teacherAnalytics; 
    }
    
    public Map<StudentGroup, StudentGroupAnalytics> getStudentGroupAnalytics() { 
        return studentGroupAnalytics; 
    }
    public void setStudentGroupAnalytics(Map<StudentGroup, StudentGroupAnalytics> studentGroupAnalytics) { 
        this.studentGroupAnalytics = studentGroupAnalytics; 
    }
    
    // Summary methods
    public int getHardViolationCount() {
        return (int) violations.stream().filter(v -> "HARD".equals(v.getSeverity())).count();
    }
    
    public int getSoftViolationCount() {
        return (int) violations.stream().filter(v -> "SOFT".equals(v.getSeverity())).count();
    }
    
    public double getAssignmentPercentage() {
        return totalLessons > 0 ? (double) assignedLessons / totalLessons * 100.0 : 0.0;
    }
    
    public boolean isFeasible() {
        return getHardViolationCount() == 0;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== TIMETABLE VALIDATION REPORT ===\n");
        sb.append(String.format("Total Lessons: %d\n", totalLessons));
        sb.append(String.format("Assigned Lessons: %d (%.1f%%)\n", assignedLessons, getAssignmentPercentage()));
        sb.append(String.format("Unassigned Lessons: %d\n", unassignedLessons));
        sb.append(String.format("Hard Violations: %d\n", getHardViolationCount()));
        sb.append(String.format("Soft Violations: %d\n", getSoftViolationCount()));
        sb.append(String.format("Feasible: %s\n", isFeasible() ? "YES" : "NO"));
        
        if (!violations.isEmpty()) {
            sb.append("\n=== VIOLATION SUMMARY ===\n");
            violationSummary.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> sb.append(String.format("%s: %d\n", entry.getKey(), entry.getValue())));
        }
        
        return sb.toString();
    }
} 