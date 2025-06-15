package org.timetable.generator;

import org.timetable.domain.*;
import org.timetable.util.*;
import java.util.*;

/**
 * Standalone timetable generator that creates schedules without OptaPlanner.
 */
public class StandaloneTimetableGenerator {
    
    private static final Random random = new Random(42); // Fixed seed for reproducibility
    
    public static void main(String[] args) {
        System.out.println("Starting comprehensive timetable generation...");
        
        // Print department data summary
        DepartmentData.printSummary();
        
        // Generate timetables for all departments and sections
        generateAllTimetables();
        
        System.out.println("Timetable generation completed!");
    }
    
    private static void generateAllTimetables() {
        // Get all student groups from the comprehensive data
        List<StudentGroup> allStudentGroups = DepartmentData.getAllStudentGroups();
        
        System.out.println("\nGenerating timetables for " + allStudentGroups.size() + " student groups...");
        
        // Generate teachers for all departments
        Map<String, List<Teacher>> teachersByDept = generateTeachers();
        
        // Generate courses for all departments
        Map<String, List<Course>> coursesByDept = generateCourses();
        
        // Generate student timetables
        for (StudentGroup group : allStudentGroups) {
            generateStudentTimetable(group, teachersByDept, coursesByDept);
        }
        
        // Generate teacher timetables
        for (String deptCode : DepartmentData.getAllDepartments()) {
            List<Teacher> teachers = teachersByDept.get(deptCode);
            if (teachers != null) {
                for (Teacher teacher : teachers) {
                    generateTeacherTimetable(teacher, allStudentGroups, coursesByDept);
                }
            }
        }
        
        System.out.println("Generated timetables for:");
        System.out.println("- " + allStudentGroups.size() + " student groups");
        System.out.println("- " + teachersByDept.values().stream().mapToInt(List::size).sum() + " teachers");
    }
    
    private static Map<String, List<Teacher>> generateTeachers() {
        Map<String, List<Teacher>> teachersByDept = new HashMap<>();
        
        for (String deptCode : DepartmentData.getAllDepartments()) {
            List<Teacher> teachers = new ArrayList<>();
            
            // Calculate number of teachers needed based on student groups
            int studentGroups = 0;
            for (String year : DepartmentData.getYearsForDepartment(deptCode)) {
                studentGroups += DepartmentData.getSectionCount(deptCode, year);
            }
            
            // Generate 1 teacher per 2-3 student groups, minimum 3, maximum 10
            int teacherCount = Math.max(3, Math.min(10, (studentGroups + 2) / 3));
            
            for (int i = 1; i <= teacherCount; i++) {
                String teacherId = deptCode + "_Teacher_" + i;
                String teacherName = deptCode + " Teacher " + i;
                Teacher teacher = new Teacher(teacherId, teacherName);
                teachers.add(teacher);
            }
            
            teachersByDept.put(deptCode, teachers);
        }
        
        return teachersByDept;
    }
    
    private static Map<String, List<Course>> generateCourses() {
        Map<String, List<Course>> coursesByDept = new HashMap<>();
        
        for (String deptCode : DepartmentData.getAllDepartments()) {
            List<Course> courses = new ArrayList<>();
            
            // Generate courses for each year
            for (String year : DepartmentData.getYearsForDepartment(deptCode)) {
                int yearNum = Integer.parseInt(year);
                
                // Theory courses
                for (int i = 1; i <= 6; i++) {
                    String courseId = deptCode + "_Y" + year + "_Theory_" + i;
                    String courseName = deptCode + " Year " + year + " Theory " + i;
                    Course course = new Course(courseId, courseName, "theory");
                    courses.add(course);
                }
                
                // Lab courses
                for (int i = 1; i <= 3; i++) {
                    String courseId = deptCode + "_Y" + year + "_Lab_" + i;
                    String courseName = deptCode + " Year " + year + " Lab " + i;
                    Course course = new Course(courseId, courseName, "lab");
                    courses.add(course);
                }
                
                // Tutorial courses
                for (int i = 1; i <= 3; i++) {
                    String courseId = deptCode + "_Y" + year + "_Tutorial_" + i;
                    String courseName = deptCode + " Year " + year + " Tutorial " + i;
                    Course course = new Course(courseId, courseName, "tutorial");
                    courses.add(course);
                }
            }
            
            coursesByDept.put(deptCode, courses);
        }
        
        return coursesByDept;
    }
    
    private static void generateStudentTimetable(StudentGroup group, 
            Map<String, List<Teacher>> teachersByDept, 
            Map<String, List<Course>> coursesByDept) {
        
        String deptCode = group.getDepartment().getCode();
        List<Teacher> teachers = teachersByDept.get(deptCode);
        List<Course> courses = coursesByDept.get(deptCode);
        
        if (teachers == null || courses == null) {
            System.err.println("Warning: No teachers or courses found for department: " + deptCode);
            return;
        }
        
        // Filter courses for this year
        String yearStr = String.valueOf(group.getYear());
        List<Course> yearCourses = courses.stream()
                .filter(c -> c.getId().contains("_Y" + yearStr + "_"))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        
        // Create timetable
        Map<String, Map<String, String>> timetable = new HashMap<>();
        
        // Initialize empty timetable
        for (String day : TimetableConstants.DAYS) {
            Map<String, String> daySchedule = new HashMap<>();
            for (String timeSlot : TimetableConstants.TIME_SLOTS) {
                daySchedule.put(timeSlot, "");
            }
            timetable.put(day, daySchedule);
        }
        
        // Assign courses to time slots
        List<String> allSlots = new ArrayList<>();
        for (String day : TimetableConstants.DAYS) {
            for (String timeSlot : TimetableConstants.TIME_SLOTS) {
                allSlots.add(day + "|" + timeSlot);
            }
        }
        Collections.shuffle(allSlots, random);
        
        int slotIndex = 0;
        for (Course course : yearCourses) {
            if (slotIndex >= allSlots.size()) break;
            
            String[] dayTime = allSlots.get(slotIndex).split("\\|");
            String day = dayTime[0];
            String timeSlot = dayTime[1];
            
            Teacher teacher = teachers.get(random.nextInt(teachers.size()));
            
            String classInfo = course.getName() + "<br>" + teacher.getName();
            timetable.get(day).put(timeSlot, classInfo);
            
            slotIndex++;
        }
        
        // Export to HTML
        TimetableExporter.exportStudentTimetable(group, timetable);
    }
    
    private static void generateTeacherTimetable(Teacher teacher, 
            List<StudentGroup> allStudentGroups,
            Map<String, List<Course>> coursesByDept) {
        
        // Create timetable
        Map<String, Map<String, String>> timetable = new HashMap<>();
        
        // Initialize empty timetable
        for (String day : TimetableConstants.DAYS) {
            Map<String, String> daySchedule = new HashMap<>();
            for (String timeSlot : TimetableConstants.TIME_SLOTS) {
                daySchedule.put(timeSlot, "");
            }
            timetable.put(day, daySchedule);
        }
        
        // Get teacher's department from ID
        String teacherId = teacher.getId();
        String deptCode = teacherId.split("_")[0];
        
        // Get student groups for this department
        List<StudentGroup> deptGroups = allStudentGroups.stream()
                .filter(g -> g.getDepartment().getCode().equals(deptCode))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        
        List<Course> courses = coursesByDept.get(deptCode);
        if (courses == null || courses.isEmpty()) {
            System.err.println("Warning: No courses found for department: " + deptCode);
            return;
        }
        
        if (deptGroups.isEmpty()) {
            System.err.println("Warning: No student groups found for department: " + deptCode);
            return;
        }
        
        // Assign classes to teacher
        List<String> allSlots = new ArrayList<>();
        for (String day : TimetableConstants.DAYS) {
            for (String timeSlot : TimetableConstants.TIME_SLOTS) {
                allSlots.add(day + "|" + timeSlot);
            }
        }
        Collections.shuffle(allSlots, random);
        
        int classesAssigned = 0;
        int maxClasses = 15 + random.nextInt(10); // 15-25 classes per teacher
        
        for (String slot : allSlots) {
            if (classesAssigned >= maxClasses) break;
            
            String[] dayTime = slot.split("\\|");
            String day = dayTime[0];
            String timeSlot = dayTime[1];
            
            if (random.nextDouble() < 0.6) { // 60% chance to assign a class
                Course course = courses.get(random.nextInt(courses.size()));
                StudentGroup group = deptGroups.get(random.nextInt(deptGroups.size()));
                
                String classInfo = course.getName() + "<br>" + group.getName();
                timetable.get(day).put(timeSlot, classInfo);
                classesAssigned++;
            }
        }
        
        // Export to HTML
        TimetableExporter.exportTeacherTimetable(teacher, timetable);
    }
} 