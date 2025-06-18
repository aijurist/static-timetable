package org.timetable.persistence;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.timetable.domain.*;
import org.timetable.config.TimetableConfig;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class TimetableDataLoader {
    private static final Logger LOGGER = Logger.getLogger(TimetableDataLoader.class.getName());

    private static final Map<String, String> DEPT_NAME_TO_CODE = Map.ofEntries(
            Map.entry("Computer Science & Design", "CSD"),
            Map.entry("Computer Science & Engineering", "CSE"),
            Map.entry("Computer Science & Engineering (Cyber Security)", "CSE-CS"),
            Map.entry("Computer Science & Business Systems", "CSBS"),
            Map.entry("Information Technology", "IT"),
            Map.entry("Artificial Intelligence & Machine Learning", "AIML"),
            Map.entry("AI & Data Science", "AIDS"),
            Map.entry("Electronics & Communication Engineering", "ECE"),
            Map.entry("Electrical & Electronics Engineering", "EEE"),
            Map.entry("Aeronautical Engineering", "AERO"),
            Map.entry("Automobile Engineering", "AUTO"),
            Map.entry("Mechatronics Engineering", "MCT"),
            Map.entry("Mechanical Engineering", "MECH"),
            Map.entry("Biotechnology", "BT"),
            Map.entry("Biomedical Engineering", "BME"),
            Map.entry("Robotics & Automation", "R&A"),
            Map.entry("Food Technology", "FT"),
            Map.entry("Civil Engineering", "CIVIL"),
            Map.entry("Chemical Engineering", "CHEM")
    );

    private static final Map<String, Map<String, Integer>> DEPARTMENT_DATA = Map.ofEntries(
            Map.entry("CSE-CS", Map.of("2", 2, "3", 1)),
            Map.entry("CSE", Map.of("2", 10, "3", 6, "4", 5)),
            Map.entry("CSBS", Map.of("2", 2, "3", 2, "4", 2)),
            Map.entry("CSD", Map.of("2", 1, "3", 1, "4", 1)),
            Map.entry("IT", Map.of("2", 5, "3", 4, "4", 3)),
            Map.entry("AIML", Map.of("2", 4, "3", 3, "4", 3)),
            Map.entry("AIDS", Map.of("2", 5, "3", 3, "4", 1)),
            Map.entry("ECE", Map.of("2", 6, "3", 4, "4", 4)),
            Map.entry("EEE", Map.of("2", 2, "3", 2, "4", 2)),
            Map.entry("AERO", Map.of("2", 1, "3", 1, "4", 1)),
            Map.entry("AUTO", Map.of("2", 1, "3", 1, "4", 1)),
            Map.entry("MCT", Map.of("2", 1, "3", 1, "4", 1)),
            Map.entry("MECH", Map.of("2", 2, "3", 2, "4", 3)),
            Map.entry("BT", Map.of("2", 3, "3", 3, "4", 3)),
            Map.entry("BME", Map.of("2", 2, "3", 2, "4", 2)),
            Map.entry("R&A", Map.of("2", 1, "3", 1, "4", 1)),
            Map.entry("FT", Map.of("2", 1, "3", 1, "4", 1)),
            Map.entry("CIVIL", Map.of("2", 1, "3", 1, "4", 1)),
            Map.entry("CHEM", Map.of("2", 1, "3", 1, "4", 1))
    );

    private static class RawDataRecord {
        final String courseId, courseCode, courseName, courseDept, courseType, teacherId, staffCode, firstName, lastName, teacherEmail, labType;
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
            
            // Read lab_type field if available
            String labTypeValue = null;
            try {
                labTypeValue = record.get("lab_type");
                if (labTypeValue != null && labTypeValue.trim().isEmpty()) {
                    labTypeValue = null;
                }
            } catch (IllegalArgumentException e) {
                // lab_type column doesn't exist, that's fine
                labTypeValue = null;
            }
            this.labType = labTypeValue;
        }
    }

    static {
        try {
            FileHandler fh = new FileHandler("timetable_loader.log");
            fh.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fh);
            LOGGER.setLevel(Level.INFO);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static TimetableProblem loadProblem(String coursesFile, String roomsDir) {
        try {
            List<RawDataRecord> rawData;
            if (coursesFile.contains(",")) {
                // Multiple files separated by comma
                String[] files = coursesFile.split(",");
                rawData = new ArrayList<>();
                for (String file : files) {
                    rawData.addAll(loadRawData(file.trim()));
                }
            } else {
                rawData = loadRawData(coursesFile);
            }
            
                
            
            Map<String, Teacher> teachers = createTeachers(rawData);
            Map<String, Course> courses = createCourses(rawData);
            List<StudentGroup> studentGroups = createStudentGroups(rawData);
            List<Room> rooms = loadRooms(roomsDir);
            List<TimeSlot> timeSlots = createTimeSlots();
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
        LOGGER.info("Loading raw data from: " + filePath);

        try (Reader reader = new FileReader(filePath);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            int totalRecords = 0;
            int validRecords = 0;

            for (CSVRecord record : csvParser) {
                totalRecords++;
                if (validSemesters.contains(parseIntSafely(record.get("semester"), 0))) {
                    if (record.get("teacher_id") != null && !record.get("teacher_id").isEmpty() && !"Unknown".equalsIgnoreCase(record.get("teacher_id"))) {
                        rawData.add(new RawDataRecord(record));
                        validRecords++;
                    } else {
                        LOGGER.warning("Skipping record due to missing/invalid teacher_id: " + record);
                    }
                }
            }
            LOGGER.info(String.format("Data loading summary: %d total records, %d valid records", totalRecords, validRecords));
        }
        return rawData;
    }

    private static Map<String, Teacher> createTeachers(List<RawDataRecord> rawData) {
        Map<String, Teacher> teachers = new HashMap<>();
        LOGGER.info("Creating teacher records...");

        for (RawDataRecord record : rawData) {
            teachers.computeIfAbsent(record.teacherId, id -> {
                String name = (record.firstName + " " + record.lastName).trim();
                LOGGER.info("Creating teacher: " + name + " (ID: " + id + ")");
                return new Teacher(id, name, record.teacherEmail, TimetableConfig.MAX_TEACHER_HOURS);
            });
        }
        LOGGER.info("Created " + teachers.size() + " unique teachers");
        return teachers;
    }

    private static Map<String, Course> createCourses(List<RawDataRecord> rawData) {
        Map<String, Course> courses = new HashMap<>();
        LOGGER.info("Creating course records...");

        for (RawDataRecord record : rawData) {
            courses.computeIfAbsent(record.courseId, id -> {
                String deptCode = DEPT_NAME_TO_CODE.getOrDefault(record.courseDept, record.courseDept);
                CourseType type = record.practicalHours > 0 ? CourseType.LAB : CourseType.THEORY;
                LOGGER.info(String.format("Creating course: %s - %s (Dept: %s, Semester: %d, Lab Type: %s)",
                        record.courseCode, record.courseName, deptCode, record.semester, record.labType));
                return new Course(id, record.courseCode, record.courseName, deptCode, type,
                        record.lectureHours, record.tutorialHours, record.practicalHours, record.credits, record.labType);
            });
        }
        LOGGER.info("Created " + courses.size() + " unique courses");
        return courses;
    }

    private static List<StudentGroup> createStudentGroups(List<RawDataRecord> rawData) {
        List<StudentGroup> studentGroups = new ArrayList<>();
        int groupCounter = 1;
        LOGGER.info("Creating student groups...");

        Map<Integer, Integer> semesterToYear = Map.of(3, 2, 5, 3, 7, 4);

        Set<Map.Entry<String, Integer>> deptYearPairs = rawData.stream()
                .map(r -> new AbstractMap.SimpleEntry<>(
                        DEPT_NAME_TO_CODE.getOrDefault(r.courseDept, r.courseDept),
                        semesterToYear.get(r.semester)
                ))
                .collect(Collectors.toSet());

        for (Map.Entry<String, Integer> entry : deptYearPairs) {
            String dept = entry.getKey();
            Integer year = entry.getValue();
            
            // Map full department name to short code
            String deptCode = DEPT_NAME_TO_CODE.getOrDefault(dept, dept);
            
            Map<String, Integer> yearToSections = DEPARTMENT_DATA.get(deptCode);
            if (yearToSections == null) {
                LOGGER.info("Creating 0 sections for " + dept + " Year " + year + " (mapped to " + deptCode + ")");
                continue;
            }
            
            Integer sections = yearToSections.get(year.toString());
            if (sections == null || sections == 0) {
                LOGGER.info("Creating 0 sections for " + dept + " Year " + year + " (mapped to " + deptCode + ")");
                continue;
            }
            
            LOGGER.info("Creating " + sections + " sections for " + dept + " Year " + year + " (mapped to " + deptCode + ")");
            
            for (int i = 1; i <= sections; i++) {
                String groupName = dept + " " + year + "." + i;
                studentGroups.add(new StudentGroup(
                        String.valueOf(groupCounter++),
                        groupName,
                        "AUTO".equals(deptCode) ? 35 : TimetableConfig.CLASS_STRENGTH,
                        dept,
                        year
                ));
            }
        }
        LOGGER.info("Created " + studentGroups.size() + " total student groups");
        return studentGroups;
    }

    private static List<Lesson> createLessons(List<RawDataRecord> rawData, Map<String, Teacher> teachers, Map<String, Course> courses, List<StudentGroup> studentGroups) {
        List<Lesson> lessons = new ArrayList<>();
        long lessonIdCounter = 1;

        LOGGER.info("Creating lessons and assigning teachers to sections...");

        // First group by course department
        Map<String, List<RawDataRecord>> deptToRecords = rawData.stream()
            .collect(Collectors.groupingBy(r -> DEPT_NAME_TO_CODE.getOrDefault(r.courseDept, r.courseDept)));

        // For each department
        for (Map.Entry<String, List<RawDataRecord>> deptEntry : deptToRecords.entrySet()) {
            String dept = deptEntry.getKey();
            List<RawDataRecord> deptRecords = deptEntry.getValue();

            // Group by semester and year
            Map<Integer, List<RawDataRecord>> semesterToRecords = deptRecords.stream()
                .collect(Collectors.groupingBy(r -> r.semester));

            // For each semester
            for (Map.Entry<Integer, List<RawDataRecord>> semesterEntry : semesterToRecords.entrySet()) {
                int semester = semesterEntry.getKey();
                List<RawDataRecord> semesterRecords = semesterEntry.getValue();
                int year = semester == 3 ? 2 : semester == 5 ? 3 : 4;

                // Get all student groups for this department and year
                List<StudentGroup> relevantGroups = studentGroups.stream()
                    .filter(g -> g.getDepartment().equals(dept) && g.getYear() == year)
                    .collect(Collectors.toList());

                // Group courses by their ID to ensure we have all courses for the semester
                Map<String, List<RawDataRecord>> courseToRecords = semesterRecords.stream()
                    .collect(Collectors.groupingBy(r -> r.courseId));

                // For each course in this semester
                for (Map.Entry<String, List<RawDataRecord>> courseEntry : courseToRecords.entrySet()) {
                    String courseId = courseEntry.getKey();
                    List<RawDataRecord> courseRecords = courseEntry.getValue();
                    Course course = courses.get(courseId);

                    if (course == null) {
                        LOGGER.warning("Skipping lesson creation for course " + courseId + " due to missing course.");
                        continue;
                    }

                    // Get all teachers who can teach this course (from this department)
                    List<String> availableTeacherIds = courseRecords.stream()
                        .map(r -> r.teacherId)
                        .distinct()
                        .collect(Collectors.toList());

                    if (availableTeacherIds.isEmpty()) {
                        LOGGER.warning("No teachers available for course " + courseId);
                        continue;
                    }

                    // For each student group in this department and year
                    for (StudentGroup group : relevantGroups) {
                        // Assign a teacher to this group's course
                        int groupIndex = Integer.parseInt(group.getId()) - 1;
                        Teacher teacher = teachers.get(availableTeacherIds.get(groupIndex % availableTeacherIds.size()));

                        // Create lecture lessons
                        for (int i = 0; i < course.getLectureHours(); i++) {
                            lessons.add(new Lesson("L-" + lessonIdCounter++, teacher, course, group, "lecture", null));
                        }

                        // Create tutorial lessons
                        for (int i = 0; i < course.getTutorialHours(); i++) {
                            lessons.add(new Lesson("L-" + lessonIdCounter++, teacher, course, group, "tutorial", null));
                        }

                        // Create lab lessons if needed
                        if (course.getPracticalHours() > 0) {
                            int labSessions = course.getPracticalHours() / 2; // Each session is 2 hours

                            // Certain project/industry-style labs must run with the FULL class in a 70-seat lab;
                            // do NOT split them even if class strength > 35.
                            final java.util.Set<String> UNBATCHED_COURSES = java.util.Set.of(
                                    "CD23321", 
                                    "CS19P23",
                                    "CS19P21"
                            );

                            boolean forceFullGroup = UNBATCHED_COURSES.contains(course.getCode());

                            boolean needsBatching = !forceFullGroup && group.getSize() > TimetableConfig.LAB_BATCH_SIZE;
                             
                            if (needsBatching) {
                                // CORRECTED FIX: Each batch gets the full practical hours allocation
                                LOGGER.info(String.format(
                                    "Creating %d lab sessions for %s (%s) - split batches B1 & B2 (each batch gets %d sessions)",
                                    labSessions * 2, course.getCode(), group.getName(), labSessions
                                ));
                                
                                // Batch B1 gets full practical hours (all required sessions)
                                for (int i = 0; i < labSessions; i++) {
                                    lessons.add(new Lesson("L-" + lessonIdCounter++, teacher, course, group, "lab", "B1"));
                                }
                                
                                // Batch B2 gets full practical hours (all required sessions)
                                for (int i = 0; i < labSessions; i++) {
                                    lessons.add(new Lesson("L-" + lessonIdCounter++, teacher, course, group, "lab", "B2"));
                                }
                            } else {
                                // If no batching needed, create sessions for whole group
                                LOGGER.info(String.format(
                                    "Creating %d lab sessions for %s (%s) - no batching",
                                    labSessions, course.getCode(), group.getName()
                                ));
                                
                                for (int i = 0; i < labSessions; i++) {
                                    lessons.add(new Lesson("L-" + lessonIdCounter++, teacher, course, group, "lab", null));
                                }
                            }
                        }
                    }
                }
            }
        }

        LOGGER.info("Created " + lessons.size() + " total lessons");
        return lessons;
    }

    private static List<TimeSlot> createTimeSlots() {
        List<TimeSlot> timeSlots = new ArrayList<>();
        int idCounter = 1;
        DayOfWeek[] days = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY};

        LOGGER.info("Creating theory and lab timeslots...");

        for (DayOfWeek day : days) {
            for (LocalTime[] slot : TimetableConfig.THEORY_TIME_SLOTS) {
                timeSlots.add(new TimeSlot("TS-" + idCounter++, day, slot[0], slot[1], false)); // isLab = false
            }
            for (LocalTime[] slot : TimetableConfig.LAB_TIME_SLOTS) {
                timeSlots.add(new TimeSlot("TS-LAB-" + idCounter++, day, slot[0], slot[1], true)); // isLab = true
            }
        }
        LOGGER.info("Created " + timeSlots.size() + " total timeslots per week.");
        return timeSlots;
    }

    public static List<Room> loadRooms(String roomsDir) throws IOException {
        List<Room> rooms = new ArrayList<>();
        File classroomDir = new File(roomsDir, "classroom");
        if (classroomDir.exists() && classroomDir.isDirectory()) {
            File[] classroomFiles = classroomDir.listFiles((dir, name) -> name.endsWith(".csv"));
            if (classroomFiles != null) {
                for (File file : classroomFiles) {
                    // Load all classroom files
                    rooms.addAll(loadRoomsFromFile(file.getPath(), false));
                }
            }
        }
        File labsDir = new File(roomsDir, "labs");
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
                    String name = record.get("room_number");
                    String block = record.get("block");
                    String description = record.get("description");
                    String uniqueId = block.replaceAll("\\s+", "") + "_" + name;
                    int capacity = parseIntSafely(record.get("room_max_cap"), 70);
                    
                    // Read lab_type if available
                    String labType = null;
                    try {
                        labType = record.get("lab_type");
                        if (labType != null && labType.trim().isEmpty()) {
                            labType = null;
                        }
                    } catch (IllegalArgumentException e) {
                        // lab_type column doesn't exist, that's fine
                        labType = null;
                    }
                    
                    rooms.add(new Room(uniqueId, name, block, description, capacity, isLab, labType));
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