package org.timetable.config;

import java.time.DayOfWeek;
import java.util.*;

/**
 * Simplified configuration for department workday policies.
 * Each department is assigned to either Monday-Friday or Tuesday-Saturday schedule.
 */
public class DepartmentWorkdayConfig {
    
    /**
     * Departments that work Monday to Friday
     */
    public static final Set<String> MONDAY_FRIDAY_DEPARTMENTS = Set.of(
        "CSE",      // Computer Science & Engineering
        "CSBS",     // Computer Science & Business Systems
        "ECE",      // Electronics & Communication Engineering
        "MECH",     // Mechanical Engineering
        "AUTO",     // Automobile Engineering
        "CHEM",     // Chemical Engineering
        "CSD",      // Computer Science & Design
        "CSE-CS",   // Computer Science & Engineering (Cyber Security)
        "FT"        // Food Technology
    );
    
    /**
     * Departments that work Tuesday to Saturday
     */
    public static final Set<String> TUESDAY_SATURDAY_DEPARTMENTS = Set.of(
        "AERO",     // Aeronautical Engineering
        "AIDS",     // AI & Data Science
        "AIML",     // Artificial Intelligence & Machine Learning
        "BME",      // Biomedical Engineering
        "BT",       // Biotechnology
        "CIVIL",    // Civil Engineering
        "EEE",      // Electrical & Electronics Engineering
        "IT",       // Information Technology
        "MCT",      // Mechatronics Engineering
        "R&A"       // Robotics & Automation
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
     * Get allowed working days for a department
     */
    public static EnumSet<DayOfWeek> getAllowedDays(String department) {
        if (department == null) {
            return getDefaultWorkingDays();
        }
        
        String deptCode = department.trim().toUpperCase();
        
        if (MONDAY_FRIDAY_DEPARTMENTS.contains(deptCode)) {
            return EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, 
                            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
        }
        
        if (TUESDAY_SATURDAY_DEPARTMENTS.contains(deptCode)) {
            return EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, 
                            DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);
        }
        
        // Default: all days Monday to Saturday for unknown departments
        return getDefaultWorkingDays();
    }
    
    /**
     * Check if a department is allowed to work on a specific day
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
     * Check if department is in Monday-Friday schedule
     */
    public static boolean isMondayFridayDepartment(String department) {
        if (department == null) return false;
        return MONDAY_FRIDAY_DEPARTMENTS.contains(department.trim().toUpperCase());
    }
    
    /**
     * Check if department is in Tuesday-Saturday schedule
     */
    public static boolean isTuesdaySaturdayDepartment(String department) {
        if (department == null) return false;
        return TUESDAY_SATURDAY_DEPARTMENTS.contains(department.trim().toUpperCase());
    }
    
    /**
     * Get the schedule type for a department
     */
    public static String getScheduleType(String department) {
        if (isMondayFridayDepartment(department)) {
            return "Monday-Friday";
        } else if (isTuesdaySaturdayDepartment(department)) {
            return "Tuesday-Saturday";
        } else {
            return "All Days (Monday-Saturday)";
        }
    }
    
    /**
     * Get statistics for the department groupings
     */
    public static Map<String, Object> getGroupingStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("mondayFridayDepartments", MONDAY_FRIDAY_DEPARTMENTS.size());
        stats.put("tuesdaySaturdayDepartments", TUESDAY_SATURDAY_DEPARTMENTS.size());
        stats.put("totalDepartments", MONDAY_FRIDAY_DEPARTMENTS.size() + TUESDAY_SATURDAY_DEPARTMENTS.size());
        stats.put("mondayFridayList", new ArrayList<>(MONDAY_FRIDAY_DEPARTMENTS));
        stats.put("tuesdaySaturdayList", new ArrayList<>(TUESDAY_SATURDAY_DEPARTMENTS));
        return stats;
    }
    
    private static EnumSet<DayOfWeek> getDefaultWorkingDays() {
        return EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, 
                        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);
    }
} 