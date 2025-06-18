package org.timetable.config;

import java.util.*;

/**
 * Configuration for department block preferences.
 * Defines which block each department prefers for theory and tutorial sessions
 * to minimize student travel time between classes.
 */
public class DepartmentBlockConfig {
    
    private static final Map<String, String> DEPARTMENT_TO_PREFERRED_BLOCK = new HashMap<>();
    private static final Map<String, Set<String>> BLOCK_TO_DEPARTMENTS = new HashMap<>();
    
    static {
        // A Block departments
        addDepartmentToBlock("CSE", "A");
        addDepartmentToBlock("IT", "A");
        addDepartmentToBlock("CSD", "A");
        addDepartmentToBlock("CSBS", "A");
        addDepartmentToBlock("AIDS", "A");
        
        // B Block departments
        addDepartmentToBlock("AIML", "B");
        addDepartmentToBlock("ECE", "B");
        addDepartmentToBlock("EEE", "B");
        addDepartmentToBlock("BT", "B");
        addDepartmentToBlock("FT", "B");
        
        // C Block departments
        addDepartmentToBlock("MECH", "C");
        addDepartmentToBlock("CIVIL", "C");
        addDepartmentToBlock("AERO", "C");
        addDepartmentToBlock("AUTO", "C");
        addDepartmentToBlock("R&A", "C");
        
        // Additional departments (can be added as needed)
        // Mechanical related
        addDepartmentToBlock("MT", "C"); // Mechatronics
        addDepartmentToBlock("RO", "C"); // Robotics
        
        // Biomedical and related
        addDepartmentToBlock("BM", "B"); // Biomedical
        addDepartmentToBlock("CH", "B"); // Chemical
        
        // Aerospace related  
        addDepartmentToBlock("AE", "C"); // Aeronautical
        addDepartmentToBlock("AT", "C"); // Automobile
    }
    
    private static void addDepartmentToBlock(String department, String block) {
        DEPARTMENT_TO_PREFERRED_BLOCK.put(department.toUpperCase(), block.toUpperCase());
        BLOCK_TO_DEPARTMENTS.computeIfAbsent(block.toUpperCase(), k -> new HashSet<>()).add(department.toUpperCase());
    }
    
    /**
     * Gets the preferred block for a department.
     * Returns null if no preference is defined (department can use any block).
     */
    public static String getPreferredBlock(String department) {
        if (department == null) return null;
        return DEPARTMENT_TO_PREFERRED_BLOCK.get(department.toUpperCase());
    }
    
    /**
     * Checks if a department has a block preference defined.
     */
    public static boolean hasBlockPreference(String department) {
        return getPreferredBlock(department) != null;
    }
    
    /**
     * Checks if a room's block matches the department's preferred block.
     */
    public static boolean isPreferredBlock(String department, String roomBlock) {
        if (department == null || roomBlock == null) return true; // No preference violation
        String preferredBlock = getPreferredBlock(department);
        if (preferredBlock == null) return true; // No preference defined, so no violation
        return preferredBlock.equalsIgnoreCase(roomBlock);
    }
    
    /**
     * Gets all departments that prefer a specific block.
     */
    public static Set<String> getDepartmentsForBlock(String block) {
        if (block == null) return Collections.emptySet();
        return new HashSet<>(BLOCK_TO_DEPARTMENTS.getOrDefault(block.toUpperCase(), Collections.emptySet()));
    }
    
    /**
     * Gets all defined blocks.
     */
    public static Set<String> getAllBlocks() {
        return new HashSet<>(BLOCK_TO_DEPARTMENTS.keySet());
    }
    
    /**
     * Calculates the preference penalty for assigning a department to a specific block.
     * Returns 0 if it's the preferred block, higher values for non-preferred blocks.
     */
    public static int getBlockPreferencePenalty(String department, String roomBlock) {
        if (!hasBlockPreference(department)) {
            return 0; // No preference, no penalty
        }
        
        if (isPreferredBlock(department, roomBlock)) {
            return 0; // Perfect match, no penalty
        }
        
        return 1; // Non-preferred block, mild penalty
    }
    
    /**
     * Gets debug information about department block preferences.
     */
    public static String getPreferenceInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Department Block Preferences:\n");
        
        for (String block : getAllBlocks()) {
            sb.append("Block ").append(block).append(": ");
            sb.append(String.join(", ", getDepartmentsForBlock(block)));
            sb.append("\n");
        }
        
        return sb.toString();
    }
} 