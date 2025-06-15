package org.timetable.generator;

import org.timetable.domain.*;
import org.timetable.util.Department;
import org.timetable.util.TimetableConstants;
import org.timetable.util.TimetableExporter;
import org.timetable.util.DataLoader;
import org.timetable.util.DataLoader.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Focused timetable generator for Computer Science departments using real data from CSV files.
 */
public class CSETimetableGenerator {
    
    // Simple domain classes for this generator
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
        private Department department;
        private int year;
        private String section;
        
        public StudentGroup(String id, String name, int studentCount, Department department) {
            this.id = id;
            this.name = name;
            this.studentCount = studentCount;
            this.department = department;
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public int getStudentCount() { return studentCount; }
        public Department getDepartment() { return department; }
        public int getYear() { return year; }
        public String getSection() { return section; }
        
        public void setYear(int year) { this.year = year; }
        public void setSection(String section) { this.section = section; }
    }
    
    private static final Random random = new Random(42);
    
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
    
    // Real data loaded from CSV files
    private static Map<String, List<TeacherInfo>> realTeachers;
    private static List<RoomInfo> realClassrooms;
    private static List<RoomInfo> realLabs;
    private static Map<String, List<CourseInfo>> realCourses;
    private static Map<String, List<DataLoader.CourseTeacherMapping>> courseTeacherMappings;
    
    public static void main(String[] args) {
        System.out.println("Starting CS Department Timetable Generation with Real Data...");
        System.out.println("Loading data from CSV files...");
        
        // Load real data from CSV files
        loadRealData();
        
        System.out.println("Generating timetables for 6 CS departments with real teacher names and room information");
        
        generateCSETimetables();
        
        System.out.println("CS Department timetable generation completed!");
    }
    
    private static void loadRealData() {
        // Load real teachers from course data
        realTeachers = DataLoader.loadTeachers("data/courses/cse_dept_red.csv");
        
        // Load real classrooms and labs
        realClassrooms = DataLoader.loadClassrooms();
        realLabs = DataLoader.loadLabs();
        
        // Load real courses
        realCourses = DataLoader.loadCourses("data/courses/cse_dept_red.csv");
        
        // Load course-teacher mappings
        courseTeacherMappings = DataLoader.loadCourseTeacherMappings("data/courses/cse_dept_red.csv");
        
        System.out.println("Loaded data:");
        System.out.println("- Teachers: " + realTeachers.values().stream().mapToInt(List::size).sum());
        System.out.println("- Classrooms: " + realClassrooms.size());
        System.out.println("- Labs: " + realLabs.size());
        System.out.println("- Courses: " + realCourses.values().stream().mapToInt(List::size).sum());
        System.out.println("- Course-Teacher Mappings: " + courseTeacherMappings.values().stream().mapToInt(List::size).sum());
    }
    
    private static void generateCSETimetables() {
        // Generate student groups for CS departments
        List<StudentGroup> csStudentGroups = generateCSStudentGroups();
        
        // Generate teachers for CS departments using real data
        Map<String, List<Teacher>> teachersByDept = generateCSTeachersFromRealData();
        
        // Generate courses for CS departments using real data
        Map<String, List<Course>> coursesByDept = generateCSCoursesFromRealData();
        
        System.out.println("\nGenerating timetables for " + csStudentGroups.size() + " CS student groups...");
        
        // Generate student timetables with real room information
        for (StudentGroup group : csStudentGroups) {
            generateStudentTimetableWithRealRooms(group, teachersByDept, coursesByDept);
        }
        
        // Generate teacher timetables with real room information
        for (String deptCode : CS_DEPARTMENT_DATA.keySet()) {
            List<Teacher> teachers = teachersByDept.get(deptCode);
            if (teachers != null) {
                for (Teacher teacher : teachers) {
                    generateTeacherTimetableWithRealRooms(teacher, csStudentGroups, coursesByDept);
                }
            }
        }
        
        System.out.println("Generated timetables for:");
        System.out.println("- " + csStudentGroups.size() + " CS student groups");
        System.out.println("- " + teachersByDept.values().stream().mapToInt(List::size).sum() + " CS teachers");
    }
    
    private static List<StudentGroup> generateCSStudentGroups() {
        List<StudentGroup> groups = new ArrayList<>();
        
        for (String deptCode : CS_DEPARTMENT_DATA.keySet()) {
            try {
                Department dept = Department.fromCode(deptCode);
                Map<String, Integer> yearData = CS_DEPARTMENT_DATA.get(deptCode);
                
                for (String year : yearData.keySet()) {
                    int sectionCount = yearData.get(year);
                    
                    for (int i = 1; i <= sectionCount; i++) {
                        char sectionLetter = (char) ('A' + i - 1);
                        String groupId = deptCode + "-Y" + year + "-" + sectionLetter;
                        String groupName = getFullDepartmentName(deptCode) + " Year " + year + " Section " + sectionLetter;
                        
                        StudentGroup group = new StudentGroup(groupId, groupName, 60, dept);
                        group.setYear(Integer.parseInt(year));
                        group.setSection(String.valueOf(sectionLetter));
                        
                        groups.add(group);
                    }
                }
            } catch (IllegalArgumentException e) {
                System.err.println("Warning: Unknown department code: " + deptCode);
            }
        }
        
        return groups;
    }
    
    private static String getFullDepartmentName(String deptCode) {
        switch (deptCode) {
            case "CSD": return "Computer Science & Design";
            case "CSE": return "Computer Science & Engineering";
            case "CSE-CS": return "Computer Science & Engineering (Cyber Security)";
            case "CSBS": return "Computer Science & Business Systems";
            case "IT": return "Information Technology";
            case "AIML": return "Artificial Intelligence & Machine Learning";
            default: return deptCode;
        }
    }
    
    private static Map<String, List<Teacher>> generateCSTeachersFromRealData() {
        Map<String, List<Teacher>> teachersByDept = new HashMap<>();
        
        for (String deptCode : CS_DEPARTMENT_DATA.keySet()) {
            List<Teacher> teachers = new ArrayList<>();
            
            // Get real teachers for this department
            List<TeacherInfo> realTeachersForDept = realTeachers.getOrDefault(deptCode, new ArrayList<>());
            
            if (realTeachersForDept.isEmpty()) {
                System.out.println("Warning: No real teachers found for " + deptCode + ", using fallback");
                // Fallback to generic teachers if no real data
                int teacherCount = Math.max(3, Math.min(8, CS_DEPARTMENT_DATA.get(deptCode).values().stream().mapToInt(Integer::intValue).sum() / 3));
                for (int i = 1; i <= teacherCount; i++) {
                    String teacherId = deptCode + "_Teacher_" + i;
                    String teacherName = getFullDepartmentName(deptCode) + " Teacher " + i;
                    Teacher teacher = new Teacher(teacherId, teacherName);
                    teachers.add(teacher);
                }
            } else {
                // Use real teacher data
                for (TeacherInfo realTeacher : realTeachersForDept) {
                    String teacherId = realTeacher.teacherId;
                    String teacherName = realTeacher.getDisplayName();
                    Teacher teacher = new Teacher(teacherId, teacherName);
                    teachers.add(teacher);
                }
            }
            
            teachersByDept.put(deptCode, teachers);
        }
        
        return teachersByDept;
    }
    
    private static Map<String, List<Course>> generateCSCoursesFromRealData() {
        Map<String, List<Course>> coursesByDept = new HashMap<>();
        
        for (String deptCode : CS_DEPARTMENT_DATA.keySet()) {
            List<Course> courses = new ArrayList<>();
            
            // Get real courses for this department
            List<CourseInfo> realCoursesForDept = realCourses.getOrDefault(deptCode, new ArrayList<>());
            
            if (realCoursesForDept.isEmpty()) {
                System.out.println("Warning: No real courses found for " + deptCode + ", using fallback");
                // Fallback to generic courses
                courses = generateFallbackCourses(deptCode);
            } else {
                // Use real course data
                for (CourseInfo realCourse : realCoursesForDept) {
                    String courseId = realCourse.courseCode;
                    String courseName = realCourse.courseName;
                    String courseType = determineCourseType(realCourse);
                    
                    Course course = new Course(courseId, courseName, courseType);
                    courses.add(course);
                }
            }
            
            coursesByDept.put(deptCode, courses);
        }
        
        return coursesByDept;
    }
    
    private static String determineCourseType(CourseInfo courseInfo) {
        if (courseInfo.practicalHours > 0) {
            return "lab";
        } else if (courseInfo.tutorialHours > 0) {
            return "tutorial";
        } else {
            return "theory";
        }
    }
    
    private static List<Course> generateFallbackCourses(String deptCode) {
        List<Course> courses = new ArrayList<>();
        Map<String, Integer> yearData = CS_DEPARTMENT_DATA.get(deptCode);
        
        for (String year : yearData.keySet()) {
            // Theory courses
            String[] theoryCourses = getTheoryCoursesForYear(year);
            for (String courseName : theoryCourses) {
                String courseId = deptCode + "_Y" + year + "_" + courseName.replace(" ", "_");
                Course course = new Course(courseId, courseName, "theory");
                courses.add(course);
            }
            
            // Lab courses
            String[] labCourses = getLabCoursesForYear(year);
            for (String courseName : labCourses) {
                String courseId = deptCode + "_Y" + year + "_" + courseName.replace(" ", "_") + "_Lab";
                Course course = new Course(courseId, courseName + " Lab", "lab");
                courses.add(course);
            }
            
            // Tutorial courses
            String[] tutorialCourses = getTutorialCoursesForYear(year);
            for (String courseName : tutorialCourses) {
                String courseId = deptCode + "_Y" + year + "_" + courseName.replace(" ", "_") + "_Tutorial";
                Course course = new Course(courseId, courseName + " Tutorial", "tutorial");
                courses.add(course);
            }
        }
        
        return courses;
    }
    
    private static String[] getTheoryCoursesForYear(String year) {
        switch (year) {
            case "2":
                return new String[]{"Data Structures", "Object Oriented Programming", "Database Management", 
                                  "Computer Networks", "Operating Systems", "Software Engineering"};
            case "3":
                return new String[]{"Algorithm Design", "Web Technologies", "Machine Learning", 
                                  "Compiler Design", "Computer Graphics", "Distributed Systems"};
            case "4":
                return new String[]{"Advanced Algorithms", "Cloud Computing", "Cybersecurity", 
                                  "Project Management", "Research Methodology", "Industry Project"};
            default:
                return new String[]{"Programming Fundamentals", "Mathematics", "Physics"};
        }
    }
    
    private static String[] getLabCoursesForYear(String year) {
        switch (year) {
            case "2":
                return new String[]{"Programming Lab", "Database Lab", "Networks Lab"};
            case "3":
                return new String[]{"Web Development Lab", "ML Lab", "Graphics Lab"};
            case "4":
                return new String[]{"Project Lab", "Advanced Programming Lab", "Research Lab"};
            default:
                return new String[]{"Basic Programming Lab", "Hardware Lab"};
        }
    }
    
    private static String[] getTutorialCoursesForYear(String year) {
        switch (year) {
            case "2":
                return new String[]{"Problem Solving", "Technical Communication", "Mathematics Tutorial"};
            case "3":
                return new String[]{"Algorithm Analysis", "System Design", "Project Planning"};
            case "4":
                return new String[]{"Industry Interaction", "Research Seminar", "Career Guidance"};
            default:
                return new String[]{"Basic Concepts", "Study Skills"};
        }
    }
    
    private static void generateStudentTimetableWithRealRooms(StudentGroup group, 
            Map<String, List<Teacher>> teachersByDept, 
            Map<String, List<Course>> coursesByDept) {
        
        String deptCode = group.getDepartment().getCode();
        List<Teacher> teachers = teachersByDept.get(deptCode);
        List<Course> courses = coursesByDept.get(deptCode);
        
        if (teachers == null || courses == null) {
            System.err.println("Warning: No teachers or courses found for department: " + deptCode);
            return;
        }
        
        // Create timetable with real room assignments and proper scheduling
        Map<String, Map<String, String>> timetable = new HashMap<>();
        
        // Initialize empty timetable
        for (String day : TimetableConstants.DAYS) {
            Map<String, String> daySchedule = new HashMap<>();
            // Use theory time slots for initialization (most common)
            for (String timeSlot : TimetableConstants.THEORY_TIME_SLOTS) {
                daySchedule.put(timeSlot, "");
            }
            // Add lab time slots
            for (String timeSlot : TimetableConstants.LAB_TIME_SLOTS) {
                daySchedule.put(timeSlot, "");
            }
            timetable.put(day, daySchedule);
        }
        
        // Track teacher schedules to avoid conflicts
        Map<String, Set<String>> teacherSchedules = new HashMap<>();
        for (Teacher teacher : teachers) {
            teacherSchedules.put(teacher.getId(), new HashSet<>());
        }
        
        // Assign courses to time slots with proper scheduling
        int maxClasses = Math.min(15, courses.size()); // Limit classes per student group
        int classesScheduled = 0;
        
        // Separate courses by type for better scheduling
        List<Course> theoryCourses = courses.stream()
            .filter(c -> "theory".equals(c.getCourseType()))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        List<Course> labCourses = courses.stream()
            .filter(c -> "lab".equals(c.getCourseType()))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        List<Course> tutorialCourses = courses.stream()
            .filter(c -> "tutorial".equals(c.getCourseType()))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        
        // Schedule lab courses first (they have fewer time slots available)
        classesScheduled += scheduleCoursesForGroup(labCourses, teachers, timetable, 
                                                   teacherSchedules, group, maxClasses - classesScheduled);
        
        // Schedule theory courses
        classesScheduled += scheduleCoursesForGroup(theoryCourses, teachers, timetable, 
                                                   teacherSchedules, group, maxClasses - classesScheduled);
        
        // Schedule tutorial courses
        classesScheduled += scheduleCoursesForGroup(tutorialCourses, teachers, timetable, 
                                                   teacherSchedules, group, maxClasses - classesScheduled);
        
        // Export to HTML
        TimetableExporter.SimpleStudentGroup simpleGroup = new TimetableExporter.SimpleStudentGroup(
            group.getId(), group.getName(), group.getDepartment().getCode());
        TimetableExporter.exportStudentTimetable(simpleGroup, timetable);
    }
    
    /**
     * Schedule courses for a student group avoiding conflicts
     */
    private static int scheduleCoursesForGroup(List<Course> courses, List<Teacher> teachers,
                                             Map<String, Map<String, String>> timetable,
                                             Map<String, Set<String>> teacherSchedules,
                                             StudentGroup group, int maxClasses) {
        int scheduled = 0;
        String deptCode = group.getDepartment().getCode();
        List<DataLoader.CourseTeacherMapping> mappings = courseTeacherMappings.getOrDefault(deptCode, new ArrayList<>());
        
        for (Course course : courses) {
            if (scheduled >= maxClasses) break;
            
            // Find the correct teacher for this course from CSV mappings
            Teacher assignedTeacher = findCorrectTeacherForCourse(course, teachers, mappings);
            if (assignedTeacher == null) {
                System.err.println("Warning: No teacher found for course " + course.getName() + " in department " + deptCode);
                continue;
            }
            
            // Get appropriate time slots for this course type
            List<String> availableTimeSlots = TimetableConstants.getTimeSlotsForCourseType(course.getCourseType());
            
            // Try to find a suitable slot
            boolean courseScheduled = false;
            for (String day : TimetableConstants.DAYS) {
                if (courseScheduled) break;
                
                for (String timeSlot : availableTimeSlots) {
                    // Skip lunch break for theory courses
                    if ("theory".equals(course.getCourseType()) && 
                        TimetableConstants.BREAK_SLOTS.contains(timeSlot)) {
                        continue;
                    }
                    
                    // Check if slot is available for student group
                    if (!timetable.get(day).get(timeSlot).isEmpty()) {
                        continue;
                    }
                    
                    // Check if the assigned teacher is available
                    if (!isTeacherAvailable(assignedTeacher, teacherSchedules, day, timeSlot)) {
                        continue;
                    }
                    
                    // Check if teacher hasn't exceeded maximum hours
                    int teacherHours = calculateTeacherHours(teacherSchedules.get(assignedTeacher.getId()), course.getCourseType());
                    if (teacherHours >= TimetableConstants.MAX_TEACHER_HOURS) {
                        continue;
                    }
                    
                    // Assign the class with the correct teacher
                    String room = assignRealRoom(course.getCourseType());
                    String classInfo = String.format("%s<br>%s<br><b>Room: %s</b><br><!-- %s -->", 
                            course.getName(), assignedTeacher.getName(), room, course.getCourseType());
                    
                    timetable.get(day).put(timeSlot, classInfo);
                    
                    // Mark teacher as busy for this slot
                    String teacherSlotKey = day + "|" + timeSlot;
                    teacherSchedules.get(assignedTeacher.getId()).add(teacherSlotKey);
                    
                    // For lab courses, handle batch splitting if needed
                    if ("lab".equals(course.getCourseType()) && 
                        TimetableConstants.CLASS_STRENGTH > TimetableConstants.LAB_BATCH_SIZE) {
                        // This lab needs multiple batches - could be enhanced further
                        // For now, we'll schedule it as one session
                    }
                    
                    scheduled++;
                    courseScheduled = true;
                    break;
                }
            }
            
            if (!courseScheduled) {
                System.err.println("Warning: Could not schedule course " + course.getName() + 
                                 " with teacher " + assignedTeacher.getName() + " in department " + deptCode);
            }
        }
        
        return scheduled;
    }
    
    /**
     * Find the correct teacher for a course based on CSV mappings
     */
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
    
    /**
     * Check if a teacher is available for the given time slot
     */
    private static boolean isTeacherAvailable(Teacher teacher, Map<String, Set<String>> teacherSchedules,
                                            String day, String timeSlot) {
        String slotKey = day + "|" + timeSlot;
        Set<String> teacherSlots = teacherSchedules.get(teacher.getId());
        
        // Check if teacher is available (no conflict)
        for (String existingSlot : teacherSlots) {
            String[] parts = existingSlot.split("\\|");
            if (parts.length == 2 && parts[0].equals(day)) {
                if (TimetableConstants.hasTimeConflict(timeSlot, parts[1])) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Calculate total hours for a teacher based on their schedule
     */
    private static int calculateTeacherHours(Set<String> teacherSlots, String courseType) {
        int totalMinutes = 0;
        
        for (String slot : teacherSlots) {
            String[] parts = slot.split("\\|");
            if (parts.length == 2) {
                String timeSlot = parts[1];
                
                // Determine duration based on time slot type
                if (TimetableConstants.LAB_TIME_SLOTS.contains(timeSlot)) {
                    totalMinutes += TimetableConstants.LAB_DURATION_MINUTES;
                } else {
                    totalMinutes += TimetableConstants.THEORY_DURATION_MINUTES;
                }
            }
        }
        
        // Convert to hours (rounded up)
        return (totalMinutes + 59) / 60; // Ceiling division
    }
    
    private static void generateTeacherTimetableWithRealRooms(Teacher teacher, 
            List<StudentGroup> allStudentGroups,
            Map<String, List<Course>> coursesByDept) {
        
        // Create timetable with proper scheduling
        Map<String, Map<String, String>> timetable = new HashMap<>();
        
        // Initialize empty timetable with all possible time slots
        for (String day : TimetableConstants.DAYS) {
            Map<String, String> daySchedule = new HashMap<>();
            // Add theory time slots
            for (String timeSlot : TimetableConstants.THEORY_TIME_SLOTS) {
                daySchedule.put(timeSlot, "");
            }
            // Add lab time slots
            for (String timeSlot : TimetableConstants.LAB_TIME_SLOTS) {
                daySchedule.put(timeSlot, "");
            }
            timetable.put(day, daySchedule);
        }
        
        // Get teacher's department from ID
        String teacherId = teacher.getId();
        String tempDeptCode = teacherId.split("_")[0];
        if (tempDeptCode.length() > 10) { // Handle real teacher IDs
            tempDeptCode = "CSE"; // Default for real teacher IDs
        }
        final String deptCode = tempDeptCode;
        
        // Get student groups for this department
        List<StudentGroup> deptGroups = allStudentGroups.stream()
                .filter(g -> g.getDepartment().getCode().equals(deptCode))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        
        // Get courses that this teacher should teach based on CSV mappings
        List<Course> teacherCourses = getCoursesForTeacher(teacher, deptCode);
        
        if (teacherCourses.isEmpty()) {
            System.err.println("Warning: No courses found for teacher: " + teacher.getName());
            return;
        }
        
        if (deptGroups.isEmpty()) {
            System.err.println("Warning: No student groups found for department: " + deptCode);
            return;
        }
        
        // Track teacher's total hours
        int totalHours = 0;
        int maxHours = TimetableConstants.MAX_TEACHER_HOURS;
        
        // Separate courses by type for better scheduling
        List<Course> theoryCourses = teacherCourses.stream()
            .filter(c -> "theory".equals(c.getCourseType()))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        List<Course> labCourses = teacherCourses.stream()
            .filter(c -> "lab".equals(c.getCourseType()))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        List<Course> tutorialCourses = teacherCourses.stream()
            .filter(c -> "tutorial".equals(c.getCourseType()))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        
        // Schedule classes for teacher respecting hour limits
        totalHours += scheduleTeacherCourses(labCourses, deptGroups, timetable, maxHours - totalHours);
        totalHours += scheduleTeacherCourses(theoryCourses, deptGroups, timetable, maxHours - totalHours);
        totalHours += scheduleTeacherCourses(tutorialCourses, deptGroups, timetable, maxHours - totalHours);
        
        // Export to HTML
        TimetableExporter.exportTeacherTimetable(teacher, timetable);
    }
    
    /**
     * Get courses that a specific teacher should teach based on CSV mappings
     */
    private static List<Course> getCoursesForTeacher(Teacher teacher, String deptCode) {
        List<Course> teacherCourses = new ArrayList<>();
        List<DataLoader.CourseTeacherMapping> mappings = courseTeacherMappings.getOrDefault(deptCode, new ArrayList<>());
        
        for (DataLoader.CourseTeacherMapping mapping : mappings) {
            // Check if this mapping is for the current teacher
            if (mapping.teacherId.equals(teacher.getId()) || 
                mapping.teacherName.equals(teacher.getName()) ||
                teacher.getName().contains(mapping.teacherName) ||
                mapping.teacherName.contains(teacher.getName())) {
                
                // Determine course type based on practical hours
                String courseType = "theory";
                if (mapping.practicalHours > 0) {
                    courseType = "lab";
                } else if (mapping.tutorialHours > 0) {
                    courseType = "tutorial";
                }
                
                Course course = new Course(mapping.courseCode, mapping.courseName, courseType);
                teacherCourses.add(course);
            }
        }
        
        // Remove duplicates
        Set<String> seen = new HashSet<>();
        teacherCourses.removeIf(c -> !seen.add(c.getId()));
        
        return teacherCourses;
    }
    
    /**
     * Schedule courses for a teacher respecting hour limits
     */
    private static int scheduleTeacherCourses(List<Course> courses, List<StudentGroup> groups,
                                            Map<String, Map<String, String>> timetable,
                                            int remainingHours) {
        int hoursScheduled = 0;
        
        for (Course course : courses) {
            if (hoursScheduled >= remainingHours) break;
            
            // Get appropriate time slots for this course type
            List<String> availableTimeSlots = TimetableConstants.getTimeSlotsForCourseType(course.getCourseType());
            
            // Calculate hours for this course type
            int courseHours = "lab".equals(course.getCourseType()) ? 
                TimetableConstants.LAB_DURATION : TimetableConstants.THEORY_DURATION;
            
            if (hoursScheduled + courseHours > remainingHours) {
                continue; // Skip if it would exceed hour limit
            }
            
            // Try to find a suitable slot
            boolean courseScheduled = false;
            for (String day : TimetableConstants.DAYS) {
                if (courseScheduled) break;
                
                for (String timeSlot : availableTimeSlots) {
                    // Skip lunch break for theory courses
                    if ("theory".equals(course.getCourseType()) && 
                        TimetableConstants.BREAK_SLOTS.contains(timeSlot)) {
                        continue;
                    }
                    
                    // Check if slot is available
                    if (!timetable.get(day).get(timeSlot).isEmpty()) {
                        continue;
                    }
                    
                    // Assign the class
                    StudentGroup group = groups.get(random.nextInt(groups.size()));
                    String room = assignRealRoom(course.getCourseType());
                    String classInfo = String.format("%s<br>%s<br><b>Room: %s</b><br><!-- %s -->", 
                            course.getName(), group.getName(), room, course.getCourseType());
                    
                    timetable.get(day).put(timeSlot, classInfo);
                    hoursScheduled += courseHours;
                    courseScheduled = true;
                    break;
                }
            }
        }
        
        return hoursScheduled;
    }
    
    private static String assignRealRoom(String courseType) {
        if (courseType == null) courseType = "theory";
        
        String assignedRoom = null;
        
        switch (courseType.toLowerCase()) {
            case "lab":
                if (!realLabs.isEmpty()) {
                    RoomInfo lab = realLabs.get(random.nextInt(realLabs.size()));
                    assignedRoom = lab.getDisplayName();
                }
                if (assignedRoom == null) {
                    assignedRoom = "Lab-001";
                }
                break;
                
            case "tutorial":
                // Use smaller classrooms for tutorials, but exclude any lab rooms
                List<RoomInfo> smallClassrooms = realClassrooms.stream()
                        .filter(r -> r.maxCapacity <= 50)
                        .filter(r -> !r.isLab) // Ensure no lab rooms
                        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
                if (!smallClassrooms.isEmpty()) {
                    RoomInfo room = smallClassrooms.get(random.nextInt(smallClassrooms.size()));
                    assignedRoom = room.getDisplayName();
                }
                // Fallback to any non-lab classroom
                break;
                
            case "theory":
            default:
                if (!realClassrooms.isEmpty()) {
                    // Filter out any lab rooms for theory courses
                    List<RoomInfo> availableClassrooms = realClassrooms.stream()
                        .filter(r -> !r.isLab) // Ensure no lab rooms
                        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
                    
                    if (!availableClassrooms.isEmpty()) {
                        RoomInfo classroom = availableClassrooms.get(random.nextInt(availableClassrooms.size()));
                        assignedRoom = classroom.getDisplayName();
                    } else {
                        assignedRoom = "CR-001";
                    }
                }
                if (assignedRoom == null) {
                    assignedRoom = "CR-001";
                }
                break;
        }
        
        // If still no room assigned, use fallback
        if (assignedRoom == null) {
            assignedRoom = "CR-001";
        }
        
        return assignedRoom;
    }
} 