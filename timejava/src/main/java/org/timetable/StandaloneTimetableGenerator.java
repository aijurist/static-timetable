package org.timetable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Standalone timetable generator that creates a timetable without dependencies on other classes.
 */
public class StandaloneTimetableGenerator {
    private static final Logger logger = LoggerFactory.getLogger(StandaloneTimetableGenerator.class);
    
    // Constants
    private static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
    private static final String[] THEORY_TIMES = {
        "8:00-8:50", "9:00-9:50", "10:00-10:50", "11:00-11:50", "12:00-12:50",
        "13:00-13:50", "14:00-14:50", "15:00-15:50", "16:00-16:50", "17:00-17:50"
    };
    private static final String[] LAB_TIMES = {
        "8:00-9:40", "9:50-11:30", "11:50-13:30", "13:50-15:30", "15:50-17:30"
    };
    
    // Department data
    private static final String[] DEPARTMENTS = {"CSD", "CSE", "CSE-CS", "CSBS", "AIML"};
    
    // Simple domain classes
    static class TimeSlot {
        int id;
        int day;  // 0 = Monday, 1 = Tuesday, etc.
        String timeStr;
        boolean isLab;
        
        public TimeSlot(int id, int day, String timeStr, boolean isLab) {
            this.id = id;
            this.day = day;
            this.timeStr = timeStr;
            this.isLab = isLab;
        }
        
        @Override
        public String toString() {
            return DAYS[day] + " " + timeStr;
        }
    }
    
    static class Room {
        String id;
        String name;
        int capacity;
        boolean isLab;
        
        public Room(String id, String name, int capacity, boolean isLab) {
            this.id = id;
            this.name = name;
            this.capacity = capacity;
            this.isLab = isLab;
        }
    }
    
    static class Teacher {
        String id;
        String name;
        String department;
        
        public Teacher(String id, String name, String department) {
            this.id = id;
            this.name = name;
            this.department = department;
        }
    }
    
    static class Course {
        String code;
        String name;
        String department;
        String type;  // "theory", "lab", "tutorial"
        
        public Course(String code, String name, String department, String type) {
            this.code = code;
            this.name = name;
            this.department = department;
            this.type = type;
        }
    }
    
    static class StudentGroup {
        String id;
        String name;
        int size;
        String department;
        int year;
        String section;
        
        public StudentGroup(String id, String name, String department, int year, String section, int size) {
            this.id = id;
            this.name = name;
            this.department = department;
            this.year = year;
            this.section = section;
            this.size = size;
        }
    }
    
    static class Lesson {
        int id;
        Teacher teacher;
        Course course;
        StudentGroup group;
        TimeSlot timeSlot;
        Room room;
        
        public Lesson(int id, Teacher teacher, Course course, StudentGroup group) {
            this.id = id;
            this.teacher = teacher;
            this.course = course;
            this.group = group;
        }
    }
    
    public static void main(String[] args) {
        logger.info("Starting Standalone Timetable Generator");
        
        // Create data
        List<TimeSlot> timeSlots = createTimeSlots();
        List<Room> rooms = createRooms();
        List<Teacher> teachers = createTeachers();
        List<StudentGroup> studentGroups = createStudentGroups();
        List<Course> courses = createCourses();
        List<Lesson> lessons = createLessons(teachers, courses, studentGroups);
        
        // Assign lessons to time slots and rooms
        assignLessons(lessons, timeSlots, rooms);
        
        // Generate timetables
        generateTimetables(lessons, studentGroups, teachers);
        
        logger.info("Timetable generation completed!");
    }
    
    /**
     * Create time slots.
     */
    private static List<TimeSlot> createTimeSlots() {
        List<TimeSlot> timeSlots = new ArrayList<>();
        int id = 1;
        
        // Create theory time slots
        for (int day = 0; day < DAYS.length; day++) {
            for (String time : THEORY_TIMES) {
                timeSlots.add(new TimeSlot(id++, day, time, false));
            }
        }
        
        // Create lab time slots
        for (int day = 0; day < DAYS.length; day++) {
            for (String time : LAB_TIMES) {
                timeSlots.add(new TimeSlot(id++, day, time, true));
            }
        }
        
        logger.info("Created {} time slots", timeSlots.size());
        return timeSlots;
    }
    
    /**
     * Create rooms.
     */
    private static List<Room> createRooms() {
        List<Room> rooms = new ArrayList<>();
        
        // Create theory rooms
        for (int i = 1; i <= 20; i++) {
            rooms.add(new Room("TR" + i, "Theory Room " + i, 70, false));
        }
        
        // Create lab rooms
        for (int i = 1; i <= 10; i++) {
            rooms.add(new Room("LR" + i, "Lab Room " + i, 40, true));
        }
        
        logger.info("Created {} rooms", rooms.size());
        return rooms;
    }
    
    /**
     * Create teachers.
     */
    private static List<Teacher> createTeachers() {
        List<Teacher> teachers = new ArrayList<>();
        int id = 1;
        
        // Create teachers for each department
        for (String dept : DEPARTMENTS) {
            for (int i = 1; i <= 5; i++) {
                teachers.add(new Teacher("T" + id++, dept + " Teacher " + i, dept));
            }
        }
        
        logger.info("Created {} teachers", teachers.size());
        return teachers;
    }
    
    /**
     * Create student groups.
     */
    private static List<StudentGroup> createStudentGroups() {
        List<StudentGroup> groups = new ArrayList<>();
        int id = 1;
        
        // Create student groups for each department
        for (String dept : DEPARTMENTS) {
            // For each year (2, 3, 4)
            for (int year = 2; year <= 4; year++) {
                // Number of sections depends on the department
                int numSections = (dept.equals("CSE") || dept.equals("AIML")) ? 3 : 1;
                
                // For each section
                for (int section = 0; section < numSections; section++) {
                    String sectionLetter = String.valueOf((char)('A' + section));
                    String name = dept + "-" + year + sectionLetter;
                    groups.add(new StudentGroup("SG" + id++, name, dept, year, sectionLetter, 70));
                }
            }
        }
        
        logger.info("Created {} student groups", groups.size());
        return groups;
    }
    
    /**
     * Create courses.
     */
    private static List<Course> createCourses() {
        List<Course> courses = new ArrayList<>();
        
        // Create courses for each department
        for (String dept : DEPARTMENTS) {
            // Theory courses
            for (int i = 1; i <= 5; i++) {
                courses.add(new Course(dept + "T" + i, dept + " Theory " + i, dept, "theory"));
            }
            
            // Lab courses
            for (int i = 1; i <= 3; i++) {
                courses.add(new Course(dept + "L" + i, dept + " Lab " + i, dept, "lab"));
            }
            
            // Tutorial courses
            for (int i = 1; i <= 2; i++) {
                courses.add(new Course(dept + "TU" + i, dept + " Tutorial " + i, dept, "tutorial"));
            }
        }
        
        logger.info("Created {} courses", courses.size());
        return courses;
    }
    
    /**
     * Create lessons.
     */
    private static List<Lesson> createLessons(List<Teacher> teachers, List<Course> courses, List<StudentGroup> groups) {
        List<Lesson> lessons = new ArrayList<>();
        int id = 1;
        
        // For each student group
        for (StudentGroup group : groups) {
            // Find courses for this department
            List<Course> deptCourses = new ArrayList<>();
            for (Course course : courses) {
                if (course.department.equals(group.department)) {
                    deptCourses.add(course);
                }
            }
            
            // Find teachers for this department
            List<Teacher> deptTeachers = new ArrayList<>();
            for (Teacher teacher : teachers) {
                if (teacher.department.equals(group.department)) {
                    deptTeachers.add(teacher);
                }
            }
            
            // Assign courses to student group
            if (!deptCourses.isEmpty() && !deptTeachers.isEmpty()) {
                int teacherIndex = 0;
                
                // Assign theory courses (3 lessons per course)
                for (Course course : deptCourses) {
                    if (course.type.equals("theory")) {
                        Teacher teacher = deptTeachers.get(teacherIndex % deptTeachers.size());
                        teacherIndex++;
                        
                        // Create 3 lessons for this theory course
                        for (int i = 0; i < 3; i++) {
                            lessons.add(new Lesson(id++, teacher, course, group));
                        }
                    }
                }
                
                // Assign lab courses (1 lesson per course)
                for (Course course : deptCourses) {
                    if (course.type.equals("lab")) {
                        Teacher teacher = deptTeachers.get(teacherIndex % deptTeachers.size());
                        teacherIndex++;
                        
                        // Create 1 lesson for this lab course
                        lessons.add(new Lesson(id++, teacher, course, group));
                    }
                }
                
                // Assign tutorial courses (2 lessons per course)
                for (Course course : deptCourses) {
                    if (course.type.equals("tutorial")) {
                        Teacher teacher = deptTeachers.get(teacherIndex % deptTeachers.size());
                        teacherIndex++;
                        
                        // Create 2 lessons for this tutorial course
                        for (int i = 0; i < 2; i++) {
                            lessons.add(new Lesson(id++, teacher, course, group));
                        }
                    }
                }
            }
        }
        
        logger.info("Created {} lessons", lessons.size());
        return lessons;
    }
    
    /**
     * Assign lessons to time slots and rooms.
     */
    private static void assignLessons(List<Lesson> lessons, List<TimeSlot> timeSlots, List<Room> rooms) {
        // Split rooms into lab and theory rooms
        List<Room> labRooms = new ArrayList<>();
        List<Room> theoryRooms = new ArrayList<>();
        
        for (Room room : rooms) {
            if (room.isLab) {
                labRooms.add(room);
            } else {
                theoryRooms.add(room);
            }
        }
        
        // Split time slots into lab and theory slots
        List<TimeSlot> labTimeSlots = new ArrayList<>();
        List<TimeSlot> theoryTimeSlots = new ArrayList<>();
        
        for (TimeSlot slot : timeSlots) {
            if (slot.isLab) {
                labTimeSlots.add(slot);
            } else {
                theoryTimeSlots.add(slot);
            }
        }
        
        // Randomly assign lessons to time slots and rooms
        Random random = new Random(42); // Use a fixed seed for reproducibility
        
        for (Lesson lesson : lessons) {
            if (lesson.course.type.equals("lab")) {
                // Assign lab lesson to a lab time slot and lab room
                if (!labTimeSlots.isEmpty() && !labRooms.isEmpty()) {
                    TimeSlot timeSlot = labTimeSlots.get(random.nextInt(labTimeSlots.size()));
                    Room room = labRooms.get(random.nextInt(labRooms.size()));
                    lesson.timeSlot = timeSlot;
                    lesson.room = room;
                }
            } else {
                // Assign theory/tutorial lesson to a theory time slot and theory room
                if (!theoryTimeSlots.isEmpty() && !theoryRooms.isEmpty()) {
                    TimeSlot timeSlot = theoryTimeSlots.get(random.nextInt(theoryTimeSlots.size()));
                    Room room = theoryRooms.get(random.nextInt(theoryRooms.size()));
                    lesson.timeSlot = timeSlot;
                    lesson.room = room;
                }
            }
        }
        
        // Count assigned and unassigned lessons
        int assignedCount = 0;
        int unassignedCount = 0;
        
        for (Lesson lesson : lessons) {
            if (lesson.timeSlot != null && lesson.room != null) {
                assignedCount++;
            } else {
                unassignedCount++;
            }
        }
        
        logger.info("Assigned lessons: {}", assignedCount);
        logger.info("Unassigned lessons: {}", unassignedCount);
    }
    
    /**
     * Generate timetables for student groups and teachers.
     */
    private static void generateTimetables(List<Lesson> lessons, List<StudentGroup> studentGroups, List<Teacher> teachers) {
        // Create output directories
        File studentTimetablesDir = new File("output/student_timetables");
        File teacherTimetablesDir = new File("output/teacher_timetables");
        
        studentTimetablesDir.mkdirs();
        teacherTimetablesDir.mkdirs();
        
        // Generate student timetables
        for (StudentGroup group : studentGroups) {
            List<Lesson> groupLessons = new ArrayList<>();
            for (Lesson lesson : lessons) {
                if (lesson.group.equals(group) && lesson.timeSlot != null && lesson.room != null) {
                    groupLessons.add(lesson);
                }
            }
            
            String filename = "timetable_" + group.department + "_Y" + group.year + "_" + group.section + ".html";
            generateTimetableHtml(group.name, groupLessons, new File(studentTimetablesDir, filename));
        }
        
        // Generate teacher timetables
        for (Teacher teacher : teachers) {
            List<Lesson> teacherLessons = new ArrayList<>();
            for (Lesson lesson : lessons) {
                if (lesson.teacher.equals(teacher) && lesson.timeSlot != null && lesson.room != null) {
                    teacherLessons.add(lesson);
                }
            }
            
            String filename = "timetable_teacher_" + teacher.name.replaceAll("\\s+", "_") + ".html";
            generateTimetableHtml(teacher.name, teacherLessons, new File(teacherTimetablesDir, filename));
        }
        
        logger.info("Generated timetables in output/ directory");
    }
    
    /**
     * Generate an HTML timetable for the given lessons.
     */
    private static void generateTimetableHtml(String title, List<Lesson> lessons, File outputFile) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write("<!DOCTYPE html>\n");
            writer.write("<html>\n");
            writer.write("<head>\n");
            writer.write("  <title>Timetable for " + title + "</title>\n");
            writer.write("  <style>\n");
            writer.write("    table { border-collapse: collapse; width: 100%; }\n");
            writer.write("    th, td { border: 1px solid #ddd; padding: 8px; text-align: center; }\n");
            writer.write("    th { background-color: #f2f2f2; }\n");
            writer.write("    .theory { background-color: #e6f7ff; }\n");
            writer.write("    .lab { background-color: #ffe6e6; }\n");
            writer.write("    .tutorial { background-color: #e6ffe6; }\n");
            writer.write("  </style>\n");
            writer.write("</head>\n");
            writer.write("<body>\n");
            writer.write("  <h1>Timetable for " + title + "</h1>\n");
            writer.write("  <table>\n");
            writer.write("    <tr>\n");
            writer.write("      <th>Time</th>\n");
            
            // Write day headers
            for (String day : DAYS) {
                writer.write("      <th>" + day + "</th>\n");
            }
            writer.write("    </tr>\n");
            
            // For theory time slots
            for (String timeStr : THEORY_TIMES) {
                writer.write("    <tr>\n");
                writer.write("      <td>" + timeStr + "</td>\n");
                
                // For each day
                for (int day = 0; day < DAYS.length; day++) {
                    Lesson lessonAtTimeSlot = findLessonAtTimeSlot(lessons, day, timeStr, false);
                    if (lessonAtTimeSlot != null) {
                        String cellClass = lessonAtTimeSlot.course.type;
                        writer.write("      <td class=\"" + cellClass + "\">\n");
                        writer.write("        " + lessonAtTimeSlot.course.name + "<br>\n");
                        writer.write("        " + lessonAtTimeSlot.teacher.name + "<br>\n");
                        writer.write("        " + lessonAtTimeSlot.room.name + "<br>\n");
                        writer.write("        (" + lessonAtTimeSlot.course.type + ")\n");
                        writer.write("      </td>\n");
                    } else {
                        writer.write("      <td></td>\n");
                    }
                }
                
                writer.write("    </tr>\n");
            }
            
            // For lab time slots
            for (String timeStr : LAB_TIMES) {
                writer.write("    <tr>\n");
                writer.write("      <td>" + timeStr + "</td>\n");
                
                // For each day
                for (int day = 0; day < DAYS.length; day++) {
                    Lesson lessonAtTimeSlot = findLessonAtTimeSlot(lessons, day, timeStr, true);
                    if (lessonAtTimeSlot != null) {
                        String cellClass = lessonAtTimeSlot.course.type;
                        writer.write("      <td class=\"" + cellClass + "\">\n");
                        writer.write("        " + lessonAtTimeSlot.course.name + "<br>\n");
                        writer.write("        " + lessonAtTimeSlot.teacher.name + "<br>\n");
                        writer.write("        " + lessonAtTimeSlot.room.name + "<br>\n");
                        writer.write("        (" + lessonAtTimeSlot.course.type + ")\n");
                        writer.write("      </td>\n");
                    } else {
                        writer.write("      <td></td>\n");
                    }
                }
                
                writer.write("    </tr>\n");
            }
            
            writer.write("  </table>\n");
            writer.write("</body>\n");
            writer.write("</html>\n");
        } catch (IOException e) {
            logger.error("Error writing timetable to {}: {}", outputFile, e.getMessage());
        }
    }
    
    /**
     * Find a lesson at the given day and time.
     */
    private static Lesson findLessonAtTimeSlot(List<Lesson> lessons, int day, String timeStr, boolean isLab) {
        for (Lesson lesson : lessons) {
            if (lesson.timeSlot != null && lesson.timeSlot.day == day && 
                lesson.timeSlot.timeStr.equals(timeStr) && lesson.timeSlot.isLab == isLab) {
                return lesson;
            }
        }
        return null;
    }
} 