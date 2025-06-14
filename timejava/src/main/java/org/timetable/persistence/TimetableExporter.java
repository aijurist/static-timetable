package org.timetable.persistence;

import org.timetable.domain.Lesson;
import org.timetable.domain.TimetableProblem;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class TimetableExporter {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public static void exportTimetableToCsv(TimetableProblem solution, String outputFile) throws IOException {
        try (FileWriter writer = new FileWriter(outputFile)) {
            // Write header
            writer.write("Teacher,Course,Group,Type,Day,StartTime,EndTime,Room\n");
            
            // Write lessons
            for (Lesson lesson : solution.getLessons()) {
                if (lesson.getTimeSlot() != null && lesson.getRoom() != null) {
                    writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s\n",
                        lesson.getTeacher().getName(),
                        lesson.getCourse().getName(),
                        lesson.getStudentGroup().getName(),
                        lesson.getSessionType(),
                        lesson.getTimeSlot().getDay(),
                        lesson.getTimeSlot().getStartTime().format(TIME_FORMATTER),
                        lesson.getTimeSlot().getEndTime().format(TIME_FORMATTER),
                        lesson.getRoom().getName()
                    ));
                }
            }
        }
    }
    
    public static void exportTeacherTimetables(TimetableProblem solution, String outputDir) throws IOException {
        // Create output directory if it doesn't exist
        Path dirPath = Paths.get(outputDir);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
        
        // Group lessons by teacher
        Map<String, List<Lesson>> lessonsByTeacher = solution.getLessons().stream()
            .filter(lesson -> lesson.getTimeSlot() != null && lesson.getRoom() != null)
            .collect(Collectors.groupingBy(
                lesson -> lesson.getTeacher().getId(),
                TreeMap::new,
                Collectors.toCollection(ArrayList::new)
            ));
        
        // Export timetable for each teacher
        for (Map.Entry<String, List<Lesson>> entry : lessonsByTeacher.entrySet()) {
            String teacherId = entry.getKey();
            List<Lesson> teacherLessons = entry.getValue();
            String teacherName = teacherLessons.get(0).getTeacher().getName().replace(' ', '_');
            
            String fileName = outputDir + "/teacher_" + teacherName + ".csv";
            try (FileWriter writer = new FileWriter(fileName)) {
                // Write header
                writer.write("Day,StartTime,EndTime,Course,Group,Room\n");
                
                // Write lessons
                for (Lesson lesson : teacherLessons) {
                    writer.write(String.format("%s,%s,%s,%s,%s,%s\n",
                        lesson.getTimeSlot().getDay(),
                        lesson.getTimeSlot().getStartTime().format(TIME_FORMATTER),
                        lesson.getTimeSlot().getEndTime().format(TIME_FORMATTER),
                        lesson.getCourse().getName(),
                        lesson.getStudentGroup().getName(),
                        lesson.getRoom().getName()
                    ));
                }
            }
        }
    }
    
    public static void exportStudentGroupTimetables(TimetableProblem solution, String outputDir) throws IOException {
        // Create output directory if it doesn't exist
        Path dirPath = Paths.get(outputDir);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
        
        // Group lessons by student group
        Map<String, List<Lesson>> lessonsByGroup = solution.getLessons().stream()
            .filter(lesson -> lesson.getTimeSlot() != null && lesson.getRoom() != null)
            .collect(Collectors.groupingBy(
                lesson -> lesson.getStudentGroup().getId(),
                TreeMap::new,
                Collectors.toCollection(ArrayList::new)
            ));
        
        // Export timetable for each student group
        for (Map.Entry<String, List<Lesson>> entry : lessonsByGroup.entrySet()) {
            String groupId = entry.getKey();
            List<Lesson> groupLessons = entry.getValue();
            
            String fileName = outputDir + "/group_" + groupId + ".csv";
            try (FileWriter writer = new FileWriter(fileName)) {
                // Write header
                writer.write("Day,StartTime,EndTime,Course,Teacher,Room\n");
                
                // Write lessons
                for (Lesson lesson : groupLessons) {
                    writer.write(String.format("%s,%s,%s,%s,%s,%s\n",
                        lesson.getTimeSlot().getDay(),
                        lesson.getTimeSlot().getStartTime().format(TIME_FORMATTER),
                        lesson.getTimeSlot().getEndTime().format(TIME_FORMATTER),
                        lesson.getCourse().getName(),
                        lesson.getTeacher().getName(),
                        lesson.getRoom().getName()
                    ));
                }
            }
        }
    }
} 