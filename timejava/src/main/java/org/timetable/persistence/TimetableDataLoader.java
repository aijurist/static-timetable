package org.timetable.persistence;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.timetable.domain.*;
import org.timetable.util.DepartmentMapper;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class TimetableDataLoader {

    private static final Map<String, String> DEPARTMENT_NAMES = new HashMap<>();
    private static final Map<String, Map<String, Integer>> DEPARTMENT_SECTIONS = new HashMap<>();
    
    static {
        // Initialize department names
        DEPARTMENT_NAMES.put("CSD", "Computer Science & Design");
        DEPARTMENT_NAMES.put("CSE", "Computer Science & Engineering");
        DEPARTMENT_NAMES.put("CSE-CS", "Computer Science & Engineering (Cyber Security)");
        DEPARTMENT_NAMES.put("CSBS", "Computer Science & Business Systems");
        DEPARTMENT_NAMES.put("IT", "Information Technology");
        DEPARTMENT_NAMES.put("AIML", "Artificial Intelligence & Machine Learning");
        DEPARTMENT_NAMES.put("AIDS", "Artificial Intelligence & Data Science");
        DEPARTMENT_NAMES.put("ECE", "Electronics & Communication Engineering");
        DEPARTMENT_NAMES.put("EEE", "Electrical & Electronics Engineering");
        DEPARTMENT_NAMES.put("AERO", "Aeronautical Engineering");
        DEPARTMENT_NAMES.put("AUTO", "Automobile Engineering");
        DEPARTMENT_NAMES.put("MCT", "Mechatronics Engineering");
        DEPARTMENT_NAMES.put("MECH", "Mechanical Engineering");
        DEPARTMENT_NAMES.put("BT", "Biotechnology");
        DEPARTMENT_NAMES.put("BME", "Biomedical Engineering");
        DEPARTMENT_NAMES.put("R&A", "Robotics & Automation");
        DEPARTMENT_NAMES.put("FT", "Fashion Technology");
        DEPARTMENT_NAMES.put("CIVIL", "Civil Engineering");
        DEPARTMENT_NAMES.put("CHEM", "Chemical Engineering");
        
        // Initialize department sections
        Map<String, Integer> cseCs = new HashMap<>();
        cseCs.put("2", 2); cseCs.put("3", 1);
        DEPARTMENT_SECTIONS.put("CSE-CS", cseCs);
        
        Map<String, Integer> cse = new HashMap<>();
        cse.put("2", 10); cse.put("3", 6); cse.put("4", 5);
        DEPARTMENT_SECTIONS.put("CSE", cse);
        
        Map<String, Integer> csbs = new HashMap<>();
        csbs.put("2", 2); csbs.put("3", 2); csbs.put("4", 2);
        DEPARTMENT_SECTIONS.put("CSBS", csbs);
        
        Map<String, Integer> csd = new HashMap<>();
        csd.put("2", 1); csd.put("3", 1); csd.put("4", 1);
        DEPARTMENT_SECTIONS.put("CSD", csd);
        
        Map<String, Integer> it = new HashMap<>();
        it.put("2", 5); it.put("3", 4); it.put("4", 3);
        DEPARTMENT_SECTIONS.put("IT", it);
        
        Map<String, Integer> aiml = new HashMap<>();
        aiml.put("2", 4); aiml.put("3", 3); aiml.put("4", 3);
        DEPARTMENT_SECTIONS.put("AIML", aiml);
        
        Map<String, Integer> aids = new HashMap<>();
        aids.put("2", 5); aids.put("3", 3); aids.put("4", 1);
        DEPARTMENT_SECTIONS.put("AIDS", aids);
        
        Map<String, Integer> ece = new HashMap<>();
        ece.put("2", 6); ece.put("3", 4); ece.put("4", 4);
        DEPARTMENT_SECTIONS.put("ECE", ece);
        
        Map<String, Integer> eee = new HashMap<>();
        eee.put("2", 2); eee.put("3", 2); eee.put("4", 2);
        DEPARTMENT_SECTIONS.put("EEE", eee);
        
        Map<String, Integer> aero = new HashMap<>();
        aero.put("2", 1); aero.put("3", 1); aero.put("4", 1);
        DEPARTMENT_SECTIONS.put("AERO", aero);
        
        Map<String, Integer> auto = new HashMap<>();
        auto.put("2", 1); auto.put("3", 1); auto.put("4", 1);
        DEPARTMENT_SECTIONS.put("AUTO", auto);
        
        Map<String, Integer> mct = new HashMap<>();
        mct.put("2", 1); mct.put("3", 1); mct.put("4", 1);
        DEPARTMENT_SECTIONS.put("MCT", mct);
        
        Map<String, Integer> mech = new HashMap<>();
        mech.put("2", 2); mech.put("3", 2); mech.put("4", 2);
        DEPARTMENT_SECTIONS.put("MECH", mech);
        
        Map<String, Integer> bt = new HashMap<>();
        bt.put("2", 3); bt.put("3", 3); bt.put("4", 3);
        DEPARTMENT_SECTIONS.put("BT", bt);
        
        Map<String, Integer> bme = new HashMap<>();
        bme.put("2", 2); bme.put("3", 2); bme.put("4", 2);
        DEPARTMENT_SECTIONS.put("BME", bme);
        
        Map<String, Integer> ra = new HashMap<>();
        ra.put("2", 1); ra.put("3", 1); ra.put("4", 1);
        DEPARTMENT_SECTIONS.put("R&A", ra);
        
        Map<String, Integer> ft = new HashMap<>();
        ft.put("2", 1); ft.put("3", 1); ft.put("4", 1);
        DEPARTMENT_SECTIONS.put("FT", ft);
        
        Map<String, Integer> civil = new HashMap<>();
        civil.put("2", 1); civil.put("3", 1); civil.put("4", 1);
        DEPARTMENT_SECTIONS.put("CIVIL", civil);
        
        Map<String, Integer> chem = new HashMap<>();
        chem.put("2", 1); chem.put("3", 1); chem.put("4", 1);
        DEPARTMENT_SECTIONS.put("CHEM", chem);
    }

    public static TimetableProblem loadProblem(String coursesFile, String roomsDir) {
        try {
            // Load teachers, courses, and student groups
            Map<String, Teacher> teachers = new HashMap<>();
            Map<String, Course> courses = new HashMap<>();
            Map<String, Department> departments = createDepartments();
            Map<String, StudentGroup> studentGroups = createStudentGroups(departments);
            
            loadTeachersAndCourses(coursesFile, teachers, courses);
            
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
    
    private static Map<String, Department> createDepartments() {
        Map<String, Department> departments = new HashMap<>();
        
        for (Map.Entry<String, String> entry : DEPARTMENT_NAMES.entrySet()) {
            String code = entry.getKey();
            String name = entry.getValue();
            departments.put(code, new Department(code, name));
        }
        
        return departments;
    }
    
    private static Map<String, StudentGroup> createStudentGroups(Map<String, Department> departments) {
        Map<String, StudentGroup> studentGroups = new HashMap<>();
        
        for (Map.Entry<String, Map<String, Integer>> deptEntry : DEPARTMENT_SECTIONS.entrySet()) {
            String deptCode = deptEntry.getKey();
            Department department = departments.get(deptCode);
            
            if (department != null) {
                Map<String, Integer> yearSections = deptEntry.getValue();
                
                for (Map.Entry<String, Integer> yearEntry : yearSections.entrySet()) {
                    int year = Integer.parseInt(yearEntry.getKey());
                    int sectionCount = yearEntry.getValue();
                    
                    for (int i = 1; i <= sectionCount; i++) {
                        String section = String.valueOf((char)('A' + i - 1));
                        String id = deptCode + "-" + year + section;
                        studentGroups.put(id, new StudentGroup(id, department, year, section, 70));
                    }
                }
            }
        }
        
        return studentGroups;
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
                    String filePrefix = "CR_" + file.getName().replace(".csv", "") + "_";
                    rooms.addAll(loadRoomsFromFile(file.getPath(), false, filePrefix));
                }
            }
        }
        
        // Load labs
        File labsDir = new File(roomsDir + "/labs");
        if (labsDir.exists() && labsDir.isDirectory()) {
            File[] labFiles = labsDir.listFiles((dir, name) -> name.endsWith(".csv"));
            if (labFiles != null) {
                for (File file : labFiles) {
                    String filePrefix = "LAB_" + file.getName().replace(".csv", "") + "_";
                    rooms.addAll(loadRoomsFromFile(file.getPath(), true, filePrefix));
                }
            }
        }
        
        return rooms;
    }
    
    private static List<Room> loadRoomsFromFile(String filePath, boolean isLab, String idPrefix) throws IOException {
        List<Room> rooms = new ArrayList<>();
        Set<String> processedRoomIds = new HashSet<>();
        
        try (Reader reader = new FileReader(filePath)) {
            // Try to determine if the file has headers
            CSVParser parser = CSVParser.parse(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase(true).withTrim());
            
            for (CSVRecord record : parser) {
                try {
                    String originalId = record.get("id");
                    String uniqueId = idPrefix + originalId;
                    
                    // Skip if we've already processed a room with this ID
                    if (processedRoomIds.contains(uniqueId)) {
                        System.err.println("Skipping duplicate room ID: " + uniqueId);
                        continue;
                    }
                    
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
                    
                    Room room = new Room(uniqueId, name, block, description, capacity, roomIsLab);
                    rooms.add(room);
                    processedRoomIds.add(uniqueId);
                } catch (IllegalArgumentException e) {
                    // Skip records with missing required fields
                    System.err.println("Skipping record due to missing fields: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing CSV file " + filePath + ": " + e.getMessage());
            // Fallback to a simpler parsing approach
            try (Reader reader = new FileReader(filePath)) {
                CSVParser parser = CSVParser.parse(reader, CSVFormat.DEFAULT.withSkipHeaderRecord().withTrim());
                int recordCounter = 0;
                
                for (CSVRecord record : parser) {
                    if (record.size() >= 5) {  // Ensure we have enough fields
                        String originalId = record.get(1);  // Assuming id is the second column
                        String uniqueId = idPrefix + originalId + "_" + recordCounter++;
                        
                        // Skip if we've already processed a room with this ID
                        if (processedRoomIds.contains(uniqueId)) {
                            System.err.println("Skipping duplicate room ID: " + uniqueId);
                            continue;
                        }
                        
                        String name = record.get(2); // Assuming room_number is the third column
                        String block = record.get(3); // Assuming block is the fourth column
                        String description = record.get(4); // Assuming description is the fifth column
                        
                        // Default capacity
                        int capacity = 70;
                        
                        // Try to parse capacity if available
                        if (record.size() >= 8) {
                            capacity = parseIntSafely(record.get(7), 70); // Assuming room_max_cap is the eighth column
                        }
                        
                        Room room = new Room(uniqueId, name, block, description, capacity, isLab);
                        rooms.add(room);
                        processedRoomIds.add(uniqueId);
                    }
                }
            } catch (Exception fallbackEx) {
                System.err.println("Failed to parse CSV file even with fallback method: " + fallbackEx.getMessage());
            }
        }
        
        return rooms;
    }
    
    private static List<TimeSlot> createTimeSlots() {
        List<TimeSlot> timeSlots = new ArrayList<>();
        int id = 0;
        
        // Define days
        DayOfWeek[] days = {
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, 
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
        };
        
        // Define theory time slots
        String[][] theoryTimeSlots = {
            {"8:00", "8:50"}, {"9:00", "9:50"}, {"10:00", "10:50"},
            {"11:00", "11:50"}, {"12:00", "12:50"}, {"13:00", "13:50"},
            {"14:00", "14:50"}, {"15:00", "15:50"}, {"16:00", "16:50"},
            {"17:00", "17:50"}, {"18:00", "18:50"}
        };
        
        // Define lab time slots
        String[][] labTimeSlots = {
            {"8:00", "9:40"}, {"9:50", "11:30"}, {"11:50", "13:30"},
            {"13:50", "15:30"}, {"15:50", "17:30"}, {"17:50", "19:30"}
        };
        
        // Create theory slots
        for (DayOfWeek day : days) {
            for (String[] timeSlot : theoryTimeSlots) {
                String startTime = timeSlot[0];
                String endTime = timeSlot[1];
                timeSlots.add(new TimeSlot("TS" + id++, day, startTime, endTime, false));
            }
        }
        
        // Create lab slots
        for (DayOfWeek day : days) {
            for (String[] timeSlot : labTimeSlots) {
                String startTime = timeSlot[0];
                String endTime = timeSlot[1];
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