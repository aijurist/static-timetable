package org.timetable.config;

import java.time.DayOfWeek;
import java.util.*;

/**
 * Configuration for department workday policies.
 * Splits departments into two batches:
 * - Batch A: Monday to Friday (contains high-conflict labs)
 * - Batch B: Tuesday to Saturday (specialized labs with less overlap)
 */
public class DepartmentWorkdayConfig {
    
    // Batch A departments (Monday to Friday)
    public static final Set<String> MONDAY_FRIDAY_DEPARTMENTS = Set.of(
        "Aeronautical Engineering",
        "Automobile Engineering", 
        "Mechanical Engineering",
        "Mechatronics Engineering",
        "Robotics & Automation",
        "Robotics and Automation", // both variations
        "Civil Engineering",
        "Chemical Engineering",
        "Electrical & Electronics Engineering",
        "Electrical and Electronics Engineering", // both variations
        "Electronics & Communication Engineering",
        "Electronics and Communication Engineering" // both variations
    );
    
    // Department code aliases
    // These are short codes used across the system (e.g., in StudentGroup objects)
    public static final Set<String> MONDAY_FRIDAY_CODES = Set.of(
        // Core departments
        "AERO",  // Aeronautical Engineering
        "AUTO",  // Automobile Engineering  
        "MECH",  // Mechanical Engineering
        "CIVIL", // Civil Engineering
        "CHEM",  // Chemical Engineering
        "EEE",   // Electrical & Electronics Engineering
        // Computer departments for balance
        "CSE",   // Computer Science and Engineering  // Information Technology
        "AIDS",
        "CSBS"
    );
    
    // Batch B departments (Tuesday to Saturday)
    public static final Set<String> TUESDAY_SATURDAY_CODES = Set.of(
        // Core departments
        "BME",   // Biomedical Engineering
        "BT",    // Biotechnology
        "FT",    // Food Technology
        "ECE",   // Electronics & Communication Engineering
        "MCT",   // Mechatronics Engineering
        "RA", "R&A", // Robotics & Automation
        "CSD",
        "IT",   // Computer Science & Design
        "AIML"   // Artificial Intelligence & Machine Learning
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
    
    // High-conflict labs that should be preferred on Monday (for Monday-Friday departments)
    // These are the most shared labs based on usage analysis
    public static final Set<String> HOTSPOT_LABS = Set.of(
        "Physics LAB - 1",              // Shared by 7 departments
        "Physics LAB - 2",              // Shared by 7 departments  
        "Fluid Mechanics Lab",          // Shared by 6 departments
        "Chemistry Lab",                // Shared by 5 departments
        "Strength of Materials Lab",    // Shared by 5 departments
        "Thermal Lab 1",                // Shared by 4 departments
        "Thermal Lab 2",                // Shared by 4 departments
        "Engineering Practices Lab (Electronics)", // Shared by 4 departments
        "Manufacturing Technology I",   // Shared by 4 departments
        "Microprocessor Lab-1",         // Shared by 4 departments
        "Microprocessor Lab-2",         // Shared by 4 departments
        "Isaac Asimov Laboratory",      // Shared by 4 departments
        "Electrical Machines I Lab",    // Shared by 3 departments
        "Electrical Machines II Lab",   // Shared by 3 departments
        "Industrial Automation Lab",    // Core engineering lab
        "MCT CAD Lab",                  // CAD/Design lab
        "Mech CAD Lab",                 // CAD/Design lab
        "Auto CAD Lab"                  // CAD/Design lab
    );
    
    /**
     * Get allowed working days for a department
     */
    public static EnumSet<DayOfWeek> getAllowedDays(String department) {
        if (department == null) {
            return EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, 
                            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);
        }
        
        String deptNormalized = department.trim();

        if (MONDAY_FRIDAY_DEPARTMENTS.contains(deptNormalized) || MONDAY_FRIDAY_CODES.contains(deptNormalized.toUpperCase())) {
            return EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, 
                            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
        }
        
        if (TUESDAY_SATURDAY_DEPARTMENTS.contains(deptNormalized) || TUESDAY_SATURDAY_CODES.contains(deptNormalized.toUpperCase())) {
            return EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, 
                            DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);
        }
        
        // Default: all days Monday to Saturday
        return EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, 
                        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);
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
     * Check if department is in Monday-Friday batch (for hotspot lab preference)
     */
    public static boolean isMondayFridayDepartment(String department) {
        if (department == null) return false;
        String d = department.trim();
        return MONDAY_FRIDAY_DEPARTMENTS.contains(d) || MONDAY_FRIDAY_CODES.contains(d.toUpperCase());
    }
} 