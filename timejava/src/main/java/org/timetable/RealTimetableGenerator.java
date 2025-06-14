package org.timetable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.timetable.domain.*;
import org.timetable.util.TimeSlotGenerator;
import org.timetable.util.TimetableConstants;
import org.timetable.visualization.TimetableVisualizer;

import java.io.File;
import java.util.*;

/**
 * Real timetable generator that creates a timetable based on the provided data.
 * This is a simplified version that doesn't use OptaPlanner yet.
 */
public class RealTimetableGenerator {
    private static final Logger logger = LoggerFactory.getLogger(RealTimetableGenerator.class);

    public static void main(String[] args) {
        logger.info("Starting Real Timetable Generator");
        
        // Create the timetable problem
        TimetableProblem problem = createProblem();
        
        // For now, we'll just assign lessons randomly
        assignLessonsRandomly(problem);
        
        // Display the solution
        displaySolution(problem);
        
        // Generate timetables
        generateTimetables(problem);
        
        logger.info("Timetable generation completed!");
    }
    
    /**
     * Create the timetable problem with all necessary data.
     */
    private static TimetableProblem createProblem() {
        TimetableProblem problem = new TimetableProblem();
        
        // 1. Generate time slots
        List<TimeSlot> timeSlots = TimeSlotGenerator.generateAllTimeSlots();
        problem.setTimeSlots(timeSlots);
        logger.info("Generated {} time slots", timeSlots.size());
        
        // 2. Create rooms
        List<Room> rooms = createRooms();
        problem.setRooms(rooms);
        logger.info("Created {} rooms", rooms.size());
        
        // 3. Create teachers
        List<Teacher> teachers = createTeachers();
        problem.setTeachers(teachers);
        logger.info("Created {} teachers", teachers.size());
        
        // 4. Create student groups
        List<StudentGroup> studentGroups = createStudentGroups();
        problem.setStudentGroups(studentGroups);
        logger.info("Created {} student groups", studentGroups.size());
        
        // 5. Create courses
        List<Course> courses = createCourses();
        problem.setCourses(courses);
        logger.info("Created {} courses", courses.size());
        
        // 6. Create lessons
        List<Lesson> lessons = createLessons(courses, teachers, studentGroups);
        problem.setLessons(lessons);
        logger.info("Created {} lessons", lessons.size());
        
        return problem;
    }
    
    /**
     * Assign lessons randomly to time slots and rooms.
     */
    private static void assignLessonsRandomly(TimetableProblem problem) {
        List<TimeSlot> timeSlots = problem.getTimeSlots();
        List<Room> rooms = problem.getRooms();
        List<Lesson> lessons = problem.getLessons();
        
        // Split rooms into lab and theory rooms
        List<Room> labRooms = new ArrayList<>();
        List<Room> theoryRooms = new ArrayList<>();
        
        for (Room room : rooms) {
            if (room.isLab()) {
                labRooms.add(room);
            } else {
                theoryRooms.add(room);
            }
        }
        
        // Split time slots into lab and theory slots
        List<TimeSlot> labTimeSlots = new ArrayList<>();
        List<TimeSlot> theoryTimeSlots = new ArrayList<>();
        
        for (TimeSlot slot : timeSlots) {
            if (slot.isLabTimeSlot()) {
                labTimeSlots.add(slot);
            } else {
                theoryTimeSlots.add(slot);
            }
        }
        
        // Randomly assign lessons to time slots and rooms
        Random random = new Random(42); // Use a fixed seed for reproducibility
        
        for (Lesson lesson : lessons) {
            if ("lab".equals(lesson.getLessonType())) {
                // Assign lab lesson to a lab time slot and lab room
                if (!labTimeSlots.isEmpty() && !labRooms.isEmpty()) {
                    TimeSlot timeSlot = labTimeSlots.get(random.nextInt(labTimeSlots.size()));
                    Room room = labRooms.get(random.nextInt(labRooms.size()));
                    lesson.setTimeSlot(timeSlot);
                    lesson.setRoom(room);
                }
            } else {
                // Assign theory/tutorial lesson to a theory time slot and theory room
                if (!theoryTimeSlots.isEmpty() && !theoryRooms.isEmpty()) {
                    TimeSlot timeSlot = theoryTimeSlots.get(random.nextInt(theoryTimeSlots.size()));
                    Room room = theoryRooms.get(random.nextInt(theoryRooms.size()));
                    lesson.setTimeSlot(timeSlot);
                    lesson.setRoom(room);
                }
            }
        }
    }
    
    /**
     * Create rooms based on data.
     */
    private static List<Room> createRooms() {
        List<Room> rooms = new ArrayList<>();
        
        // Create theory rooms
        for (int i = 1; i <= 20; i++) {
            rooms.add(new Room("TR" + i, "Theory Room " + i, 70));
        }
        
        // Create lab rooms
        for (int i = 1; i <= 10; i++) {
            rooms.add(new Room("LR" + i, "Lab Room " + i, "B", "1", 40, true));
        }
        
        return rooms;
    }
    
    /**
     * Create teachers based on data.
     */
    private static List<Teacher> createTeachers() {
        List<Teacher> teachers = new ArrayList<>();
        int teacherId = 1;
        
        // Create teachers for each department
        for (Department dept : Department.values()) {
            // Create 5 teachers per department
            for (int i = 1; i <= 5; i++) {
                String name = dept.name() + " Teacher " + i;
                teachers.add(new Teacher("T" + teacherId++, name, dept.name(), 20));
            }
        }
        
        return teachers;
    }
    
    /**
     * Create student groups based on department data.
     */
    private static List<StudentGroup> createStudentGroups() {
        List<StudentGroup> groups = new ArrayList<>();
        int groupId = 1;
        
        // For each department in the department data
        for (Map.Entry<String, Map<String, Integer>> deptEntry : TimetableConstants.DEPARTMENT_DATA.entrySet()) {
            String deptCode = deptEntry.getKey();
            Department dept = Department.valueOf(deptCode);
            
            // For each year in the department
            for (Map.Entry<String, Integer> yearEntry : deptEntry.getValue().entrySet()) {
                int year = Integer.parseInt(yearEntry.getKey());
                int numSections = yearEntry.getValue();
                
                // For each section
                for (int section = 1; section <= numSections; section++) {
                    String sectionLetter = (char)('A' + section - 1) + "";
                    String groupName = deptCode + "-" + year + sectionLetter;
                    
                    StudentGroup group = new StudentGroup(
                            "SG" + groupId++,
                            groupName,
                            70,
                            dept
                    );
                    group.setYear(year);
                    group.setSection(sectionLetter);
                    
                    groups.add(group);
                }
            }
        }
        
        return groups;
    }
    
    /**
     * Create courses based on data.
     */
    private static List<Course> createCourses() {
        List<Course> courses = new ArrayList<>();
        int courseId = 1;
        
        // Create courses for each department
        for (Department dept : Department.values()) {
            // Create theory courses
            for (int i = 1; i <= 5; i++) {
                String code = dept.name() + "T" + i;
                String name = dept.name() + " Theory " + i;
                courses.add(new Course(code, name, dept.name(), "theory", 3, 0, 0, 0));
            }
            
            // Create lab courses
            for (int i = 1; i <= 3; i++) {
                String code = dept.name() + "L" + i;
                String name = dept.name() + " Lab " + i;
                courses.add(new Course(code, name, dept.name(), "lab", 0, 3, 0, 0));
            }
            
            // Create tutorial courses
            for (int i = 1; i <= 2; i++) {
                String code = dept.name() + "TU" + i;
                String name = dept.name() + " Tutorial " + i;
                courses.add(new Course(code, name, dept.name(), "tutorial", 0, 0, 2, 0));
            }
        }
        
        return courses;
    }
    
    /**
     * Create lessons based on courses, teachers, and student groups.
     */
    private static List<Lesson> createLessons(List<Course> courses, List<Teacher> teachers, List<StudentGroup> studentGroups) {
        List<Lesson> lessons = new ArrayList<>();
        long lessonId = 1;
        
        // For each student group
        for (StudentGroup group : studentGroups) {
            Department dept = group.getDepartment();
            
            // Find courses for this department
            List<Course> deptCourses = new ArrayList<>();
            for (Course course : courses) {
                if (course.getDepartment().equals(dept.name())) {
                    deptCourses.add(course);
                }
            }
            
            // Find teachers for this department
            List<Teacher> deptTeachers = new ArrayList<>();
            for (Teacher teacher : teachers) {
                if (teacher.getDepartment().equals(dept.name())) {
                    deptTeachers.add(teacher);
                }
            }
            
            // Assign courses to student group
            if (!deptCourses.isEmpty() && !deptTeachers.isEmpty()) {
                int teacherIndex = 0;
                
                // Assign theory courses
                for (Course course : deptCourses) {
                    if (course.getLectureHours() > 0) {
                        Teacher teacher = deptTeachers.get(teacherIndex % deptTeachers.size());
                        teacherIndex++;
                        
                        // Create a lesson for each lecture hour
                        for (int i = 0; i < course.getLectureHours(); i++) {
                            lessons.add(new Lesson(
                                    lessonId++,
                                    teacher,
                                    course,
                                    group,
                                    "lecture",
                                    1
                            ));
                        }
                    }
                    
                    // Assign lab courses
                    if (course.getLabHours() > 0) {
                        Teacher teacher = deptTeachers.get(teacherIndex % deptTeachers.size());
                        teacherIndex++;
                        
                        // Create a lesson for each lab hour (labs are typically longer)
                        lessons.add(new Lesson(
                                lessonId++,
                                teacher,
                                course,
                                group,
                                "lab",
                                2
                        ));
                    }
                    
                    // Assign tutorial courses
                    if (course.getTutorialHours() > 0) {
                        Teacher teacher = deptTeachers.get(teacherIndex % deptTeachers.size());
                        teacherIndex++;
                        
                        // Create a lesson for each tutorial hour
                        for (int i = 0; i < course.getTutorialHours(); i++) {
                            lessons.add(new Lesson(
                                    lessonId++,
                                    teacher,
                                    course,
                                    group,
                                    "tutorial",
                                    1
                            ));
                        }
                    }
                }
            }
        }
        
        return lessons;
    }
    
    /**
     * Generate timetables for the solution.
     */
    private static void generateTimetables(TimetableProblem solution) {
        TimetableVisualizer visualizer = new TimetableVisualizer();
        
        // Create output directories
        File studentTimetablesDir = new File("output/student_timetables");
        File teacherTimetablesDir = new File("output/teacher_timetables");
        
        // Generate timetables
        visualizer.generateStudentTimetables(solution, studentTimetablesDir.getPath());
        visualizer.generateTeacherTimetables(solution, teacherTimetablesDir.getPath());
        
        // Generate solution summary
        visualizer.generateSolutionSummary(solution, new File("output/solution_summary.txt"));
        
        logger.info("Timetables generated in output/ directory");
    }
    
    /**
     * Display the solution.
     */
    private static void displaySolution(TimetableProblem solution) {
        logger.info("Timetable Solution:");
        logger.info("-------------------");
        
        // Count assigned and unassigned lessons
        int assignedCount = 0;
        int unassignedCount = 0;
        
        for (Lesson lesson : solution.getLessons()) {
            if (lesson.getTimeSlot() != null && lesson.getRoom() != null) {
                assignedCount++;
            } else {
                unassignedCount++;
            }
        }
        
        logger.info("Assigned lessons: {}", assignedCount);
        logger.info("Unassigned lessons: {}", unassignedCount);
        
        // Display a few example assignments
        logger.info("\nSample assignments:");
        int count = 0;
        for (Lesson lesson : solution.getLessons()) {
            if (lesson.getTimeSlot() != null && lesson.getRoom() != null) {
                logger.info("  {} - {} - {} - {} - {} - {}",
                        lesson.getCourse().getName(),
                        lesson.getLessonType(),
                        lesson.getStudentGroup().getName(),
                        lesson.getTeacher().getName(),
                        lesson.getRoom().getName(),
                        lesson.getTimeSlot());
                
                count++;
                if (count >= 10) break;
            }
        }
    }
} 