package org.timetable.util;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Constants used for timetable generation.
 */
public class TimetableConstants {
    // Days of the week
    public static final List<String> DAYS = Arrays.asList(
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday"
    );
    
    // Time slots for theory classes
    public static final List<String[]> THEORY_TIME_SLOTS = Arrays.asList(
        new String[]{"8:00", "8:50"}, new String[]{"9:00", "9:50"}, new String[]{"10:00", "10:50"},
        new String[]{"11:00", "11:50"}, new String[]{"12:00", "12:50"}, new String[]{"13:00", "13:50"},
        new String[]{"14:00", "14:50"}, new String[]{"15:00", "15:50"}, new String[]{"16:00", "16:50"},
        new String[]{"17:00", "17:50"}, new String[]{"18:00", "18:50"}
    );
    
    // Time slots for lab classes
    public static final List<String[]> LAB_TIME_SLOTS = Arrays.asList(
        new String[]{"8:00", "9:40"}, new String[]{"9:50", "11:30"}, new String[]{"11:50", "13:30"},
        new String[]{"13:50", "15:30"}, new String[]{"15:50", "17:30"}, new String[]{"17:50", "19:30"}
    );
    
    // Teacher constraints
    public static final int MAX_TEACHER_HOURS = 21;
    
    // Class constraints
    public static final int CLASS_STRENGTH = 70;
    public static final int LAB_BATCH_SIZE = 35;
    
    // Shifts
    public static final Map<String, LocalTime[]> SHIFTS = new HashMap<>();
    static {
        SHIFTS.put("MORNING", new LocalTime[]{LocalTime.of(8, 0), LocalTime.of(15, 0)});
        SHIFTS.put("AFTERNOON", new LocalTime[]{LocalTime.of(10, 0), LocalTime.of(17, 0)});
        SHIFTS.put("EVENING", new LocalTime[]{LocalTime.of(12, 0), LocalTime.of(19, 0)});
    }
    
    // Shift patterns
    public static final List<Map<String, Integer>> SHIFT_PATTERNS = Arrays.asList(
        Map.of("MORNING", 2, "AFTERNOON", 2, "EVENING", 1),
        Map.of("AFTERNOON", 2, "EVENING", 2, "MORNING", 1),
        Map.of("MORNING", 1, "AFTERNOON", 2, "EVENING", 2)
    );
    
    // Department data - number of sections per year
    public static final Map<String, Map<String, Integer>> DEPARTMENT_DATA = new HashMap<>();
    static {
        DEPARTMENT_DATA.put("CSE-CS", Map.of("2", 2, "3", 1));
        DEPARTMENT_DATA.put("CSE", Map.of("2", 10, "3", 6, "4", 5));
        DEPARTMENT_DATA.put("CSBS", Map.of("2", 2, "3", 2, "4", 2));
        DEPARTMENT_DATA.put("CSD", Map.of("2", 1, "3", 1, "4", 1));
        DEPARTMENT_DATA.put("IT", Map.of("2", 5, "3", 4, "4", 3));
        DEPARTMENT_DATA.put("AIML", Map.of("2", 4, "3", 3, "4", 3));
        DEPARTMENT_DATA.put("AIDS", Map.of("2", 5, "3", 3, "4", 1));
        DEPARTMENT_DATA.put("ECE", Map.of("2", 6, "3", 4, "4", 4));
        DEPARTMENT_DATA.put("EEE", Map.of("2", 2, "3", 2, "4", 2));
        DEPARTMENT_DATA.put("AERO", Map.of("2", 1, "3", 1, "4", 1));
        DEPARTMENT_DATA.put("AUTO", Map.of("2", 1, "3", 1, "4", 1));
        DEPARTMENT_DATA.put("MCT", Map.of("2", 1, "3", 1, "4", 1));
        DEPARTMENT_DATA.put("MECH", Map.of("2", 2, "3", 2, "4", 2));
        DEPARTMENT_DATA.put("BT", Map.of("2", 3, "3", 3, "4", 3));
        DEPARTMENT_DATA.put("BME", Map.of("2", 2, "3", 2, "4", 2));
        DEPARTMENT_DATA.put("R&A", Map.of("2", 1, "3", 1, "4", 1));
        DEPARTMENT_DATA.put("FT", Map.of("2", 1, "3", 1, "4", 1));
        DEPARTMENT_DATA.put("CIVIL", Map.of("2", 1, "3", 1, "4", 1));
        DEPARTMENT_DATA.put("CHEM", Map.of("2", 1, "3", 1, "4", 1));
    }
    
    // Department blocks
    public static final Map<String, String> DEPARTMENT_BLOCKS = new HashMap<>();
    static {
        DEPARTMENT_BLOCKS.put("CSE", "A");
        DEPARTMENT_BLOCKS.put("CSBS", "A");
        DEPARTMENT_BLOCKS.put("CSD", "A");
        DEPARTMENT_BLOCKS.put("AIML", "A or B");
    }
    
    // Lunch break slots
    public static final List<String[]> LUNCH_BREAK_SLOTS = Arrays.asList(
        new String[]{"11:00", "11:50"}, 
        new String[]{"11:50", "12:40"}, 
        new String[]{"12:40", "13:30"}
    );
} 