package org.timetable.generator;

import org.timetable.domain.*;
import org.timetable.util.*;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * OptaPlanner-based CS Department Timetable Generator with correct course-teacher mappings
 */
public class OptaPlannerCSETimetableGenerator {
    
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
    private static Map<String, List<DataLoader.TeacherInfo>> realTeachers;
    private static List<DataLoader.RoomInfo> realClassrooms;
    private static List<DataLoader.RoomInfo> realLabs;
    private static Map<String, List<DataLoader.CourseInfo>> realCourses;
    private static Map<String, List<DataLoader.CourseTeacherMapping>> courseTeacherMappings;
    
    private static long lessonIdCounter = 1L;
    private static int timeSlotIdCounter = 1;
    
    public static void main(String[] args) {
        System.out.println("Starting OptaPlanner CS Department Timetable Generation...");
        
        // Load real data
        loadRealData();
        
        // Generate timetables for all CS departments
        generateCSETimetables();
        
        System.out.println("OptaPlanner CS timetable generation completed!");
    }
    
    private static void loadRealData() {
        System.out.println("Loading real institutional data...");
        
        // Load classrooms
        realClassrooms = DataLoader.loadClassrooms();
        System.out.println("Loaded " + realClassrooms.size() + " classrooms");
        
        // Load labs
        realLabs = DataLoader.loadLabs();
        System.out.println("Loaded " + realLabs.size() + " labs");
        
        // Load courses
        realCourses = DataLoader.loadCourses("data/courses/cse_dept_red.csv");
        System.out.println("Loaded " + realCourses.values().stream().mapToInt(List::size).sum() + " courses");
        
        // Load course-teacher mappings
        courseTeacherMappings = DataLoader.loadCourseTeacherMappings("data/courses/cse_dept_red.csv");
        System.out.println("Loaded " + courseTeacherMappings.values().stream().mapToInt(List::size).sum() + " course-teacher mappings");
        
        // Create teachers from course-teacher mappings
        realTeachers = createTeachersFromMappings();
        System.out.println("Created " + realTeachers.values().stream().mapToInt(List::size).sum() + " teachers from mappings");
    }
    
    private static Map<String, List<DataLoader.TeacherInfo>> createTeachersFromMappings() {
        Map<String, List<DataLoader.TeacherInfo>> teachersByDept = new HashMap<>();
        Set<String> seenTeachers = new HashSet<>();
        
        for (String deptCode : courseTeacherMappings.keySet()) {
            List<DataLoader.TeacherInfo> teachers = new ArrayList<>();
            
            for (DataLoader.CourseTeacherMapping mapping : courseTeacherMappings.get(deptCode)) {
                String teacherKey = deptCode + "_" + mapping.teacherId;
                if (!seenTeachers.contains(teacherKey)) {
                    // Parse teacher name to extract parts
                    String[] nameParts = mapping.teacherName.split(" - ");
                    String staffCode = nameParts.length > 0 ? nameParts[0] : mapping.teacherId;
                    String fullName = nameParts.length > 1 ? nameParts[1] : mapping.teacherName;
                    String[] nameComponents = fullName.split(" ");
                    String firstName = nameComponents.length > 0 ? nameComponents[0] : fullName;
                    String lastName = nameComponents.length > 1 ? 
                        String.join(" ", Arrays.copyOfRange(nameComponents, 1, nameComponents.length)) : "";
                    
                    DataLoader.TeacherInfo teacher = new DataLoader.TeacherInfo(
                        mapping.teacherId, staffCode, firstName, lastName, "", mapping.department);
                    teachers.add(teacher);
                    seenTeachers.add(teacherKey);
                }
            }
            
            teachersByDept.put(deptCode, teachers);
        }
        
        return teachersByDept;
    }
    
    private static void generateCSETimetables() {
        System.out.println("Generating CS department timetables...");
        
        // Generate student groups
        List<StudentGroup> studentGroups = generateCSStudentGroups();
        System.out.println("Generated " + studentGroups.size() + " student groups");
        
        // Generate teachers from real data
        Map<String, List<Teacher>> teachersByDept = generateCSTeachersFromRealData();
        System.out.println("Generated " + teachersByDept.values().stream().mapToInt(List::size).sum() + " teachers");
        
        // Generate courses from real data
        Map<String, List<Course>> coursesByDept = generateCSCoursesFromRealData();
        System.out.println("Generated " + coursesByDept.values().stream().mapToInt(List::size).sum() + " courses");
        
        // Generate rooms
        List<Room> rooms = generateRooms();
        System.out.println("Generated " + rooms.size() + " rooms");
        
        // Generate time slots
        List<TimeSlot> timeSlots = generateTimeSlots();
        System.out.println("Generated " + timeSlots.size() + " time slots");
        
        // Generate timetables for each department
        for (String deptCode : CS_DEPARTMENT_DATA.keySet()) {
            System.out.println("\nGenerating timetables for department: " + deptCode);
            
            List<StudentGroup> deptGroups = studentGroups.stream()
                .filter(g -> g.getDepartment().getCode().equals(deptCode))
                .collect(Collectors.toList());
            
            List<Teacher> deptTeachers = teachersByDept.getOrDefault(deptCode, new ArrayList<>());
            List<Course> deptCourses = coursesByDept.getOrDefault(deptCode, new ArrayList<>());
            
            if (!deptGroups.isEmpty() && !deptTeachers.isEmpty() && !deptCourses.isEmpty()) {
                generateDepartmentTimetable(deptCode, deptGroups, deptTeachers, deptCourses, rooms, timeSlots);
            }
        }
    }
    
    private static void generateDepartmentTimetable(String deptCode, List<StudentGroup> studentGroups,
                                                  List<Teacher> teachers, List<Course> courses,
                                                  List<Room> rooms, List<TimeSlot> timeSlots) {
        
        // Create lessons with correct teacher assignments and proper frequency
        List<Lesson> lessons = createLessonsWithCorrectTeachers(deptCode, studentGroups, teachers, courses);
        
        // Create the timetable problem
        TimetableProblem problem = new TimetableProblem();
        problem.setStudentGroups(studentGroups);
        problem.setTeachers(teachers);
        problem.setCourses(courses);
        problem.setRooms(rooms);
        problem.setTimeSlots(timeSlots);
        problem.setLessons(lessons);
        
        // Solve using OptaPlanner with improved configuration
        org.optaplanner.core.config.solver.SolverConfig solverConfig = new org.optaplanner.core.config.solver.SolverConfig()
            .withSolutionClass(TimetableProblem.class)
            .withEntityClasses(Lesson.class)
            .withScoreDirectorFactory(new org.optaplanner.core.config.score.director.ScoreDirectorFactoryConfig()
                .withEasyScoreCalculatorClass(SimpleTimetableScoreCalculator.class))
            .withTerminationConfig(new org.optaplanner.core.config.solver.termination.TerminationConfig()
                .withSecondsSpentLimit(lessons.size() < 50 ? 60L : 120L)); // More time for smaller problems
        
        SolverFactory<TimetableProblem> solverFactory = SolverFactory.create(solverConfig);
        Solver<TimetableProblem> solver = solverFactory.buildSolver();
        
        System.out.println("Solving timetable for " + deptCode + " with " + lessons.size() + " lessons...");
        TimetableProblem solution = solver.solve(problem);
        
        // Fallback: Assign any unassigned lessons to available slots
        assignUnassignedLessons(solution, rooms, timeSlots);
        
        System.out.println("Final score: " + solution.getScore());
        
        // Export timetables
        exportTimetables(solution, deptCode);
    }
    
    private static List<Lesson> createLessonsWithCorrectTeachers(String deptCode, List<StudentGroup> studentGroups,
                                                               List<Teacher> teachers, List<Course> courses) {
        List<Lesson> lessons = new ArrayList<>();
        List<DataLoader.CourseTeacherMapping> mappings = courseTeacherMappings.getOrDefault(deptCode, new ArrayList<>());
        
        for (StudentGroup group : studentGroups) {
            // Get courses appropriate for this student group's year
            List<Course> appropriateCourses = getCoursesForStudentGroup(group, courses, mappings);
            
            for (Course course : appropriateCourses) {
                // Find the correct teacher for this course
                Teacher assignedTeacher = findCorrectTeacherForCourse(course, teachers, mappings);
                
                if (assignedTeacher != null) {
                    // Get LTP hours from mapping
                    DataLoader.CourseTeacherMapping mapping = findMappingForCourse(course.getId(), mappings);
                    
                    if (mapping != null) {
                        // Create lecture lessons
                        for (int i = 0; i < mapping.lectureHours; i++) {
                            Lesson lesson = new Lesson();
                            lesson.setId(lessonIdCounter++);
                            lesson.setStudentGroup(group);
                            lesson.setCourse(course);
                            lesson.setTeacher(assignedTeacher);
                            lesson.setLessonType("theory"); // Lecture = theory
                            lesson.setDuration(1); // 1 hour for theory
                            
                            lessons.add(lesson);
                        }
                        
                        // Create tutorial lessons
                        for (int i = 0; i < mapping.tutorialHours; i++) {
                            Lesson lesson = new Lesson();
                            lesson.setId(lessonIdCounter++);
                            lesson.setStudentGroup(group);
                            lesson.setCourse(course);
                            lesson.setTeacher(assignedTeacher);
                            lesson.setLessonType("tutorial");
                            lesson.setDuration(1); // 1 hour for tutorial
                            
                            lessons.add(lesson);
                        }
                        
                        // Create practical lessons (labs)
                        for (int i = 0; i < mapping.practicalHours; i++) {
                            Lesson lesson = new Lesson();
                            lesson.setId(lessonIdCounter++);
                            lesson.setStudentGroup(group);
                            lesson.setCourse(course);
                            lesson.setTeacher(assignedTeacher);
                            lesson.setLessonType("lab"); // Practical = lab
                            lesson.setDuration(1); // 1 hour per practical session
                            
                            lessons.add(lesson);
                        }
                    } else {
                        // Fallback: create lessons based on course type if no mapping found
                        int lessonsPerWeek = getLessonsPerWeek(course);
                        
                        for (int i = 0; i < lessonsPerWeek; i++) {
                            Lesson lesson = new Lesson();
                            lesson.setId(lessonIdCounter++);
                            lesson.setStudentGroup(group);
                            lesson.setCourse(course);
                            lesson.setTeacher(assignedTeacher);
                            lesson.setLessonType(course.getCourseType());
                            lesson.setDuration(course.getCourseType().equals("lab") ? 2 : 1);
                            
                            lessons.add(lesson);
                        }
                    }
                }
            }
        }
        
        return lessons;
    }
    
    /**
     * Find the course-teacher mapping for a specific course
     */
    private static DataLoader.CourseTeacherMapping findMappingForCourse(String courseId, List<DataLoader.CourseTeacherMapping> mappings) {
        for (DataLoader.CourseTeacherMapping mapping : mappings) {
            if (mapping.courseCode.equals(courseId)) {
                return mapping;
            }
        }
        return null;
    }
    
    /**
     * Get courses that are appropriate for a specific student group based on year and semester
     */
    private static List<Course> getCoursesForStudentGroup(StudentGroup group, List<Course> allCourses, 
                                                         List<DataLoader.CourseTeacherMapping> mappings) {
        List<Course> appropriateCourses = new ArrayList<>();
        int studentYear = group.getYear();
        
        // Create a map of course codes to their year/semester info from mappings
        Map<String, DataLoader.CourseTeacherMapping> courseYearMap = new HashMap<>();
        for (DataLoader.CourseTeacherMapping mapping : mappings) {
            courseYearMap.put(mapping.courseCode, mapping);
        }
        
        for (Course course : allCourses) {
            DataLoader.CourseTeacherMapping mapping = courseYearMap.get(course.getId());
            
            if (mapping != null) {
                // Only assign courses that match the student group's year
                if (mapping.academicYear == studentYear) {
                    appropriateCourses.add(course);
                }
            } else {
                // If no mapping found, assign to all years (fallback for missing data)
                // This prevents completely empty timetables
                appropriateCourses.add(course);
            }
        }
        
        System.out.println("Student Group: " + group.getName() + " (Year " + studentYear + ") -> " + 
                          appropriateCourses.size() + " appropriate courses");
        
        return appropriateCourses;
    }
    
    private static Teacher findCorrectTeacherForCourse(Course course, List<Teacher> teachers,
                                                     List<DataLoader.CourseTeacherMapping> mappings) {
        // Find the mapping for this course
        DataLoader.CourseTeacherMapping courseMapping = null;
        for (DataLoader.CourseTeacherMapping mapping : mappings) {
            if (mapping.courseCode.equals(course.getId()) || mapping.courseName.equals(course.getName())) {
                courseMapping = mapping;
                break;
            }
        }
        
        if (courseMapping == null) {
            // Fallback to any available teacher if no mapping found
            return teachers.isEmpty() ? null : teachers.get(0);
        }
        
        // Find the teacher with matching ID or name
        for (Teacher teacher : teachers) {
            if (teacher.getId().equals(courseMapping.teacherId) || 
                teacher.getName().equals(courseMapping.teacherName)) {
                return teacher;
            }
        }
        
        // If exact match not found, try partial name match
        for (Teacher teacher : teachers) {
            if (teacher.getName().contains(courseMapping.teacherName) || 
                courseMapping.teacherName.contains(teacher.getName())) {
                return teacher;
            }
        }
        
        // Fallback to any available teacher
        return teachers.isEmpty() ? null : teachers.get(0);
    }
    
    private static int getLessonsPerWeek(Course course) {
        // Determine lessons per week based on course type - reduced for better distribution
        switch (course.getCourseType().toLowerCase()) {
            case "lab":
                return 1; // 1 lab session per week (was 2)
            case "theory":
                return 2; // 2 theory classes per week (was 3)
            case "tutorial":
                return 1; // 1 tutorial per week
            default:
                return 1; // Default to 1 lesson per week
        }
    }
    
    private static List<StudentGroup> generateCSStudentGroups() {
        List<StudentGroup> groups = new ArrayList<>();
        
        for (Map.Entry<String, Map<String, Integer>> deptEntry : CS_DEPARTMENT_DATA.entrySet()) {
            String deptCode = deptEntry.getKey();
            org.timetable.util.Department department = org.timetable.util.Department.fromCode(deptCode);
            
            for (Map.Entry<String, Integer> yearEntry : deptEntry.getValue().entrySet()) {
                String year = yearEntry.getKey();
                int sectionCount = yearEntry.getValue();
                
                for (int section = 1; section <= sectionCount; section++) {
                    String sectionName = String.valueOf((char)('A' + section - 1));
                    String groupId = deptCode + "_Y" + year + "_" + sectionName;
                    String groupName = deptCode + " Year " + year + " Section " + sectionName;
                    
                    StudentGroup group = new StudentGroup();
                    group.setId(groupId);
                    group.setName(groupName);
                    group.setSize(TimetableConstants.CLASS_STRENGTH);
                    group.setDepartment(department);
                    group.setYear(Integer.parseInt(year));
                    group.setSection(sectionName);
                    
                    groups.add(group);
                }
            }
        }
        
        return groups;
    }
    
    private static Map<String, List<Teacher>> generateCSTeachersFromRealData() {
        Map<String, List<Teacher>> teachersByDept = new HashMap<>();
        
        for (String deptCode : CS_DEPARTMENT_DATA.keySet()) {
            List<Teacher> teachers = new ArrayList<>();
            List<DataLoader.TeacherInfo> deptTeachers = realTeachers.getOrDefault(deptCode, new ArrayList<>());
            
            for (DataLoader.TeacherInfo teacherInfo : deptTeachers) {
                Teacher teacher = new Teacher();
                teacher.setId(teacherInfo.staffCode);
                teacher.setName(teacherInfo.staffCode + " - " + teacherInfo.firstName + " " + teacherInfo.lastName);
                teacher.setDepartment(deptCode);
                
                teachers.add(teacher);
            }
            
            teachersByDept.put(deptCode, teachers);
        }
        
        return teachersByDept;
    }
    
    private static Map<String, List<Course>> generateCSCoursesFromRealData() {
        Map<String, List<Course>> coursesByDept = new HashMap<>();
        
        for (String deptCode : CS_DEPARTMENT_DATA.keySet()) {
            List<Course> courses = new ArrayList<>();
            List<DataLoader.CourseInfo> deptCourses = realCourses.getOrDefault(deptCode, new ArrayList<>());
            
            for (DataLoader.CourseInfo courseInfo : deptCourses) {
                Course course = new Course(courseInfo.courseCode, courseInfo.courseName, determineCourseType(courseInfo));
                
                courses.add(course);
            }
            
            coursesByDept.put(deptCode, courses);
        }
        
        return coursesByDept;
    }
    
    private static String determineCourseType(DataLoader.CourseInfo courseInfo) {
        if (courseInfo.practicalHours > 0) {
            return "lab";
        } else if (courseInfo.tutorialHours > 0) {
            return "tutorial";
        } else {
            return "theory";
        }
    }
    
    private static List<Room> generateRooms() {
        List<Room> rooms = new ArrayList<>();
        
        // Add real classrooms
        for (DataLoader.RoomInfo roomInfo : realClassrooms) {
            Room room = new Room();
            room.setId(roomInfo.roomNumber);
            room.setName(roomInfo.getDisplayName());
            room.setCapacity(roomInfo.maxCapacity);
            
            rooms.add(room);
        }
        
        // Add real labs
        for (DataLoader.RoomInfo labInfo : realLabs) {
            Room room = new Room();
            room.setId(labInfo.roomNumber);
            room.setName(labInfo.getDisplayName());
            room.setCapacity(TimetableConstants.LAB_BATCH_SIZE);
            
            rooms.add(room);
        }
        
        return rooms;
    }
    
    private static List<TimeSlot> generateTimeSlots() {
        List<TimeSlot> timeSlots = new ArrayList<>();
        
        for (String day : TimetableConstants.DAYS) {
            int dayIndex = getDayIndex(day);
            
            // Add theory time slots
            for (String slot : TimetableConstants.THEORY_TIME_SLOTS) {
                TimeSlot timeSlot = new TimeSlot();
                timeSlot.setId(timeSlotIdCounter++);
                timeSlot.setDay(dayIndex);
                String[] timeParts = slot.split(" - ");
                timeSlot.setStartTimeStr(timeParts[0].trim());
                timeSlot.setEndTimeStr(timeParts[1].trim());
                timeSlot.setLabTimeSlot(false);
                
                timeSlots.add(timeSlot);
            }
            
            // Add lab time slots
            for (String slot : TimetableConstants.LAB_TIME_SLOTS) {
                TimeSlot timeSlot = new TimeSlot();
                timeSlot.setId(timeSlotIdCounter++);
                timeSlot.setDay(dayIndex);
                String[] timeParts = slot.split(" - ");
                timeSlot.setStartTimeStr(timeParts[0].trim());
                timeSlot.setEndTimeStr(timeParts[1].trim());
                timeSlot.setLabTimeSlot(true);
                
                timeSlots.add(timeSlot);
            }
        }
        
        return timeSlots;
    }
    
    private static int getDayIndex(String day) {
        switch (day.toLowerCase()) {
            case "monday": return 0;
            case "tuesday": return 1;
            case "wednesday": return 2;
            case "thursday": return 3;
            case "friday": return 4;
            case "saturday": return 5;
            case "sunday": return 6;
            default: return 0;
        }
    }
    
    private static void exportTimetables(TimetableProblem solution, String deptCode) {
        System.out.println("Exporting timetable results for " + deptCode);
        
        // Export student timetables
        for (StudentGroup group : solution.getStudentGroups()) {
            Map<String, Map<String, String>> timetable = createStudentTimetable(group, solution.getLessons());
            TimetableExporter.SimpleStudentGroup simpleGroup = new TimetableExporter.SimpleStudentGroup(
                group.getId(), group.getName(), group.getDepartment().getCode());
            TimetableExporter.exportStudentTimetable(simpleGroup, timetable);
        }
        
        // Export teacher timetables
        for (Teacher teacher : solution.getTeachers()) {
            Map<String, Map<String, String>> timetable = createTeacherTimetable(teacher, solution.getLessons());
            TimetableExporter.SimpleTeacher simpleTeacher = new TimetableExporter.SimpleTeacher(
                teacher.getId(), teacher.getName());
            TimetableExporter.exportTeacherTimetable(simpleTeacher, timetable);
        }
        
        System.out.println("Completed export for " + deptCode);
    }
    
    private static Map<String, Map<String, String>> createStudentTimetable(StudentGroup group, List<Lesson> lessons) {
        Map<String, Map<String, String>> timetable = new HashMap<>();
        
        // Initialize empty timetable
        for (String day : TimetableConstants.DAYS) {
            Map<String, String> daySchedule = new HashMap<>();
            for (String timeSlot : TimetableConstants.THEORY_TIME_SLOTS) {
                daySchedule.put(timeSlot, "");
            }
            for (String timeSlot : TimetableConstants.LAB_TIME_SLOTS) {
                daySchedule.put(timeSlot, "");
            }
            timetable.put(day, daySchedule);
        }
        
        // Fill in scheduled lessons
        for (Lesson lesson : lessons) {
            if (lesson.getStudentGroup().getId().equals(group.getId()) && 
                lesson.getTimeSlot() != null && lesson.getRoom() != null) {
                
                String day = lesson.getTimeSlot().getDayString();
                String timeSlot = lesson.getTimeSlot().getTimeString();
                
                String courseInfo = lesson.getCourse().getName() + "<br>" +
                                   lesson.getTeacher().getName() + "<br>" +
                                   "<b>Room: " + lesson.getRoom().getName() + "</b><br>" +
                                   "<!-- " + lesson.getLessonType() + " -->";
                
                // Add lesson type indicator
                String lessonTypeIndicator = "";
                switch (lesson.getLessonType().toLowerCase()) {
                    case "theory":
                        lessonTypeIndicator = " (L)";
                        break;
                    case "tutorial":
                        lessonTypeIndicator = " (T)";
                        break;
                    case "lab":
                        lessonTypeIndicator = " (P)";
                        break;
                }
                
                courseInfo = lesson.getCourse().getName() + lessonTypeIndicator + "<br>" +
                           lesson.getTeacher().getName() + "<br>" +
                           "<b>Room: " + lesson.getRoom().getName() + "</b><br>" +
                           "<!-- " + lesson.getLessonType() + " -->";
                
                timetable.get(day).put(timeSlot, courseInfo);
            }
        }
        
        return timetable;
    }
    
    private static Map<String, Map<String, String>> createTeacherTimetable(Teacher teacher, List<Lesson> lessons) {
        Map<String, Map<String, String>> timetable = new HashMap<>();
        
        // Initialize empty timetable
        for (String day : TimetableConstants.DAYS) {
            Map<String, String> daySchedule = new HashMap<>();
            for (String timeSlot : TimetableConstants.THEORY_TIME_SLOTS) {
                daySchedule.put(timeSlot, "");
            }
            for (String timeSlot : TimetableConstants.LAB_TIME_SLOTS) {
                daySchedule.put(timeSlot, "");
            }
            timetable.put(day, daySchedule);
        }
        
        // Fill in scheduled lessons
        for (Lesson lesson : lessons) {
            if (lesson.getTeacher().getId().equals(teacher.getId()) && 
                lesson.getTimeSlot() != null && lesson.getRoom() != null) {
                
                String day = lesson.getTimeSlot().getDayString();
                String timeSlot = lesson.getTimeSlot().getTimeString();
                
                // Add lesson type indicator
                String lessonTypeIndicator = "";
                switch (lesson.getLessonType().toLowerCase()) {
                    case "theory":
                        lessonTypeIndicator = " (L)";
                        break;
                    case "tutorial":
                        lessonTypeIndicator = " (T)";
                        break;
                    case "lab":
                        lessonTypeIndicator = " (P)";
                        break;
                }
                
                String courseInfo = lesson.getCourse().getName() + lessonTypeIndicator + "<br>" +
                                  lesson.getStudentGroup().getName() + "<br>" +
                                  "<b>Room: " + lesson.getRoom().getName() + "</b><br>" +
                                  "<!-- " + lesson.getLessonType() + " -->";
                
                timetable.get(day).put(timeSlot, courseInfo);
            }
        }
        
        return timetable;
    }
    
    /**
     * Fallback method to assign any unassigned lessons to available slots
     */
    private static void assignUnassignedLessons(TimetableProblem solution, List<Room> rooms, List<TimeSlot> timeSlots) {
        List<Lesson> unassignedLessons = new ArrayList<>();
        
        // Find unassigned lessons
        for (Lesson lesson : solution.getLessons()) {
            if (lesson.getRoom() == null || lesson.getTimeSlot() == null) {
                unassignedLessons.add(lesson);
            }
        }
        
        if (unassignedLessons.isEmpty()) {
            System.out.println("All lessons successfully assigned!");
            return;
        }
        
        System.out.println("Assigning " + unassignedLessons.size() + " unassigned lessons using fallback method...");
        
        // Simple assignment: try to assign to any available slot
        for (Lesson lesson : unassignedLessons) {
            boolean assigned = false;
            
            // Try each time slot
            for (TimeSlot timeSlot : timeSlots) {
                if (assigned) break;
                
                // Check if this time slot is available for the student group and teacher
                boolean slotAvailable = true;
                
                for (Lesson otherLesson : solution.getLessons()) {
                    if (otherLesson == lesson) continue;
                    if (otherLesson.getTimeSlot() == null) continue;
                    
                    if (otherLesson.getTimeSlot().equals(timeSlot)) {
                        // Check for conflicts
                        if ((lesson.getStudentGroup() != null && lesson.getStudentGroup().equals(otherLesson.getStudentGroup())) ||
                            (lesson.getTeacher() != null && lesson.getTeacher().equals(otherLesson.getTeacher()))) {
                            slotAvailable = false;
                            break;
                        }
                    }
                }
                
                if (slotAvailable) {
                    // Find an appropriate room
                    for (Room room : rooms) {
                        boolean roomAvailable = true;
                        
                        // Check if room is available at this time
                        for (Lesson otherLesson : solution.getLessons()) {
                            if (otherLesson == lesson) continue;
                            if (otherLesson.getTimeSlot() == null || otherLesson.getRoom() == null) continue;
                            
                            if (otherLesson.getTimeSlot().equals(timeSlot) && otherLesson.getRoom().equals(room)) {
                                roomAvailable = false;
                                break;
                            }
                        }
                        
                        if (roomAvailable) {
                            // Assign the lesson
                            lesson.setTimeSlot(timeSlot);
                            lesson.setRoom(room);
                            assigned = true;
                            break;
                        }
                    }
                }
            }
            
            if (!assigned) {
                System.out.println("Warning: Could not assign lesson: " + lesson.getCourse().getName() + 
                                 " for " + lesson.getStudentGroup().getName());
            }
        }
    }
}