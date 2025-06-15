package org.timetable.util;

import java.io.*;
import java.util.*;

/**
 * Utility class to load real data from CSV files
 */
public class DataLoader {
    
    public static class TeacherInfo {
        public String teacherId;
        public String staffCode;
        public String firstName;
        public String lastName;
        public String email;
        public String department;
        
        public TeacherInfo(String teacherId, String staffCode, String firstName, String lastName, String email, String department) {
            this.teacherId = teacherId;
            this.staffCode = staffCode;
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.department = department;
        }
        
        public String getFullName() {
            return firstName + " " + lastName;
        }
        
        public String getDisplayName() {
            return staffCode + " - " + getFullName();
        }
    }
    
    public static class RoomInfo {
        public String roomNumber;
        public String block;
        public String description;
        public boolean isLab;
        public String roomType;
        public int minCapacity;
        public int maxCapacity;
        
        public RoomInfo(String roomNumber, String block, String description, boolean isLab, String roomType, int minCapacity, int maxCapacity) {
            this.roomNumber = roomNumber;
            this.block = block;
            this.description = description;
            this.isLab = isLab;
            this.roomType = roomType;
            this.minCapacity = minCapacity;
            this.maxCapacity = maxCapacity;
        }
        
        public String getDisplayName() {
            return roomNumber + " (" + block + ")";
        }
    }
    
    public static class CourseInfo {
        public String courseCode;
        public String courseName;
        public String courseType;
        public String department;
        public int lectureHours;
        public int practicalHours;
        public int tutorialHours;
        public int academicYear;
        public int semester;
        
        public CourseInfo(String courseCode, String courseName, String courseType, String department, 
                         int lectureHours, int practicalHours, int tutorialHours, int academicYear, int semester) {
            this.courseCode = courseCode;
            this.courseName = courseName;
            this.courseType = courseType;
            this.department = department;
            this.lectureHours = lectureHours;
            this.practicalHours = practicalHours;
            this.tutorialHours = tutorialHours;
            this.academicYear = academicYear;
            this.semester = semester;
        }
    }
    
    public static class CourseTeacherMapping {
        public String courseCode;
        public String courseName;
        public String courseType;
        public String teacherId;
        public String teacherName;
        public String department;
        public int lectureHours;
        public int practicalHours;
        public int tutorialHours;
        public int academicYear;
        public int semester;
        
        public CourseTeacherMapping(String courseCode, String courseName, String courseType, 
                                  String teacherId, String teacherName, String department,
                                  int lectureHours, int practicalHours, int tutorialHours, int academicYear, int semester) {
            this.courseCode = courseCode;
            this.courseName = courseName;
            this.courseType = courseType;
            this.teacherId = teacherId;
            this.teacherName = teacherName;
            this.department = department;
            this.lectureHours = lectureHours;
            this.practicalHours = practicalHours;
            this.tutorialHours = tutorialHours;
            this.academicYear = academicYear;
            this.semester = semester;
        }
        
        /**
         * Get total weekly lessons needed for this course
         */
        public int getTotalWeeklyLessons() {
            return lectureHours + practicalHours + tutorialHours;
        }
        
        /**
         * Check if this course has lecture component
         */
        public boolean hasLectures() {
            return lectureHours > 0;
        }
        
        /**
         * Check if this course has practical component
         */
        public boolean hasPracticals() {
            return practicalHours > 0;
        }
        
        /**
         * Check if this course has tutorial component
         */
        public boolean hasTutorials() {
            return tutorialHours > 0;
        }
    }
    
    /**
     * Load teacher data from CSV file
     */
    public static Map<String, List<TeacherInfo>> loadTeachers(String filePath) {
        Map<String, List<TeacherInfo>> teachersByDept = new HashMap<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine(); // Skip header
            
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 19) {
                    String teacherId = parts[5].trim();
                    String staffCode = parts[6].trim();
                    String firstName = parts[7].trim();
                    String lastName = parts[8].trim();
                    String email = parts[9].trim();
                    String department = parts[18].trim();
                    
                    // Filter for CS departments
                    if (isCSRelatedDepartment(department)) {
                        String deptCode = getDepartmentCode(department);
                        TeacherInfo teacher = new TeacherInfo(teacherId, staffCode, firstName, lastName, email, department);
                        
                        teachersByDept.computeIfAbsent(deptCode, k -> new ArrayList<>()).add(teacher);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading teachers: " + e.getMessage());
        }
        
        // Remove duplicates
        for (String dept : teachersByDept.keySet()) {
            List<TeacherInfo> teachers = teachersByDept.get(dept);
            Set<String> seen = new HashSet<>();
            teachers.removeIf(t -> !seen.add(t.staffCode));
        }
        
        return teachersByDept;
    }
    
    /**
     * Load classroom data from CSV files
     */
    public static List<RoomInfo> loadClassrooms() {
        List<RoomInfo> classrooms = new ArrayList<>();
        String[] files = {"data/classroom/a_block.csv", "data/classroom/b_block.csv", 
                         "data/classroom/c_block.csv", "data/classroom/d_block.csv"};
        
        for (String file : files) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line = br.readLine(); // Skip header
                
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 8) {
                        String roomNumber = parts[2].trim();
                        String block = parts[3].trim();
                        String description = parts[4].trim();
                        boolean isLab = "1".equals(parts[5].trim());
                        String roomType = parts[6].trim();
                        int minCap = parseIntSafe(parts[7].trim());
                        int maxCap = parseIntSafe(parts[8].trim());
                        
                        if (!isLab) { // Only classrooms, not labs
                            classrooms.add(new RoomInfo(roomNumber, block, description, isLab, roomType, minCap, maxCap));
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Error loading classrooms from " + file + ": " + e.getMessage());
            }
        }
        
        return classrooms;
    }
    
    /**
     * Load lab data from CSV files
     */
    public static List<RoomInfo> loadLabs() {
        List<RoomInfo> labs = new ArrayList<>();
        String[] files = {"data/labs/lab_core.csv", "data/labs/j_block.csv", "data/labs/k_block.csv"};
        
        for (String file : files) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line = br.readLine(); // Skip header
                
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 8) {
                        String roomNumber = parts[1].trim();
                        String block = parts[2].trim();
                        String description = parts[3].trim();
                        boolean isLab = "1".equals(parts[4].trim());
                        String roomType = parts[5].trim();
                        int minCap = parseIntSafe(parts[6].trim());
                        int maxCap = parseIntSafe(parts[7].trim());
                        
                        if (isLab) { // Only labs
                            labs.add(new RoomInfo(roomNumber, block, description, isLab, roomType, minCap, maxCap));
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Error loading labs from " + file + ": " + e.getMessage());
            }
        }
        
        return labs;
    }
    
    /**
     * Load course data from CSV file
     */
    public static Map<String, List<CourseInfo>> loadCourses(String filePath) {
        Map<String, List<CourseInfo>> coursesByDept = new HashMap<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine(); // Skip header
            
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 19) {
                    int academicYear = parseIntSafe(parts[1].trim());  // Column 1: academic_year
                    int semester = parseIntSafe(parts[2].trim());      // Column 2: semester
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
                        CourseInfo course = new CourseInfo(courseCode, courseName, courseType, department, 
                                                         lectureHours, practicalHours, tutorialHours, academicYear, semester);
                        
                        coursesByDept.computeIfAbsent(deptCode, k -> new ArrayList<>()).add(course);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading courses: " + e.getMessage());
        }
        
        return coursesByDept;
    }
    
    /**
     * Load course-teacher mappings from CSV file
     */
    public static Map<String, List<CourseTeacherMapping>> loadCourseTeacherMappings(String filePath) {
        Map<String, List<CourseTeacherMapping>> mappingsByDept = new HashMap<>();
        
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
                            department, lectureHours, practicalHours, tutorialHours, parseIntSafe(parts[1].trim()), parseIntSafe(parts[2].trim()));
                        
                        mappingsByDept.computeIfAbsent(deptCode, k -> new ArrayList<>()).add(mapping);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading course-teacher mappings: " + e.getMessage());
        }
        
        return mappingsByDept;
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
    
    // Debug method to test room loading
    public static void main(String[] args) {
        System.out.println("=== Testing Room Loading ===");
        
        List<RoomInfo> classrooms = loadClassrooms();
        List<RoomInfo> labs = loadLabs();
        
        System.out.println("Loaded " + classrooms.size() + " classrooms:");
        for (RoomInfo room : classrooms) {
            if (room.isLab) {
                System.err.println("ERROR: Lab room in classrooms list: " + room.getDisplayName() + " - " + room.description);
            }
        }
        
        System.out.println("\nLoaded " + labs.size() + " labs:");
        for (RoomInfo room : labs) {
            if (!room.isLab) {
                System.err.println("ERROR: Non-lab room in labs list: " + room.getDisplayName() + " - " + room.description);
            }
        }
        
        // Check for room 208 and 207
        System.out.println("\n=== Checking for specific rooms ===");
        boolean found208InClassrooms = classrooms.stream().anyMatch(r -> r.roomNumber.equals("208"));
        boolean found208InLabs = labs.stream().anyMatch(r -> r.roomNumber.equals("208"));
        boolean found207InClassrooms = classrooms.stream().anyMatch(r -> r.roomNumber.equals("207"));
        boolean found207InLabs = labs.stream().anyMatch(r -> r.roomNumber.equals("207"));
        
        System.out.println("Room 208 found in classrooms: " + found208InClassrooms);
        System.out.println("Room 208 found in labs: " + found208InLabs);
        System.out.println("Room 207 found in classrooms: " + found207InClassrooms);
        System.out.println("Room 207 found in labs: " + found207InLabs);
        
        if (found208InLabs) {
            labs.stream()
                .filter(r -> r.roomNumber.equals("208"))
                .forEach(r -> System.out.println("  Lab 208: " + r.getDisplayName() + " - " + r.description));
        }
        
        if (found207InLabs) {
            labs.stream()
                .filter(r -> r.roomNumber.equals("207"))
                .forEach(r -> System.out.println("  Lab 207: " + r.getDisplayName() + " - " + r.description));
        }
    }
} 