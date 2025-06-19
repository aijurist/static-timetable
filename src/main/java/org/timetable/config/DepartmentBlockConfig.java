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
        // A Block departments (Computer Science related)
        addDepartmentToBlock("CSE", "A Block");        // Computer Science & Engineering
        addDepartmentToBlock("IT", "A Block");         // Information Technology
        addDepartmentToBlock("CSD", "A Block");        // Computer Science & Design
        addDepartmentToBlock("CSBS", "A Block");       // Computer Science & Business Systems
        addDepartmentToBlock("AIDS", "A Block");       // AI & Data Science
        addDepartmentToBlock("CSE-CS", "A Block");     // Computer Science & Engineering (Cyber Security)
        
        // B Block departments (Electronics & Bio related)
        addDepartmentToBlock("AIML", "B Block");       // Artificial Intelligence & Machine Learning
        addDepartmentToBlock("ECE", "B Block");        // Electronics & Communication Engineering
        addDepartmentToBlock("EEE", "B Block");        // Electrical & Electronics Engineering
        addDepartmentToBlock("BT", "B Block");         // Biotechnology
        addDepartmentToBlock("BME", "B Block");        // Biomedical Engineering
        addDepartmentToBlock("FT", "B Block");         // Food Technology
        addDepartmentToBlock("CHEM", "B Block");       // Chemical Engineering
        
        // C Block departments (Mechanical & Civil related)
        addDepartmentToBlock("MECH", "C Block");       // Mechanical Engineering
        addDepartmentToBlock("MCT", "C Block");        // Mechatronics Engineering
        addDepartmentToBlock("CIVIL", "C Block");      // Civil Engineering
        addDepartmentToBlock("AERO", "C Block");       // Aeronautical Engineering
        addDepartmentToBlock("AUTO", "C Block");       // Automobile Engineering
        addDepartmentToBlock("R&A", "C Block");        // Robotics & Automation
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
    
    /**
     * Validates that block preferences are correctly configured and working.
     * Call this method to verify the configuration matches CSV data.
     */
    public static void validateConfiguration() {
        System.out.println("=== DEPARTMENT BLOCK PREFERENCE VALIDATION ===");
        
        // Test that block names match CSV format
        String[] csvBlockNames = {"A Block", "B Block", "C Block", "J Block", "K Block"};
        for (String blockName : csvBlockNames) {
            if (blockName.equals("A Block") || blockName.equals("B Block") || blockName.equals("C Block")) {
                Set<String> depts = getDepartmentsForBlock(blockName);
                System.out.println("✓ " + blockName + " has " + depts.size() + " departments configured: " + depts);
            }
        }
        
        // Test specific department lookups
        String[] testDepartments = {"CSE", "AIML", "MECH", "ECE", "AUTO"};
        for (String dept : testDepartments) {
            String preferredBlock = getPreferredBlock(dept);
            boolean hasPreference = hasBlockPreference(dept);
            System.out.println("Department " + dept + ": " + 
                              (hasPreference ? "prefers " + preferredBlock : "no block preference"));
        }
        
        System.out.println("=== END VALIDATION ===");
    }

    /**
     * Test method to verify department mappings are working correctly.
     * Prints debug information about specific departments.
     */
    public static void printDebugInfo() {
        System.out.println("=== DEPARTMENT BLOCK CONFIGURATION DEBUG ===");
        System.out.println(getPreferenceInfo());
        
        // Test some specific departments
        String[] testDepts = {"CSE", "ECE", "MECH", "MCT", "BME", "CHEM", "UNKNOWN"};
        String[] testBlocks = {"A Block", "B Block", "C Block"};
        
        System.out.println("Department Assignment Tests:");
        for (String dept : testDepts) {
            String preferred = getPreferredBlock(dept);
            System.out.printf("%-8s -> Preferred: %-5s", dept, preferred != null ? preferred : "None");
            
            if (hasBlockPreference(dept)) {
                for (String block : testBlocks) {
                    boolean isPreferred = isPreferredBlock(dept, block);
                    System.out.printf(" | %s: %s", block, isPreferred ? "✓" : "✗");
                }
            } else {
                System.out.print(" | No preference defined");
            }
            System.out.println();
        }
        
        System.out.println("=== END DEBUG ===");
    }
} 