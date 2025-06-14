package org.timetable;

import org.timetable.domain.*;
import org.timetable.persistence.TimetableDataLoader;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Simple test class to verify data loading
 */
public class DataLoadTest {

    public static void main(String[] args) {
        // Get file paths from arguments or use defaults
        String coursesFile = args.length > 0 ? args[0] : "data/courses/cse_dept_red.csv";
        String dataDir = args.length > 1 ? args[1] : "data";
        
        System.out.println("Loading data from:");
        System.out.println("- Courses file: " + coursesFile);
        System.out.println("- Data directory: " + dataDir);
        
        // Load problem
        TimetableProblem problem = TimetableDataLoader.loadProblem(coursesFile, dataDir);
        
        // Print statistics
        System.out.println("\nData Loading Statistics:");
        System.out.println("- Teachers: " + problem.getTeachers().size());
        System.out.println("- Courses: " + problem.getCourses().size());
        System.out.println("- Student Groups: " + problem.getStudentGroups().size());
        System.out.println("- Rooms: " + problem.getRooms().size());
        System.out.println("- Time Slots: " + problem.getTimeSlots().size());
        System.out.println("- Lessons: " + problem.getLessons().size());
        
        // Print room statistics
        List<Room> labRooms = problem.getRooms().stream()
            .filter(Room::isLab)
            .collect(Collectors.toList());
        List<Room> theoryRooms = problem.getRooms().stream()
            .filter(room -> !room.isLab())
            .collect(Collectors.toList());
        
        System.out.println("\nRoom Statistics:");
        System.out.println("- Lab Rooms: " + labRooms.size());
        System.out.println("- Theory Rooms: " + theoryRooms.size());
        
        // Print lesson statistics
        Map<String, Long> lessonsByType = problem.getLessons().stream()
            .collect(Collectors.groupingBy(Lesson::getLessonType, Collectors.counting()));
        
        System.out.println("\nLesson Statistics:");
        lessonsByType.forEach((type, count) -> System.out.println("- " + type + ": " + count));
        
        // Print sample data
        System.out.println("\nSample Teachers:");
        problem.getTeachers().stream().limit(5).forEach(teacher -> 
            System.out.println("- " + teacher.getName() + " (ID: " + teacher.getId() + ")"));
        
        System.out.println("\nSample Courses:");
        problem.getCourses().stream().limit(5).forEach(course -> 
            System.out.println("- " + course.getCourseCode() + ": " + course.getName()));
        
        System.out.println("\nSample Rooms:");
        problem.getRooms().stream().limit(5).forEach(room -> 
            System.out.println("- " + room.getName() + " (" + (room.isLab() ? "Lab" : "Theory") + ")"));
    }
} 