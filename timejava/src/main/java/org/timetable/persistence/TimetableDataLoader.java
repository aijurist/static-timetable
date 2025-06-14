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

    // --- Configuration Constants (from your Python config) ---
    private static final int CLASS_STRENGTH = 70;
    private static final Map<String, String> DEPT_NAME_TO_CODE = Map.of(
            "Computer Science & Design", "CSD",
            "Computer Science & Engineering", "CSE",
            "Computer Science & Engineering (Cyber Security)", "CSE-CS",
            "Computer Science & Business Systems", "CSBS",
            "Information Technology", "IT",
            "Artificial Intelligence & Machine Learning", "AIML"
    );

    private static final Map<String, Map<String, Integer>> DEPARTMENT_DATA = Map.of(
            "CSE-CS", Map.of("2", 2, "3", 1),
            "CSE", Map.of("2", 10, "3", 6, "4", 5),
            "CSBS", Map.of("2", 2, "3", 2, "4", 2),
            "CSD", Map.of("2", 1, "3", 1, "4", 1),
            "IT", Map.of("2", 5, "3", 4, "4", 3),
            "AIML", Map.of("2", 4, "3", 3, "4", 3)
    );
    
    // A simple record to hold the raw CSV data before processing
    private static class RawDataRecord {
        final String courseId, courseCode, courseName, courseDept, courseType, teacherId, staffCode, firstName, lastName, teacherEmail;
        final int semester, lectureHours, practicalHours, tutorialHours, credits;

        RawDataRecord(CSVRecord record) {
            this.courseId = record.get("course_id");
            this.courseCode = record.get("course_code");
            this.courseName = record.get("course_name");
            this.courseDept = record.get("course_dept");
            this.courseType = record.get("course_type");
            this.teacherId = record.get("teacher_id");
            this.staffCode = record.get("staff_code");
            this.firstName = record.get("first_name");
            this.lastName = record.get("last_name");
            this.teacherEmail = record.get("teacher_email");
            this.semester = parseIntSafely(record.get("semester"), 0);
            this.lectureHours = parseIntSafely(record.get("lecture_hours"), 0);
            this.practicalHours = parseIntSafely(record.get("practical_hours"), 0);
            this.tutorialHours = parseIntSafely(record.get("tutorial_hours"), 0);
            this.credits = parseIntSafely(record.get("credits"), 0);
        }
    }


    public static TimetableProblem loadProblem(String coursesFile, String roomsDir) {
        try {
            // 1. Load all raw data from CSV into memory, filtering for relevant semesters
            List<RawDataRecord> rawData = loadRawData(coursesFile);

            // 2. Create unique domain objects from raw data
            Map<String, Teacher> teachers = createTeachers(rawData);
            Map<String, Course> courses = createCourses(rawData);

            // 3. Create student groups based on department configuration
            List<StudentGroup> studentGroups = createStudentGroups(rawData);

            // 4. Load rooms
            List<Room> rooms = loadRooms(roomsDir);

            // 5. Create timeslots
            List<TimeSlot> timeSlots = createTimeSlots();

            // 6. Create all the lessons to be scheduled
            List<Lesson> lessons = createLessons(rawData, teachers, courses, studentGroups);

            return new TimetableProblem(
                    "University Timetable",
                    timeSlots,
                    rooms,
                    new ArrayList<>(teachers.values()),
                    new ArrayList<>(courses.values()),
                    studentGroups,
                    lessons
            );

        } catch (IOException e) {
            throw new RuntimeException("Failed to load timetable data", e);
        }
    }

    private static List<RawDataRecord> loadRawData(String filePath) throws IOException {
        List<RawDataRecord> rawData = new ArrayList<>();
        Set<Integer> validSemesters = Set.of(3, 5, 7);
        try (Reader reader = new FileReader(filePath);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            for (CSVRecord record : csvParser) {
                if (validSemesters.contains(parseIntSafely(record.get("semester"), 0))) {
                     if (record.get("teacher_id") != null && !record.get("teacher_id").isEmpty() && !"Unknown".equalsIgnoreCase(record.get("teacher_id"))) {
                        rawData.add(new RawDataRecord(record));
                    }
                }
            }
        }
        return rawData;
    }
    
    private static Map<String, Teacher> createTeachers(List<RawDataRecord> rawData) {
        Map<String, Teacher> teachers = new HashMap<>();
        for (RawDataRecord record : rawData) {
            teachers.computeIfAbsent(record.teacherId, id -> {
                String name = (record.firstName + " " + record.lastName).trim();
                return new Teacher(id, name, record.teacherEmail, 21); // Max 21 hours per week
            });
        }
        return teachers;
    }

    private static Map<String, Course> createCourses(List<RawDataRecord> rawData) {
        Map<String, Course> courses = new HashMap<>();
        for (RawDataRecord record : rawData) {
            courses.computeIfAbsent(record.courseId, id -> {
                String deptCode = DEPT_NAME_TO_CODE.getOrDefault(record.courseDept, record.courseDept);
                return new Course(id, record.courseCode, record.courseName, deptCode,
                        record.lectureHours, record.tutorialHours, record.practicalHours, record.credits);
            });
        }
        return courses;
    }

    private static List<StudentGroup> createStudentGroups(List<RawDataRecord> rawData) {
        List<StudentGroup> studentGroups = new ArrayList<>();
        int groupCounter = 1;
        
        // Map semester to year (e.g., 3rd semester is 2nd year)
        Map<Integer, Integer> semesterToYear = Map.of(3, 2, 5, 3, 7, 4);

        // Find all unique Department-Year combinations that need groups
        Set<Map.Entry<String, Integer>> deptYearPairs = rawData.stream()
            .map(r -> new AbstractMap.SimpleEntry<>(
                DEPT_NAME_TO_CODE.getOrDefault(r.courseDept, r.courseDept),
                semesterToYear.get(r.semester)
            ))
            .collect(Collectors.toSet());

        // Create groups for each combination based on config
        for (Map.Entry<String, Integer> pair : deptYearPairs) {
            String dept = pair.getKey();
            int year = pair.getValue();
            int numSections = DEPARTMENT_DATA.getOrDefault(dept, Collections.emptyMap())
                                              .getOrDefault(String.valueOf(year), 0);

            for (int i = 0; i < numSections; i++) {
                char section = (char) ('A' + i);
                String groupName = String.format("%s-%d%c", dept, year, section);
                studentGroups.add(new StudentGroup(
                    String.valueOf(groupCounter++), 
                    groupName, 
                    CLASS_STRENGTH, 
                    dept, 
                    year
                ));
            }
        }
        return studentGroups;
    }
    
    private static List<Lesson> createLessons(List<RawDataRecord> rawData, Map<String, Teacher> teachers, Map<String, Course> courses, List<StudentGroup> studentGroups) {
        List<Lesson> lessons = new ArrayList<>();
        long lessonIdCounter = 1;

        // Map courses to the teachers who can teach them
        Map<String, List<String>> courseToTeacherIds = new HashMap<>();
        for (RawDataRecord record : rawData) {
            courseToTeacherIds.computeIfAbsent(record.courseId, k -> new ArrayList<>()).add(record.teacherId);
        }
        
        // Map (dept, year) to courses for that group
        Map<Integer, Integer> semesterToYear = Map.of(3, 2, 5, 3, 7, 4);
        Map<Map.Entry<String, Integer>, Set<String>> groupToCourseIds = new HashMap<>();
        for (RawDataRecord r : rawData) {
            Map.Entry<String, Integer> key = new AbstractMap.SimpleEntry<>(
                DEPT_NAME_TO_CODE.getOrDefault(r.courseDept, r.courseDept),
                semesterToYear.get(r.semester)
            );
            groupToCourseIds.computeIfAbsent(key, k -> new HashSet<>()).add(r.courseId);
        }

        // --- Generate lessons for each student group ---
        for (StudentGroup group : studentGroups) {
            Set<String> relevantCourseIds = groupToCourseIds.getOrDefault(
                new AbstractMap.SimpleEntry<>(group.getDepartment(), group.getYear()), 
                Collections.emptySet()
            );

            for (String courseId : relevantCourseIds) {
                Course course = courses.get(courseId);
                List<String> availableTeacherIds = courseToTeacherIds.getOrDefault(courseId, Collections.emptyList());
                if (course == null || availableTeacherIds.isEmpty()) continue;

                // Assign a teacher (simple round-robin based on group ID to be deterministic)
                int groupNumericId = Integer.parseInt(group.getId());
                Teacher teacher = teachers.get(availableTeacherIds.get((groupNumericId - 1) % availableTeacherIds.size()));
                
                // Create Lecture lessons
                for (int i = 0; i < course.getLectureHours(); i++) {
                    lessons.add(new Lesson("L-" + lessonIdCounter++, teacher, course, group, "lecture", null));
                }

                // Create Tutorial lessons
                for (int i = 0; i < course.getTutorialHours(); i++) {
                    lessons.add(new Lesson("L-" + lessonIdCounter++, teacher, course, group, "tutorial", null));
                }

                // Create Lab lessons
                if (course.getPracticalHours() > 0) {
                    if (course.getPracticalHours() == 6) {
                        // Special case: 3 sessions for the whole class in a large lab
                        int sessions = course.getPracticalHours() / 2;
                        for (int i = 0; i < sessions; i++) {
                           lessons.add(new Lesson("L-" + lessonIdCounter++, teacher, course, group, "lab", null));
                        }
                    } else {
                        // Standard case: split into 2 batches
                        int sessions = course.getPracticalHours() / 2;
                        for (int i = 0; i < sessions; i++) { // Sessions for Batch 1
                            lessons.add(new Lesson("L-" + lessonIdCounter++, teacher, course, group, "lab", "B1"));
                        }
                        for (int i = 0; i < sessions; i++) { // Sessions for Batch 2
                            lessons.add(new Lesson("L-" + lessonIdCounter++, teacher, course, group, "lab", "B2"));
                        }
                    }
                }
            }
        }
        return lessons;
    }
    
    private static List<TimeSlot> createTimeSlots() {
        List<TimeSlot> timeSlots = new ArrayList<>();
        int id = 1;
        DayOfWeek[] days = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY};

        // Theory slots (50 mins) based on your requirements
        // 8:10-9:00, 9:00-9:50, [BREAK], 10:10-11:00, 11:00-11:50, [LUNCH], 12:40-13:30, etc.
        LocalTime[] theoryStartTimes = {
            LocalTime.of(8, 10), LocalTime.of(9, 0), LocalTime.of(10, 10),
            LocalTime.of(11, 0), LocalTime.of(12, 40), LocalTime.of(13, 30),
            LocalTime.of(14, 20)
        };
        for (DayOfWeek day : days) {
            for (LocalTime startTime : theoryStartTimes) {
                LocalTime endTime = startTime.plusMinutes(50);
                timeSlots.add(new TimeSlot("TS" + id++, day, startTime, endTime, false));
            }
        }
        
        // Lab slots (1h 40m, i.e., two 50-min periods)
        LocalTime[] labStartTimes = {
            LocalTime.of(8, 10),  // 8:10 - 9:50
            LocalTime.of(10, 10), // 10:10 - 11:50
            LocalTime.of(12, 40), // 12:40 - 14:20
            LocalTime.of(14, 20)  // 14:20 - 16:00
        };
        for (DayOfWeek day : days) {
            for (LocalTime startTime : labStartTimes) {
                LocalTime endTime = startTime.plusMinutes(100);
                timeSlots.add(new TimeSlot("TS_LAB" + id++, day, startTime, endTime, true));
            }
        }
        return timeSlots;
    }

    private static List<Room> loadRooms(String roomsDir) throws IOException {
        // This method seems okay, but ensure your CSVs have headers. 
        // For robustness, I'm keeping your existing room loading logic.
        // --- Your loadRooms and loadRoomsFromFile methods from the original file go here ---
        // (No changes needed to these specific methods)
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
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase(true).withTrim())) {
            for (CSVRecord record : csvParser) {
                try {
                    String id = record.get("id");
                    String name = record.get("room_number");
                    String block = record.get("block");
                    // Create a unique ID by combining block and room number
                    String uniqueId = block.replaceAll("\\s+", "") + "_" + name;
                    int capacity = parseIntSafely(record.get("room_max_cap"), 70);
                    rooms.add(new Room(uniqueId, name, block, "", capacity, isLab));
                } catch (IllegalArgumentException e) {
                     System.err.println("Skipping record in " + filePath + " due to missing fields: " + e.getMessage());
                }
            }
        }
        return rooms;
    }

    private static int parseIntSafely(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return (int) Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}