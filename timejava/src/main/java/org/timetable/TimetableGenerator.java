package org.timetable;

import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.termination.TerminationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.timetable.domain.*;
import org.timetable.solver.TimetableEasyScoreCalculator;
import org.timetable.util.TimeSlotGenerator;
import org.timetable.util.TimetableConstants;

import java.io.File;
import java.util.*;

/**
 * Real timetable generator that creates a timetable based on the provided data.
 */
public class TimetableGenerator {
    private static final Logger logger = LoggerFactory.getLogger(TimetableGenerator.class);

    public static void main(String[] args) {
        logger.info("Starting Timetable Generator");
        
        // Create the timetable problem
        TimetableProblem problem = createProblem();
        
        // Build the solver
        SolverFactory<TimetableProblem> solverFactory = SolverFactory.create(
                new SolverConfig()
                        .withSolutionClass(TimetableProblem.class)
                        .withEntityClasses(Lesson.class)
                        .withScoreDirectorFactory(
                                new SolverConfig.ScoreDirectorFactoryConfig()
                                        .withEasyScoreCalculatorClass(TimetableEasyScoreCalculator.class))
                        .withTerminationConfig(new TerminationConfig()
                                .withSecondsSpentLimit(30L)) // Limit to 30 seconds for testing
        );
        
        Solver<TimetableProblem> solver = solverFactory.buildSolver();
        
        // Solve the problem
        logger.info("Solving timetable problem...");
        TimetableProblem solution = solver.solve(problem);
        
        // Display the solution
        logger.info("Solution score: {}", solution.getScore());
        displaySolution(solution);
        
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
     * Create rooms based on data.
     */
    private static List<Room> createRooms() {
        List<Room> rooms = new ArrayList<>();
        int roomId = 1;
        
        // Create theory rooms
        for (int i = 1; i <= 20; i++) {
            rooms.add(new Room("TR" + i, "Theory Room " + i, "A", "1", TimetableConstants.CLASS_STRENGTH, false));
        }
        
        // Create lab rooms
        for (int i = 1; i <= 10; i++) {
            rooms.add(new Room("LR" + i, "Lab Room " + i, "B", "1", TimetableConstants.LAB_BATCH_SIZE * 2, true));
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
                teachers.add(new Teacher("T" + teacherId++, name, dept.name(), TimetableConstants.MAX_TEACHER_HOURS));
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
            Department dept = Department.getByAbbreviation(deptCode);
            
            // If department is not in our enum, skip it
            if (dept == null) continue;
            
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
                            TimetableConstants.CLASS_STRENGTH,
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