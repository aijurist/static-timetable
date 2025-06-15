package org.timetable.generator;

import java.util.*;
import java.util.stream.Collectors;
import java.io.*;

/**
 * Standalone CS Department Timetable Generator with correct course-teacher mappings
 */
public class StandaloneCSETimetableGenerator {
    
    // Simple domain classes
    static class Teacher {
        private String id;
        private String name;
        
        public Teacher(String id, String name) {
            this.id = id;
            this.name = name;
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
    }
    
    static class Course {
        private String id;
        private String name;
        private String courseType;
        
        public Course(String id, String name, String courseType) {
            this.id = id;
            this.name = name;
            this.courseType = courseType;
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public String getCourseType() { return courseType; }
    }
    
    static class StudentGroup {
        private String id;
        private String name;
        private int studentCount;
        private String department;
        private int year;
        private String section;
        
        public StudentGroup(String id, String name, int studentCount, String department) {
            this.id = id;
            this.name = name;
            this.studentCount = studentCount;
            this.department = department;
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public int getStudentCount() { return studentCount; }
        public String getDepartment() { return department; }
        public int getYear() { return year; }
        public String getSection() { return section; }
        
        public void setYear(int year) { this.year = year; }
        public void setSection(String section) { this.section = section; }
    }
    
    static class CourseTeacherMapping {
        public String courseCode;
        public String courseName;
        public String courseType;
        public String teacherId;
        public String teacherName;
        public String department;
        public int lectureHours;
        public int practicalHours;
        public int tutorialHours;
        
        public CourseTeacherMapping(String courseCode, String courseName, String courseType, 
                                  String teacherId, String teacherName, String department,
                                  int lectureHours, int practicalHours, int tutorialHours) {
            this.courseCode = courseCode;
            this.courseName = courseName;
            this.courseType = courseType;
            this.teacherId = teacherId;
            this.teacherName = teacherName;
            this.department = department;
            this.lectureHours = lectureHours;
            this.practicalHours = practicalHours;
            this.tutorialHours = tutorialHours;
        }
    }
    
    // Constants
    private static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
    private static final String[] THEORY_TIME_SLOTS = {
        "8:00-8:50", "8:50-9:40", "9:50-10:40", "10:40-11:30", "11:50-12:40", 
        "12:40-13:30", "13:50-14:40", "14:40-15:30", "15:50-16:40", "16:40-17:30", "17:50-18:40"
    };
    private static final String[] LAB_TIME_SLOTS = {
        "8:00-9:40", "9:50-11:30", "11:50-13:30", "13:50-15:30", "15:50-17:30", "17:50-19:30"
    };
    private static final int MAX_TEACHER_HOURS = 21;
    private static final int CLASS_STRENGTH = 70;
    private static final int LAB_BATCH_SIZE = 35;
    
    // CS Department data
    private static final Map<String, Map<String, Integer>> CS_DEPARTMENT_DATA = new HashMap<>();
    static {
        CS_DEPARTMENT_DATA.put("CSD", Map.of("2", 1, "3", 1, "4", 1));
        CS_DEPARTMENT_DATA.put("CSE", Map.of("2", 10, "3", 6, "4", 5));
        CS_DEPARTMENT_DATA.put("CSE-CS", Map.of("2", 2, "3", 1));
        CS_DEPARTMENT_DATA.put("CSBS", Map.of("2", 2, "3", 2, "4", 2));
        CS_DEPARTMENT_DATA.put("IT", Map.of("2", 5, "3", 4, "4", 3));
        CS_DEPARTMENT_DATA.put("AIML", Map.of("2", 4, "3", 3, "4", 3));
    }
    
    // Data storage
    private static Map<String, List<CourseTeacherMapping>> courseTeacherMappings = new HashMap<>();
    
    public static void main(String[] args) {
        System.out.println("Starting Standalone CS Department Timetable Generation...");
        System.out.println("Loading course-teacher mappings from CSV...");
        
        // Load course-teacher mappings
        loadCourseTeacherMappings("data/courses/cse_dept_red.csv");
        
        System.out.println("Loaded " + courseTeacherMappings.values().stream().mapToInt(List::size).sum() + " course-teacher mappings");
        
        // Test the mapping for specific courses
        testCourseTeacherMappings();
        
        System.out.println("Standalone CS timetable generation completed!");
    }
    
    private static void loadCourseTeacherMappings(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine(); // Skip header
            
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 19) {
                    String teacherId = parts[5].trim();
                    String staffCode = parts[6].trim();
                    String firstName = parts[7].trim();
                    String lastName = parts[8].trim();
                    String courseCode = parts[11].trim();
                    String courseName = parts[12].trim();
                    String courseType = parts[13].trim();
                    String department = parts[18].trim();
                    int lectureHours = parseIntSafe(parts[14].trim());
                    int practicalHours = parseIntSafe(parts[15].trim());
                    int tutorialHours = parseIntSafe(parts[16].trim());
                    
                    // Filter for CS departments
                    if (isCSRelatedDepartment(department)) {
                        String deptCode = getDepartmentCode(department);
                        String teacherName = staffCode + " - " + firstName + " " + lastName;
                        
                        CourseTeacherMapping mapping = new CourseTeacherMapping(
                            courseCode, courseName, courseType, teacherId, teacherName, 
                            department, lectureHours, practicalHours, tutorialHours);
                        
                        courseTeacherMappings.computeIfAbsent(deptCode, k -> new ArrayList<>()).add(mapping);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading course-teacher mappings: " + e.getMessage());
        }
    }
    
    private static void testCourseTeacherMappings() {
        System.out.println("\n=== TESTING COURSE-TEACHER MAPPINGS ===");
        
        // Test Database Management Systems
        System.out.println("\nDatabase Management Systems should be taught by:");
        for (String dept : courseTeacherMappings.keySet()) {
            for (CourseTeacherMapping mapping : courseTeacherMappings.get(dept)) {
                if (mapping.courseName.equals("Database Management Systems")) {
                    System.out.println("  " + dept + ": " + mapping.teacherName);
                }
            }
        }
        
        // Test what IT03 - Dr. Kumar P should teach
        System.out.println("\nIT03 - Dr. Kumar P should teach:");
        for (String dept : courseTeacherMappings.keySet()) {
            for (CourseTeacherMapping mapping : courseTeacherMappings.get(dept)) {
                if (mapping.teacherName.contains("IT03") && mapping.teacherName.contains("Dr. Kumar P")) {
                    System.out.println("  " + dept + ": " + mapping.courseName);
                }
            }
        }
        
        // Show some sample mappings
        System.out.println("\nSample course-teacher mappings:");
        int count = 0;
        for (String dept : courseTeacherMappings.keySet()) {
            for (CourseTeacherMapping mapping : courseTeacherMappings.get(dept)) {
                if (count < 10) {
                    System.out.println("  " + dept + ": " + mapping.courseName + " -> " + mapping.teacherName);
                    count++;
                }
            }
        }
    }
    
    private static boolean isCSRelatedDepartment(String department) {
        return department.contains("Computer Science") || 
               department.contains("Information Technology") || 
               department.contains("Artificial Intelligence");
    }
    
    private static String getDepartmentCode(String department) {
        if (department.contains("Computer Science & Design")) return "CSD";
        if (department.contains("Computer Science & Engineering (Cyber Security)")) return "CSE-CS";
        if (department.contains("Computer Science & Engineering")) return "CSE";
        if (department.contains("Computer Science & Business Systems")) return "CSBS";
        if (department.contains("Information Technology")) return "IT";
        if (department.contains("Artificial Intelligence")) return "AIML";
        return "CSE"; // Default
    }
    
    private static int parseIntSafe(String str) {
        try {
            return str.isEmpty() ? 0 : Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
} 