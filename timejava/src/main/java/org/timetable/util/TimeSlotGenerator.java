package org.timetable.util;

import org.timetable.domain.TimeSlot;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to generate time slots from the defined constants.
 */
public class TimeSlotGenerator {
    
    /**
     * Generate all time slots (theory and lab) for all days.
     * 
     * @return List of all time slots
     */
    public static List<TimeSlot> generateAllTimeSlots() {
        List<TimeSlot> allSlots = new ArrayList<>();
        allSlots.addAll(generateTheoryTimeSlots());
        allSlots.addAll(generateLabTimeSlots());
        return allSlots;
    }
    
    /**
     * Generate theory time slots for all days.
     * 
     * @return List of theory time slots
     */
    public static List<TimeSlot> generateTheoryTimeSlots() {
        List<TimeSlot> theorySlots = new ArrayList<>();
        
        // For each day of the week
        for (int day = 0; day < TimetableConstants.DAYS.size(); day++) {
            // For each theory time slot
            for (String[] timeSlot : TimetableConstants.THEORY_TIME_SLOTS) {
                theorySlots.add(new TimeSlot(day, timeSlot[0], timeSlot[1], "theory"));
            }
        }
        
        return theorySlots;
    }
    
    /**
     * Generate lab time slots for all days.
     * 
     * @return List of lab time slots
     */
    public static List<TimeSlot> generateLabTimeSlots() {
        List<TimeSlot> labSlots = new ArrayList<>();
        
        // For each day of the week
        for (int day = 0; day < TimetableConstants.DAYS.size(); day++) {
            // For each lab time slot
            for (String[] timeSlot : TimetableConstants.LAB_TIME_SLOTS) {
                labSlots.add(new TimeSlot(day, timeSlot[0], timeSlot[1], "lab"));
            }
        }
        
        return labSlots;
    }
    
    /**
     * Find lunch break slots for a specific day.
     * 
     * @param day Day index (0-4)
     * @return List of lunch break time slots for the day
     */
    public static List<TimeSlot> getLunchBreakSlots(int day) {
        List<TimeSlot> lunchSlots = new ArrayList<>();
        
        for (String[] timeSlot : TimetableConstants.LUNCH_BREAK_SLOTS) {
            TimeSlot slot = new TimeSlot(day, timeSlot[0], timeSlot[1], "lunch");
            lunchSlots.add(slot);
        }
        
        return lunchSlots;
    }
} 