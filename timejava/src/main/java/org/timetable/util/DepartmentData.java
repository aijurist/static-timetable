package org.timetable.util;

import java.util.*;
import org.timetable.domain.StudentGroup;

/**
 * Configuration class containing department data including sections per year.
 */
public class DepartmentData {
    
    // Department data: Department -> Year -> Number of sections
    private static final Map<String, Map<String, Integer>> DEPARTMENT_DATA = new HashMap<>();
    
    static {
        // Initialize department data based on the provided information
        addDepartmentData("CSE-CS", Map.of("2", 2, "3", 1));
        addDepartmentData("CSE", Map.of("2", 10, "3", 6, "4", 5));
        addDepartmentData("CSBS", Map.of("2", 2, "3", 2, "4", 2));
        addDepartmentData("CSD", Map.of("2", 1, "3", 1, "4", 1));
        addDepartmentData("IT", Map.of("2", 5, "3", 4, "4", 3));
        addDepartmentData("AIML", Map.of("2", 4, "3", 3, "4", 3));
        addDepartmentData("AIDS", Map.of("2", 5, "3", 3, "4", 1));
        addDepartmentData("ECE", Map.of("2", 6, "3", 4, "4", 4));
        addDepartmentData("EEE", Map.of("2", 2, "3", 2, "4", 2));
        addDepartmentData("AERO", Map.of("2", 1, "3", 1, "4", 1));
        addDepartmentData("AUTO", Map.of("2", 1, "3", 1, "4", 1));
        addDepartmentData("MCT", Map.of("2", 1, "3", 1, "4", 1));
        addDepartmentData("MECH", Map.of("2", 2, "3", 2, "4", 2));
        addDepartmentData("BT", Map.of("2", 3, "3", 3, "4", 3));
        addDepartmentData("BME", Map.of("2", 2, "3", 2, "4", 2));
        addDepartmentData("R&A", Map.of("2", 1, "3", 1, "4", 1));
        addDepartmentData("FT", Map.of("2", 1, "3", 1, "4", 1));
        addDepartmentData("CIVIL", Map.of("2", 1, "3", 1, "4", 1));
        addDepartmentData("CHEM", Map.of("2", 1, "3", 1, "4", 1));
    }
    
    private static void addDepartmentData(String dept, Map<String, Integer> yearData) {
        DEPARTMENT_DATA.put(dept, new HashMap<>(yearData));
    }
    
    /**
     * Get all departments.
     */
    public static Set<String> getAllDepartments() {
        return DEPARTMENT_DATA.keySet();
    }
    
    /**
     * Get years for a department.
     */
    public static Set<String> getYearsForDepartment(String department) {
        Map<String, Integer> yearData = DEPARTMENT_DATA.get(department);
        return yearData != null ? yearData.keySet() : Collections.emptySet();
    }
    
    /**
     * Get number of sections for a department and year.
     */
    public static int getSectionCount(String department, String year) {
        Map<String, Integer> yearData = DEPARTMENT_DATA.get(department);
        if (yearData != null) {
            return yearData.getOrDefault(year, 0);
        }
        return 0;
    }
    
    /**
     * Get all student groups (department-year-section combinations).
     */
    public static List<StudentGroup> getAllStudentGroups() {
        List<StudentGroup> groups = new ArrayList<>();
        
        for (String deptCode : getAllDepartments()) {
            try {
                Department dept = Department.fromCode(deptCode);
                
                for (String year : getYearsForDepartment(deptCode)) {
                    int sectionCount = getSectionCount(deptCode, year);
                    
                    for (int i = 1; i <= sectionCount; i++) {
                        char sectionLetter = (char) ('A' + i - 1);
                        String groupId = deptCode + "-Y" + year + "-" + sectionLetter;
                        String groupName = dept.getCode() + " Year " + year + " Section " + sectionLetter;
                        
                        StudentGroup group = new StudentGroup(groupId, groupName, 60, dept); // Assuming 60 students per section
                        group.setYear(Integer.parseInt(year));
                        group.setSection(String.valueOf(sectionLetter));
                        
                        groups.add(group);
                    }
                }
            } catch (IllegalArgumentException e) {
                System.err.println("Warning: Unknown department code: " + deptCode);
            }
        }
        
        return groups;
    }
    
    /**
     * Get total number of student groups.
     */
    public static int getTotalStudentGroups() {
        int total = 0;
        for (String dept : getAllDepartments()) {
            for (String year : getYearsForDepartment(dept)) {
                total += getSectionCount(dept, year);
            }
        }
        return total;
    }
    
    /**
     * Get summary statistics.
     */
    public static void printSummary() {
        System.out.println("Department Data Summary:");
        System.out.println("=======================");
        System.out.printf("%-10s %-15s %-15s %-15s%n", "Dept", "Years", "Total Sections", "Breakdown");
        System.out.println("---------------------------------------------------------------");
        
        int totalSections = 0;
        for (String dept : new TreeSet<>(getAllDepartments())) {
            Set<String> years = getYearsForDepartment(dept);
            int deptTotal = 0;
            StringBuilder breakdown = new StringBuilder();
            
            for (String year : new TreeSet<>(years)) {
                int sections = getSectionCount(dept, year);
                deptTotal += sections;
                if (breakdown.length() > 0) breakdown.append(", ");
                breakdown.append("Y").append(year).append(":").append(sections);
            }
            
            totalSections += deptTotal;
            
            System.out.printf("%-10s %-15s %-15d %-15s%n", 
                    dept, 
                    String.join(", ", new TreeSet<>(years)),
                    deptTotal,
                    breakdown.toString());
        }
        
        System.out.println("---------------------------------------------------------------");
        System.out.printf("Total: %d departments, %d student groups%n", 
                getAllDepartments().size(), totalSections);
    }
} 