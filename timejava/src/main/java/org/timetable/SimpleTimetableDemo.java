package org.timetable;

import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.timetable.domain.*;

import java.util.ArrayList;
import java.util.List;

public class SimpleTimetableDemo {
    private static final Logger logger = LoggerFactory.getLogger(SimpleTimetableDemo.class);

    public static void main(String[] args) {
        // Create a solver factory from the solver configuration
        SolverFactory<TimetableProblem> solverFactory = SolverFactory.createFromXmlResource(
                "org/timetable/solver/timetableSolverConfig.xml");
        
        // Build the Solver
        Solver<TimetableProblem> solver = solverFactory.buildSolver();
        
        // Create a simple problem
        TimetableProblem problem = createDemoData();
        
        // Solve the problem
        logger.info("Solving timetable problem...");
        TimetableProblem solution = solver.solve(problem);
        
        // Display the solution
        logger.info("Solution score: {}", solution.getScore());
        displaySolution(solution);
    }
    
    private static TimetableProblem createDemoData() {
        // Create some time slots
        List<TimeSlot> timeSlots = new ArrayList<>();
        for (int day = 0; day < 5; day++) { // Monday to Friday
            for (int hour = 8; hour < 16; hour++) { // 8 AM to 4 PM
                timeSlots.add(new TimeSlot(day, hour, hour + 1));
            }
        }
        
        // Create some rooms
        List<Room> rooms = new ArrayList<>();
        rooms.add(new Room("R1", "Room 1", 30));
        rooms.add(new Room("R2", "Room 2", 40));
        rooms.add(new Room("R3", "Room 3", 50));
        
        // Create some teachers
        List<Teacher> teachers = new ArrayList<>();
        teachers.add(new Teacher("T1", "Teacher 1"));
        teachers.add(new Teacher("T2", "Teacher 2"));
        
        // Create some student groups
        List<StudentGroup> studentGroups = new ArrayList<>();
        studentGroups.add(new StudentGroup("G1", "Group 1", 25));
        studentGroups.add(new StudentGroup("G2", "Group 2", 35));
        
        // Create some courses
        List<Course> courses = new ArrayList<>();
        courses.add(new Course("C1", "Course 1"));
        courses.add(new Course("C2", "Course 2"));
        
        // Create some lessons
        List<Lesson> lessons = new ArrayList<>();
        lessons.add(new Lesson(1L, teachers.get(0), courses.get(0), studentGroups.get(0), "lecture", 1));
        lessons.add(new Lesson(2L, teachers.get(0), courses.get(1), studentGroups.get(0), "lecture", 1));
        lessons.add(new Lesson(3L, teachers.get(1), courses.get(0), studentGroups.get(1), "lecture", 1));
        lessons.add(new Lesson(4L, teachers.get(1), courses.get(1), studentGroups.get(1), "lecture", 1));
        
        // Create the problem
        TimetableProblem problem = new TimetableProblem();
        problem.setTimeSlotList(timeSlots);
        problem.setRoomList(rooms);
        problem.setTeacherList(teachers);
        problem.setStudentGroupList(studentGroups);
        problem.setCourseList(courses);
        problem.setLessonList(lessons);
        
        return problem;
    }
    
    private static void displaySolution(TimetableProblem solution) {
        logger.info("Timetable Solution:");
        for (Lesson lesson : solution.getLessons()) {
            logger.info("Lesson: {}, Room: {}, Time: {}",
                    lesson.getCourse().getName(),
                    lesson.getRoom() != null ? lesson.getRoom().getName() : "Not assigned",
                    lesson.getTimeSlot() != null ? lesson.getTimeSlot().toString() : "Not assigned");
        }
    }
} 