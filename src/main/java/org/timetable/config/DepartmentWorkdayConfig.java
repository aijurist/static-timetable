package org.timetable.config;

import java.time.DayOfWeek;
import java.util.*;

/**
 * Configuration for optimized department workday policies.
 * Implements optimized lab utilization strategy that groups departments by year
 * to maximize computer lab and core lab usage while balancing workload.
 * 
 * Based on analysis:
 * - 30 computer labs (35-seat capacity each) 
 * - 75 core labs (specialized for specific disciplines)
 * - Optimizes utilization: Computer labs 88% (Mon-Fri) vs 93% (Tue-Sat)
 * - Core labs 43% (Mon-Fri) vs 51% (Tue-Sat)
 */
public class DepartmentWorkdayConfig {
    
    /**
     * Optimized grouping for Monday-Friday schedule
     * Groups computer-heavy years and departments with shared lab requirements
     */
    private static final Map<String, Set<Integer>> MONDAY_FRIDAY_DEPT_YEARS;
    
    /**
     * Optimized grouping for Tuesday-Saturday schedule  
     * Groups AI/ML-focused years and specialized core lab departments
     */
    private static final Map<String, Set<Integer>> TUESDAY_SATURDAY_DEPT_YEARS;
    
    static {
        // Initialize Monday-Friday groupings
        Map<String, Set<Integer>> mondayFridayMap = new HashMap<>();
        mondayFridayMap.put("CSE", Set.of(2, 4));        // CSE Y2 (10 sections), Y4 (5 sections)
        mondayFridayMap.put("CSBS", Set.of(2, 3, 4));    // CSBS all years (2+2+2 sections)
        mondayFridayMap.put("AIML", Set.of(2));          // AIML Y2 (4 sections)
        mondayFridayMap.put("IT", Set.of(3, 4));         // IT Y3 (4 sections), Y4 (3 sections)
        mondayFridayMap.put("ECE", Set.of(2, 4));        // ECE Y2 (6 sections), Y4 (4 sections)
        mondayFridayMap.put("MECH", Set.of(3, 4));       // MECH Y3 (2 sections), Y4 (3 sections)
        mondayFridayMap.put("AUTO", Set.of(2, 3, 4));    // AUTO all years (1+1+1 sections)
        mondayFridayMap.put("CHEM", Set.of(2, 3, 4));    // CHEM all years (1+1+1 sections)
        MONDAY_FRIDAY_DEPT_YEARS = Collections.unmodifiableMap(mondayFridayMap);
        
        // Initialize Tuesday-Saturday groupings
        Map<String, Set<Integer>> tuesdaySaturdayMap = new HashMap<>();
        tuesdaySaturdayMap.put("CSE", Set.of(3));           // CSE Y3 (6 sections)
        tuesdaySaturdayMap.put("AIDS", Set.of(2, 3, 4));    // AIDS all years (5+3+1 sections)
        tuesdaySaturdayMap.put("AIML", Set.of(3, 4));       // AIML Y3 (3 sections), Y4 (3 sections)
        tuesdaySaturdayMap.put("IT", Set.of(2));            // IT Y2 (5 sections)
        tuesdaySaturdayMap.put("ECE", Set.of(3));           // ECE Y3 (4 sections)
        tuesdaySaturdayMap.put("MECH", Set.of(2));          // MECH Y2 (2 sections)
        tuesdaySaturdayMap.put("BME", Set.of(2, 3, 4));     // BME all years (2+2+2 sections)
        tuesdaySaturdayMap.put("AERO", Set.of(2, 3, 4));    // AERO all years (1+1+1 sections)
        tuesdaySaturdayMap.put("R&A", Set.of(2, 3, 4));     // R&A all years (1+1+1 sections)
        tuesdaySaturdayMap.put("CIVIL", Set.of(2, 3, 4));   // CIVIL all years (1+1+1 sections)
        tuesdaySaturdayMap.put("BT", Set.of(2, 3, 4));      // BT all years (3+3+3 sections)
        tuesdaySaturdayMap.put("FT", Set.of(2, 3, 4));      // FT all years (1+1+1 sections)
        tuesdaySaturdayMap.put("MCT", Set.of(2, 3, 4));     // MCT all years (1+1+1 sections)
        tuesdaySaturdayMap.put("EEE", Set.of(2, 3, 4));     // EEE all years (2+2+2 sections)
        tuesdaySaturdayMap.put("CSE-CS", Set.of(2, 3));     // CSE-CS Y2 (2 sections), Y3 (1 section)
        tuesdaySaturdayMap.put("CSD", Set.of(2, 3, 4));     // CSD all years (1+1+1 sections)
        TUESDAY_SATURDAY_DEPT_YEARS = Collections.unmodifiableMap(tuesdaySaturdayMap);
    }
    
    // Legacy department-only mappings (for backward compatibility)
    public static final Set<String> MONDAY_FRIDAY_DEPARTMENTS = Set.of(
        "Aeronautical Engineering",
        "Automobile Engineering", 
        "Mechanical Engineering",
        "Mechatronics Engineering",
        "Civil Engineering",
        "Chemical Engineering",
        "Electrical & Electronics Engineering",
        "Electronics & Communication Engineering"
    );
    
    public static final Set<String> MONDAY_FRIDAY_CODES = Set.of(
        "AUTO", "MECH", "CIVIL", "CHEM", "EEE", "ECE",
        "CSE", "CSBS", "AIML", "IT"
    );
    
    public static final Set<String> TUESDAY_SATURDAY_CODES = Set.of(
        "BME", "BT", "FT", "MCT", "R&A", "AERO", 
        "AIDS", "CSD", "CSE-CS"
    );
    
    public static final Set<String> TUESDAY_SATURDAY_DEPARTMENTS = Set.of(
        "Biomedical Engineering",
        "Biotechnology", 
        "Food Technology",
        "Artificial Intelligence and Data Science",
        "Computer Science and Business Systems",
        "Computer Science and Engineering",
        "Information Technology"
    );
    
    // High-conflict labs for optimal scheduling
    public static final Set<String> HOTSPOT_LABS = Set.of(
        "Physics LAB - 1", "Physics LAB - 2",              
        "Fluid Mechanics Lab", "Chemistry Lab",                
        "Strength of Materials Lab", "Thermal Lab 1", "Thermal Lab 2",                
        "Engineering Practices Lab (Electronics)", "Manufacturing Technology I",   
        "Microprocessor Lab-1", "Microprocessor Lab-2",         
        "Isaac Asimov Laboratory", "Electrical Machines I Lab", "Electrical Machines II Lab",    
        "Industrial Automation Lab", "MCT CAD Lab", "Mech CAD Lab", "Auto CAD Lab"                 
    );
    
    /**
     * Get allowed working days for a department and year combination
     */
    public static EnumSet<DayOfWeek> getAllowedDays(String department, int year) {
        if (department == null) {
            return getDefaultWorkingDays();
        }
        
        String deptCode = department.trim().toUpperCase();
        
        // Check optimized groupings first (department + year)
        if (MONDAY_FRIDAY_DEPT_YEARS.containsKey(deptCode)) {
            Set<Integer> mondayFridayYears = MONDAY_FRIDAY_DEPT_YEARS.get(deptCode);
            if (mondayFridayYears.contains(year)) {
                return EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, 
                                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
            }
        }
        
        if (TUESDAY_SATURDAY_DEPT_YEARS.containsKey(deptCode)) {
            Set<Integer> tuesdaySaturdayYears = TUESDAY_SATURDAY_DEPT_YEARS.get(deptCode);
            if (tuesdaySaturdayYears.contains(year)) {
                return EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, 
                                DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);
            }
        }
        
        // Fallback to legacy department-only mapping
        return getAllowedDays(department);
    }
    
    /**
     * Get allowed working days for a department (legacy method for backward compatibility)
     */
    public static EnumSet<DayOfWeek> getAllowedDays(String department) {
        if (department == null) {
            return getDefaultWorkingDays();
        }
        
        String deptNormalized = department.trim();

        if (MONDAY_FRIDAY_DEPARTMENTS.contains(deptNormalized) || 
            MONDAY_FRIDAY_CODES.contains(deptNormalized.toUpperCase())) {
            return EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, 
                            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
        }
        
        if (TUESDAY_SATURDAY_DEPARTMENTS.contains(deptNormalized) || 
            TUESDAY_SATURDAY_CODES.contains(deptNormalized.toUpperCase())) {
            return EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, 
                            DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);
        }
        
        // Default: all days Monday to Saturday
        return getDefaultWorkingDays();
    }
    
    /**
     * Check if a department and year combination is allowed to work on a specific day
     */
    public static boolean isAllowedDay(String department, int year, DayOfWeek day) {
        return getAllowedDays(department, year).contains(day);
    }
    
    /**
     * Check if a department is allowed to work on a specific day (legacy method)
     */
    public static boolean isAllowedDay(String department, DayOfWeek day) {
        return getAllowedDays(department).contains(day);
    }
    
    /**
     * Check if a lab is a hotspot lab that should be preferred on Monday
     */
    public static boolean isHotspotLab(String labDescription) {
        return HOTSPOT_LABS.contains(labDescription);
    }
    
    /**
     * Check if department+year combination is in Monday-Friday batch
     */
    public static boolean isMondayFridayDepartment(String department, int year) {
        if (department == null) return false;
        String deptCode = department.trim().toUpperCase();
        
        return MONDAY_FRIDAY_DEPT_YEARS.containsKey(deptCode) && 
               MONDAY_FRIDAY_DEPT_YEARS.get(deptCode).contains(year);
    }
    
    /**
     * Check if department is in Monday-Friday batch (legacy method)
     */
    public static boolean isMondayFridayDepartment(String department) {
        if (department == null) return false;
        String d = department.trim();
        return MONDAY_FRIDAY_DEPARTMENTS.contains(d) || 
               MONDAY_FRIDAY_CODES.contains(d.toUpperCase());
    }
    
    /**
     * Check if department+year combination is in Tuesday-Saturday batch
     */
    public static boolean isTuesdaySaturdayDepartment(String department, int year) {
        if (department == null) return false;
        String deptCode = department.trim().toUpperCase();
        
        return TUESDAY_SATURDAY_DEPT_YEARS.containsKey(deptCode) && 
               TUESDAY_SATURDAY_DEPT_YEARS.get(deptCode).contains(year);
    }
    
    /**
     * Get the day order assignment for a department and year
     */
    public static String getDayOrderAssignment(String department, int year) {
        if (isMondayFridayDepartment(department, year)) {
            return "Monday-Friday";
        } else if (isTuesdaySaturdayDepartment(department, year)) {
            return "Tuesday-Saturday";
        } else {
            return "All Days (Monday-Saturday)";
        }
    }
    
    /**
     * Get statistics for the optimized grouping
     */
    public static Map<String, Object> getGroupingStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Count sections in each group
        int mondayFridaySections = 0;
        int tuesdaySaturdaySections = 0;
        
        // Monday-Friday group sections (based on DEPARTMENT_DATA from TimetableDataLoader)
        Map<String, Map<String, Integer>> deptData = Map.of(
            "CSE", Map.of("2", 10, "3", 6, "4", 5),
            "CSBS", Map.of("2", 2, "3", 2, "4", 2),
            "AIML", Map.of("2", 4, "3", 3, "4", 3),
            "IT", Map.of("2", 5, "3", 4, "4", 3),
            "ECE", Map.of("2", 6, "3", 4, "4", 4),
            "MECH", Map.of("2", 2, "3", 2, "4", 3),
            "AUTO", Map.of("2", 1, "3", 1, "4", 1),
            "CHEM", Map.of("2", 1, "3", 1, "4", 1)
        );
        
        for (Map.Entry<String, Set<Integer>> entry : MONDAY_FRIDAY_DEPT_YEARS.entrySet()) {
            String dept = entry.getKey();
            Set<Integer> years = entry.getValue();
            Map<String, Integer> yearSections = deptData.get(dept);
            if (yearSections != null) {
                for (Integer year : years) {
                    Integer sections = yearSections.get(year.toString());
                    if (sections != null) {
                        mondayFridaySections += sections;
                    }
                }
            }
        }
        
        // Calculate Tuesday-Saturday sections similarly
        tuesdaySaturdaySections = 186 - mondayFridaySections; // Total sections - Monday-Friday
        
        stats.put("mondayFridaySections", mondayFridaySections);
        stats.put("tuesdaySaturdaySections", tuesdaySaturdaySections);
        stats.put("totalSections", mondayFridaySections + tuesdaySaturdaySections);
        stats.put("balanceRatio", (double) mondayFridaySections / tuesdaySaturdaySections);
        
        return stats;
    }
    
    private static EnumSet<DayOfWeek> getDefaultWorkingDays() {
        return EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, 
                        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);
    }
} 