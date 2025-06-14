package org.timetable.util;

import org.timetable.domain.Department;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to map between department names and abbreviations.
 */
public class DepartmentMapper {
    private static final Map<String, String> nameToAbbreviation = new HashMap<>();
    private static final Map<String, String> abbreviationToName = new HashMap<>();
    
    static {
        // Initialize the mappings
        addMapping("Computer Science & Design", "CSD");
        addMapping("Computer Science & Engineering", "CSE");
        addMapping("Computer Science & Engineering (Cyber Security)", "CSE-CS");
        addMapping("Computer Science & Business Systems", "CSBS");
        addMapping("Artificial Intelligence & Machine Learning", "AIML");
    }
    
    private static void addMapping(String fullName, String abbreviation) {
        nameToAbbreviation.put(fullName, abbreviation);
        abbreviationToName.put(abbreviation, fullName);
    }
    
    /**
     * Get the abbreviation for a department name.
     * 
     * @param fullName The full department name
     * @return The abbreviation or null if not found
     */
    public static String getAbbreviation(String fullName) {
        return nameToAbbreviation.get(fullName);
    }
    
    /**
     * Get the full name for a department abbreviation.
     * 
     * @param abbreviation The department abbreviation
     * @return The full name or null if not found
     */
    public static String getFullName(String abbreviation) {
        return abbreviationToName.get(abbreviation);
    }
    
    /**
     * Get the Department enum for a full name.
     * 
     * @param fullName The full department name
     * @return The Department enum or null if not found
     */
    public static Department getDepartmentByFullName(String fullName) {
        String abbreviation = getAbbreviation(fullName);
        return abbreviation != null ? Department.getByAbbreviation(abbreviation) : null;
    }
    
    /**
     * Get the Department enum for an abbreviation.
     * 
     * @param abbreviation The department abbreviation
     * @return The Department enum or null if not found
     */
    public static Department getDepartmentByAbbreviation(String abbreviation) {
        return Department.getByAbbreviation(abbreviation);
    }
} 