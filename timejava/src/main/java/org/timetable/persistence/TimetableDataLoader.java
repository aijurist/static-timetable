package org.timetable.persistence;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.timetable.domain.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class TimetableDataLoader {

    public static TimetableProblem loadProblem(String coursesFile, String roomsDir) {
        try {
            // Load teachers, courses, and student groups
            Map<String, Teacher> teachers = new HashMap<>();
            Map<String, Course> courses = new HashMap<>();
            Map<String, StudentGroup> studentGroups = new HashMap<>();
            
            loadTeachersAndCourses(coursesFile, teachers, courses);
            
            // Create student groups A through F
            for (char c = 'A'; c <= 'F'; c++) {
                String id = String.valueOf(c);
                studentGroups.put(id, new StudentGroup(id, "Section " + id, 70));
            }
            
            // Load rooms (both classrooms and labs)
            List<Room> rooms = loadRooms(roomsDir);
            
            // Create time slots
            List<TimeSlot> timeSlots = createTimeSlots();
            
            // Create lessons
            List<Lesson> lessons = createLessons(teachers, courses, studentGroups);
            
            return new TimetableProblem(
                "Timetable Problem",
                timeSlots,
                rooms,
                new ArrayList<>(teachers.values()),
                new ArrayList<>(courses.values()),
                new ArrayList<>(studentGroups.values()),
                lessons
            );
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to load timetable data", e);
        }
    }
    
    private static void loadTeachersAndCourses(String filePath, 
                                              Map<String, Teacher> teachers, 
                                              Map<String, Course> courses) throws IOException {
        try (Reader reader = new FileReader(filePath);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            
            for (CSVRecord record : csvParser) {
                String teacherId = record.get("teacher_id");
                String courseId = record.get("course_id");
                
                // Skip if teacher_id or course_id is empty or "Unknown"
                if (teacherId == null || teacherId.isEmpty() || "Unknown".equalsIgnoreCase(teacherId)) {
                    continue;
                }
                
                // Create teacher if not exists
                if (!teachers.containsKey(teacherId)) {
                    String firstName = record.get("first_name");
                    String lastName = record.get("last_name");
                    String name = (firstName + " " + lastName).trim();
                    String email = record.get("teacher_email");
                    
                    Teacher teacher = new Teacher(teacherId, name, email, 40);
                    teachers.put(teacherId, teacher);
                }
                
                // Create course if not exists
                if (!courses.containsKey(courseId)) {
                    String courseName = record.get("course_name");
                    String courseCode = record.get("course_code");
                    String courseDept = record.get("course_dept");
                    
                    // Parse integers with error handling
                    int lectureHours = parseIntSafely(record.get("lecture_hours"), 3);
                    int practicalHours = parseIntSafely(record.get("practical_hours"), 0);
                    int tutorialHours = parseIntSafely(record.get("tutorial_hours"), 0);
                    int credits = parseIntSafely(record.get("credits"), 3);
                    
                    Course course = new Course(courseId, courseCode, courseName, courseDept, 
                                             lectureHours, tutorialHours, practicalHours, credits);
                    courses.put(courseId, course);
                }
                
                // Assign course to teacher
                Teacher teacher = teachers.get(teacherId);
                Course course = courses.get(courseId);
                teacher.addCourse(course);
            }
        }
    }
    
    private static List<Room> loadRooms(String roomsDir) throws IOException {
        List<Room> rooms = new ArrayList<>();
        
        // Load classrooms
        File classroomDir = new File(roomsDir + "/classroom");
        if (classroomDir.exists() && classroomDir.isDirectory()) {
            File[] classroomFiles = classroomDir.listFiles((dir, name) -> name.endsWith(".csv"));
            if (classroomFiles != null) {
                for (File file : classroomFiles) {
                    rooms.addAll(loadRoomsFromFile(file.getPath(), false));
                }
            }
        }
        
        // Load labs
        File labsDir = new File(roomsDir + "/labs");
        if (labsDir.exists() && labsDir.isDirectory()) {
            File[] labFiles = labsDir.listFiles((dir, name) -> name.endsWith(".csv"));
            if (labFiles != null) {
                for (File file : labFiles) {
                    rooms.addAll(loadRoomsFromFile(file.getPath(), true));
                }
            }
        }
        
        return rooms;
    }
    
    private static List<Room> loadRoomsFromFile(String filePath, boolean isLab) throws IOException {
        List<Room> rooms = new ArrayList<>();
        
        try (Reader reader = new FileReader(filePath);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            
            for (CSVRecord record : csvParser) {
                String id = record.get("id");
                String name = record.get("room_number");
                String block = record.get("block");
                String description = record.get("description");
                
                // Parse capacity with error handling
                int capacity = parseIntSafely(record.get("room_max_cap"), 70);
                
                // Override isLab if the file has an is_lab column
                boolean roomIsLab = isLab;
                if (record.isMapped("is_lab")) {
                    roomIsLab = "1".equals(record.get("is_lab"));
                }
                
                Room room = new Room(id, name, block, description, capacity, roomIsLab);
                rooms.add(room);
            }
        }
        
        return rooms;
    }
    
    private static List<TimeSlot> createTimeSlots() {
        List<TimeSlot> timeSlots = new ArrayList<>();
        int id = 0;
        
        // Theory slots (1 hour each)
        LocalTime[] theoryTimes = {
            LocalTime.of(8, 0), LocalTime.of(9, 0), LocalTime.of(10, 0), LocalTime.of(11, 0),
            LocalTime.of(12, 0), LocalTime.of(13, 0), LocalTime.of(14, 0), LocalTime.of(15, 0)
        };
        
        for (DayOfWeek day : new DayOfWeek[] {
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, 
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
            }) {
            for (LocalTime startTime : theoryTimes) {
                LocalTime endTime = startTime.plusHours(1);
                timeSlots.add(new TimeSlot("TS" + id++, day, startTime, endTime, false));
            }
        }
        
        // Lab slots (2 hours each)
        LocalTime[] labTimes = {
            LocalTime.of(8, 0), LocalTime.of(10, 0), LocalTime.of(12, 0), LocalTime.of(14, 0)
        };
        
        for (DayOfWeek day : new DayOfWeek[] {
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, 
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
            }) {
            for (LocalTime startTime : labTimes) {
                LocalTime endTime = startTime.plusHours(2);
                timeSlots.add(new TimeSlot("TS" + id++, day, startTime, endTime, true));
            }
        }
        
        return timeSlots;
    }
    
    private static List<Lesson> createLessons(Map<String, Teacher> teachers, 
                                            Map<String, Course> courses, 
                                            Map<String, StudentGroup> studentGroups) {
        List<Lesson> lessons = new ArrayList<>();
        int id = 0;
        
        for (Teacher teacher : teachers.values()) {
            for (Course course : teacher.getAssignedCourses()) {
                for (StudentGroup group : studentGroups.values()) {
                    // Create theory lessons (A1, A2, A3) if course has lecture hours
                    if (course.getLectureHours() > 0) {
                        for (int i = 1; i <= 3; i++) {
                            lessons.add(new Lesson(
                                "L" + id++,
                                teacher,
                                course,
                                group,
                                "lecture",
                                "A" + i,
                                null
                            ));
                        }
                    }
                    
                    // Create lab lessons (TA1, TA2) if course has practical hours
                    if (course.getPracticalHours() > 0) {
                        for (int i = 1; i <= 2; i++) {
                            lessons.add(new Lesson(
                                "L" + id++,
                                teacher,
                                course,
                                group,
                                "lab",
                                "TA" + i,
                                null
                            ));
                        }
                    }
                    
                    // Create tutorial lessons (T1) if course has tutorial hours
                    if (course.getTutorialHours() > 0) {
                        lessons.add(new Lesson(
                            "L" + id++,
                            teacher,
                            course,
                            group,
                            "tutorial",
                            "T1",
                            null
                        ));
                    }
                }
            }
        }
        
        return lessons;
    }
    
    private static int parseIntSafely(String value, int defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // Try to parse as float and convert to int
            try {
                return (int) Float.parseFloat(value);
            } catch (NumberFormatException ex) {
                return defaultValue;
            }
        }
    }
} 