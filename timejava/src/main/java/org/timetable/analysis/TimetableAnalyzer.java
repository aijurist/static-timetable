package org.timetable.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analyzes timetable data to ensure all classes and departments have proper assignments.
 */
public class TimetableAnalyzer {
    
    private static final String STUDENT_TIMETABLES_DIR = "output/student_timetables";
    private static final String TEACHER_TIMETABLES_DIR = "output/teacher_timetables";
    
    // Patterns for extracting information from filenames
    private static final Pattern STUDENT_PATTERN = Pattern.compile("timetable_([A-Z-]+)_Y(\\d)_([A-Z]).html");
    private static final Pattern TEACHER_PATTERN = Pattern.compile("timetable_teacher_([A-Z-]+)_Teacher_(\\d+).html");
    
    // Patterns for extracting information from HTML content
    private static final Pattern CLASS_PATTERN = Pattern.compile("<td class=\"(\\w+)\">\\s*([^<]+)<br>");
    
    public static void main(String[] args) {
        System.out.println("Analyzing timetable data...");
        
        // Analyze student timetables
        analyzeStudentTimetables();
        
        // Analyze teacher timetables
        analyzeTeacherTimetables();
        
        // Cross-reference analysis
        crossReferenceAnalysis();
    }
    
    /**
     * Analyzes student timetables.
     */
    private static void analyzeStudentTimetables() {
        System.out.println("\n=== STUDENT TIMETABLE ANALYSIS ===");
        
        File dir = new File(STUDENT_TIMETABLES_DIR);
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("Student timetables directory not found.");
            return;
        }
        
        File[] files = dir.listFiles((d, name) -> name.endsWith(".html"));
        if (files == null || files.length == 0) {
            System.out.println("No student timetables found.");
            return;
        }
        
        // Department statistics
        Map<String, Integer> deptCount = new HashMap<>();
        Map<String, Set<String>> deptYears = new HashMap<>();
        Map<String, Set<String>> deptSections = new HashMap<>();
        
        // Class type statistics
        Map<String, Map<String, Integer>> deptClassTypes = new HashMap<>();
        
        for (File file : files) {
            String filename = file.getName();
            Matcher matcher = STUDENT_PATTERN.matcher(filename);
            
            if (matcher.find()) {
                String dept = matcher.group(1);
                String year = matcher.group(2);
                String section = matcher.group(3);
                
                // Update department statistics
                deptCount.put(dept, deptCount.getOrDefault(dept, 0) + 1);
                
                // Update year statistics
                deptYears.computeIfAbsent(dept, k -> new HashSet<>()).add(year);
                
                // Update section statistics
                deptSections.computeIfAbsent(dept, k -> new HashSet<>()).add(section);
                
                // Analyze class types
                try {
                    String content = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
                    Matcher classMatcher = CLASS_PATTERN.matcher(content);
                    
                    while (classMatcher.find()) {
                        String classType = classMatcher.group(1);
                        
                        // Update class type statistics
                        Map<String, Integer> classTypes = deptClassTypes.computeIfAbsent(dept, k -> new HashMap<>());
                        classTypes.put(classType, classTypes.getOrDefault(classType, 0) + 1);
                    }
                } catch (IOException e) {
                    System.out.println("Error reading file: " + file.getName());
                }
            }
        }
        
        // Print department statistics
        System.out.println("\nDepartment Statistics:");
        System.out.println("---------------------");
        System.out.printf("%-10s %-15s %-15s %-15s%n", "Dept", "Timetables", "Years", "Sections");
        System.out.println("--------------------------------------------------");
        
        for (String dept : new TreeSet<>(deptCount.keySet())) {
            System.out.printf("%-10s %-15d %-15s %-15s%n", 
                    dept, 
                    deptCount.get(dept),
                    String.join(", ", new TreeSet<>(deptYears.get(dept))),
                    String.join(", ", new TreeSet<>(deptSections.get(dept))));
        }
        
        // Print class type statistics
        System.out.println("\nClass Type Statistics by Department:");
        System.out.println("----------------------------------");
        System.out.printf("%-10s %-15s %-15s %-15s%n", "Dept", "Theory", "Lab", "Tutorial");
        System.out.println("--------------------------------------------------");
        
        for (String dept : new TreeSet<>(deptClassTypes.keySet())) {
            Map<String, Integer> classTypes = deptClassTypes.get(dept);
            System.out.printf("%-10s %-15d %-15d %-15d%n", 
                    dept, 
                    classTypes.getOrDefault("theory", 0),
                    classTypes.getOrDefault("lab", 0),
                    classTypes.getOrDefault("tutorial", 0));
        }
    }
    
    /**
     * Analyzes teacher timetables.
     */
    private static void analyzeTeacherTimetables() {
        System.out.println("\n=== TEACHER TIMETABLE ANALYSIS ===");
        
        File dir = new File(TEACHER_TIMETABLES_DIR);
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("Teacher timetables directory not found.");
            return;
        }
        
        File[] files = dir.listFiles((d, name) -> name.endsWith(".html"));
        if (files == null || files.length == 0) {
            System.out.println("No teacher timetables found.");
            return;
        }
        
        // Department statistics
        Map<String, Integer> deptCount = new HashMap<>();
        
        // Class type statistics
        Map<String, Map<String, Integer>> deptClassTypes = new HashMap<>();
        
        // Teacher load statistics
        Map<String, Integer> teacherLoad = new HashMap<>();
        
        for (File file : files) {
            String filename = file.getName();
            Matcher matcher = TEACHER_PATTERN.matcher(filename);
            
            if (matcher.find()) {
                String dept = matcher.group(1);
                String teacherNumber = matcher.group(2);
                String teacherId = dept + "_" + teacherNumber;
                
                // Update department statistics
                deptCount.put(dept, deptCount.getOrDefault(dept, 0) + 1);
                
                // Initialize teacher load
                teacherLoad.put(teacherId, 0);
                
                // Analyze class types
                try {
                    String content = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
                    Matcher classMatcher = CLASS_PATTERN.matcher(content);
                    
                    while (classMatcher.find()) {
                        String classType = classMatcher.group(1);
                        
                        // Update class type statistics
                        Map<String, Integer> classTypes = deptClassTypes.computeIfAbsent(dept, k -> new HashMap<>());
                        classTypes.put(classType, classTypes.getOrDefault(classType, 0) + 1);
                        
                        // Update teacher load
                        teacherLoad.put(teacherId, teacherLoad.get(teacherId) + 1);
                    }
                } catch (IOException e) {
                    System.out.println("Error reading file: " + file.getName());
                }
            }
        }
        
        // Print department statistics
        System.out.println("\nDepartment Statistics:");
        System.out.println("---------------------");
        System.out.printf("%-10s %-15s%n", "Dept", "Teachers");
        System.out.println("--------------------------");
        
        for (String dept : new TreeSet<>(deptCount.keySet())) {
            System.out.printf("%-10s %-15d%n", dept, deptCount.get(dept));
        }
        
        // Print class type statistics
        System.out.println("\nClass Type Statistics by Department:");
        System.out.println("----------------------------------");
        System.out.printf("%-10s %-15s %-15s %-15s%n", "Dept", "Theory", "Lab", "Tutorial");
        System.out.println("--------------------------------------------------");
        
        for (String dept : new TreeSet<>(deptClassTypes.keySet())) {
            Map<String, Integer> classTypes = deptClassTypes.get(dept);
            System.out.printf("%-10s %-15d %-15d %-15d%n", 
                    dept, 
                    classTypes.getOrDefault("theory", 0),
                    classTypes.getOrDefault("lab", 0),
                    classTypes.getOrDefault("tutorial", 0));
        }
        
        // Print teacher load statistics
        System.out.println("\nTeacher Load Statistics:");
        System.out.println("------------------------");
        System.out.printf("%-15s %-15s%n", "Teacher", "Classes");
        System.out.println("--------------------------");
        
        // Calculate min, max, and average load
        int minLoad = Integer.MAX_VALUE;
        int maxLoad = Integer.MIN_VALUE;
        int totalLoad = 0;
        
        for (String teacherId : new TreeSet<>(teacherLoad.keySet())) {
            int load = teacherLoad.get(teacherId);
            System.out.printf("%-15s %-15d%n", teacherId, load);
            
            minLoad = Math.min(minLoad, load);
            maxLoad = Math.max(maxLoad, load);
            totalLoad += load;
        }
        
        double avgLoad = (double) totalLoad / teacherLoad.size();
        
        System.out.println("\nLoad Summary:");
        System.out.printf("Min: %d, Max: %d, Avg: %.2f%n", minLoad, maxLoad, avgLoad);
    }
    
    /**
     * Cross-references student and teacher timetables.
     */
    private static void crossReferenceAnalysis() {
        System.out.println("\n=== CROSS-REFERENCE ANALYSIS ===");
        
        // Check if all departments have both student and teacher timetables
        Set<String> studentDepts = new HashSet<>();
        Set<String> teacherDepts = new HashSet<>();
        
        // Get student departments
        File studentDir = new File(STUDENT_TIMETABLES_DIR);
        if (studentDir.exists() && studentDir.isDirectory()) {
            File[] files = studentDir.listFiles((d, name) -> name.endsWith(".html"));
            if (files != null) {
                for (File file : files) {
                    Matcher matcher = STUDENT_PATTERN.matcher(file.getName());
                    if (matcher.find()) {
                        studentDepts.add(matcher.group(1));
                    }
                }
            }
        }
        
        // Get teacher departments
        File teacherDir = new File(TEACHER_TIMETABLES_DIR);
        if (teacherDir.exists() && teacherDir.isDirectory()) {
            File[] files = teacherDir.listFiles((d, name) -> name.endsWith(".html"));
            if (files != null) {
                for (File file : files) {
                    Matcher matcher = TEACHER_PATTERN.matcher(file.getName());
                    if (matcher.find()) {
                        teacherDepts.add(matcher.group(1));
                    }
                }
            }
        }
        
        // Find departments with only student or only teacher timetables
        Set<String> studentOnlyDepts = new HashSet<>(studentDepts);
        studentOnlyDepts.removeAll(teacherDepts);
        
        Set<String> teacherOnlyDepts = new HashSet<>(teacherDepts);
        teacherOnlyDepts.removeAll(studentDepts);
        
        System.out.println("\nDepartment Coverage:");
        System.out.println("-------------------");
        System.out.println("Total departments: " + (studentDepts.size() + teacherDepts.size() - 
                (studentDepts.size() + teacherDepts.size() - studentOnlyDepts.size() - teacherOnlyDepts.size())));
        System.out.println("Departments with both student and teacher timetables: " + 
                (studentDepts.size() - studentOnlyDepts.size()));
        System.out.println("Departments with only student timetables: " + studentOnlyDepts.size() + 
                (studentOnlyDepts.isEmpty() ? "" : " (" + String.join(", ", studentOnlyDepts) + ")"));
        System.out.println("Departments with only teacher timetables: " + teacherOnlyDepts.size() + 
                (teacherOnlyDepts.isEmpty() ? "" : " (" + String.join(", ", teacherOnlyDepts) + ")"));
    }
} 