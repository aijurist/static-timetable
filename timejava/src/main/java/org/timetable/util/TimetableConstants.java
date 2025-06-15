package org.timetable.util;

import java.util.Arrays;
import java.util.List;

/**
 * Constants used throughout the timetable system.
 */
public class TimetableConstants {
    
    // Days of the week for scheduling (including Saturday)
    public static final List<String> DAYS = Arrays.asList(
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    );
    
    // Theory class time slots (50-minute classes)
    public static final List<String> THEORY_TIME_SLOTS = Arrays.asList(
        "8:00 - 8:50",
        "9:00 - 9:50", 
        "10:00 - 10:50",
        "11:00 - 11:50",
        "12:00 - 12:50",
        "13:00 - 13:50",
        "14:00 - 14:50",
        "15:00 - 15:50",
        "16:00 - 16:50",
        "17:00 - 17:50",
        "18:00 - 18:50"
    );
    
    // Lab time slots (100-minute sessions)
    public static final List<String> LAB_TIME_SLOTS = Arrays.asList(
        "8:00 - 9:40",
        "9:50 - 11:30",
        "11:50 - 13:30",
        "13:50 - 15:30",
        "15:50 - 17:30",
        "17:50 - 19:30"
    );
    
    // Combined time slots for general use (backward compatibility)
    public static final List<String> TIME_SLOTS = THEORY_TIME_SLOTS;
    
    // Room types
    public static final String CLASSROOM = "Classroom";
    public static final String LAB = "Lab";
    public static final String TUTORIAL_ROOM = "Tutorial Room";
    
    // Class types
    public static final String THEORY = "theory";
    public static final String LAB_CLASS = "lab";
    public static final String TUTORIAL = "tutorial";
    
    // Scheduling constraints
    public static final int MAX_TEACHER_HOURS = 21; // Maximum hours per week for a teacher
    public static final int CLASS_STRENGTH = 70;    // Maximum students per class
    public static final int LAB_BATCH_SIZE = 35;    // Maximum students per lab batch
    
    // Class durations (in minutes)
    public static final int THEORY_DURATION_MINUTES = 50;
    public static final int LAB_DURATION_MINUTES = 100;
    public static final int TUTORIAL_DURATION_MINUTES = 50;
    
    // Legacy durations (in hours) for backward compatibility
    public static final int THEORY_DURATION = 1;
    public static final int LAB_DURATION = 2;
    public static final int TUTORIAL_DURATION = 1;
    
    // Maximum classes per day for students
    public static final int MAX_CLASSES_PER_DAY = 6;
    
    // Maximum classes per day for teachers
    public static final int MAX_TEACHER_CLASSES_PER_DAY = 8;
    
    // Break times (slots to avoid scheduling during)
    public static final List<String> BREAK_SLOTS = Arrays.asList(
        "12:00 - 12:50" // Lunch break
    );
    
    /**
     * Get appropriate time slots based on course type
     */
    public static List<String> getTimeSlotsForCourseType(String courseType) {
        if (courseType == null) {
            return THEORY_TIME_SLOTS;
        }
        
        switch (courseType.toLowerCase()) {
            case LAB_CLASS:
                return LAB_TIME_SLOTS;
            case THEORY:
            case TUTORIAL:
            default:
                return THEORY_TIME_SLOTS;
        }
    }
    
    /**
     * Check if a time slot conflicts with another
     */
    public static boolean hasTimeConflict(String slot1, String slot2) {
        if (slot1 == null || slot2 == null) {
            return false;
        }
        
        // Extract start and end times
        String[] slot1Times = slot1.split(" - ");
        String[] slot2Times = slot2.split(" - ");
        
        if (slot1Times.length != 2 || slot2Times.length != 2) {
            return false;
        }
        
        try {
            int slot1Start = parseTime(slot1Times[0]);
            int slot1End = parseTime(slot1Times[1]);
            int slot2Start = parseTime(slot2Times[0]);
            int slot2End = parseTime(slot2Times[1]);
            
            // Check for overlap: slot1 starts before slot2 ends AND slot2 starts before slot1 ends
            return (slot1Start < slot2End) && (slot2Start < slot1End);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Parse time string to minutes since midnight
     */
    private static int parseTime(String timeStr) {
        String[] parts = timeStr.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        return hours * 60 + minutes;
    }
    
    private TimetableConstants() {
        // Utility class - prevent instantiation
    }
}