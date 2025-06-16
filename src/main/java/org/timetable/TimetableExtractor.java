package org.timetable;

import org.timetable.domain.*;
import org.timetable.persistence.TimetableDataLoader;
import java.util.*;
import java.util.stream.Collectors;
import java.time.DayOfWeek;

public class TimetableExtractor {
    public static void main(String[] args) {
        // Get file paths from arguments or use defaults
        String coursesFile = args.length > 0 ? args[0] : "data/courses/cse_dept_red.csv";
        String dataDir = args.length > 1 ? args[1] : "data";
        
        // Load problem
        TimetableProblem problem = TimetableDataLoader.loadProblem(coursesFile, dataDir);
        
        // Filter for CSE-CS 2nd year
        List<StudentGroup> cseCs2Groups = problem.getStudentGroups().stream()
            .filter(group -> group.getDepartment().equals("CSE") && group.getYear() == 2)
            .collect(Collectors.toList());
            
        if (cseCs2Groups.isEmpty()) {
            System.out.println("No CSE-CS 2nd year groups found!");
            return;
        }
        
        // Get all lessons for these groups
        List<Lesson> cseCs2Lessons = problem.getLessons().stream()
            .filter(lesson -> cseCs2Groups.contains(lesson.getStudentGroup()))
            .collect(Collectors.toList());
            
        // Group lessons by course
        Map<Course, List<Lesson>> lessonsByCourse = cseCs2Lessons.stream()
            .collect(Collectors.groupingBy(Lesson::getCourse));
            
        // Print timetable information
        System.out.println("\n=== CSE-CS 2nd Year Timetable Information ===\n");
        
        // Print course information
        System.out.println("Courses:");
        System.out.println("--------");
        for (Course course : lessonsByCourse.keySet()) {
            System.out.printf("%s (%s)\n", course.getName(), course.getCode());
            System.out.printf("  L: %d, T: %d, P: %d\n", 
                course.getLectureHours(), 
                course.getTutorialHours(), 
                course.getPracticalHours());
                
            // Get lessons for this course
            List<Lesson> courseLessons = lessonsByCourse.get(course);
            
            // Group by session type
            Map<String, List<Lesson>> byType = courseLessons.stream()
                .collect(Collectors.groupingBy(Lesson::getSessionType));
                
            // Print lecture sessions
            if (byType.containsKey("lecture")) {
                System.out.println("  Lectures:");
                byType.get("lecture").forEach(lesson -> 
                    System.out.printf("    - Teacher: %s, Room: %s, Time: %s\n",
                        lesson.getTeacher().getName(),
                        lesson.getRoom() != null ? lesson.getRoom().getName() : "Not assigned",
                        lesson.getTimeSlot() != null ? lesson.getTimeSlot().toString() : "Not assigned"));
            }
            
            // Print tutorial sessions
            if (byType.containsKey("tutorial")) {
                System.out.println("  Tutorials:");
                byType.get("tutorial").forEach(lesson -> 
                    System.out.printf("    - Teacher: %s, Room: %s, Time: %s\n",
                        lesson.getTeacher().getName(),
                        lesson.getRoom() != null ? lesson.getRoom().getName() : "Not assigned",
                        lesson.getTimeSlot() != null ? lesson.getTimeSlot().toString() : "Not assigned"));
            }
            
            // Print lab sessions
            if (byType.containsKey("lab")) {
                System.out.println("  Labs:");
                byType.get("lab").forEach(lesson -> 
                    System.out.printf("    - Teacher: %s, Room: %s, Time: %s, Batch: %s\n",
                        lesson.getTeacher().getName(),
                        lesson.getRoom() != null ? lesson.getRoom().getName() : "Not assigned",
                        lesson.getTimeSlot() != null ? lesson.getTimeSlot().toString() : "Not assigned",
                        lesson.getLabBatch() != null ? lesson.getLabBatch() : "Full class"));
            }
            System.out.println();
        }
        
        // Print teacher information
        System.out.println("\nTeachers and their schedules:");
        System.out.println("----------------------------");
        Map<Teacher, List<Lesson>> lessonsByTeacher = cseCs2Lessons.stream()
            .collect(Collectors.groupingBy(Lesson::getTeacher));
            
        for (Map.Entry<Teacher, List<Lesson>> entry : lessonsByTeacher.entrySet()) {
            Teacher teacher = entry.getKey();
            List<Lesson> teacherLessons = entry.getValue();
            
            System.out.printf("\n%s:\n", teacher.getName());
            System.out.printf("  Total hours: %d\n", 
                teacherLessons.stream()
                    .mapToInt(Lesson::getEffectiveHours)
                    .sum());
                    
            // Group lessons by day
            Map<DayOfWeek, List<Lesson>> byDay = teacherLessons.stream()
                .filter(lesson -> lesson.getTimeSlot() != null)
                .collect(Collectors.groupingBy(lesson -> lesson.getTimeSlot().getDayOfWeek()));
                
            for (DayOfWeek day : DayOfWeek.values()) {
                if (byDay.containsKey(day)) {
                    System.out.printf("  %s:\n", day);
                    byDay.get(day).forEach(lesson -> 
                        System.out.printf("    - %s: %s (%s) in %s\n",
                            lesson.getTimeSlot().getStartTime(),
                            lesson.getCourse().getCode(),
                            lesson.getSessionType(),
                            lesson.getRoom() != null ? lesson.getRoom().getName() : "Not assigned"));
                }
            }
        }
    }
} 