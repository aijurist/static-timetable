package org.timetable.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.timetable.domain.Lesson;
import org.timetable.domain.Teacher;
import org.timetable.domain.StudentGroup;
import org.timetable.domain.TimetableProblem;

import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;

public class TimetableJsonExporter {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public static void exportTimetableToJson(TimetableProblem solution, String outputFile) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        
        // Create the data structure for the visualizer
        Map<String, Object> data = Map.of(
            "teachers", solution.getTeachers().stream()
                .map(t -> Map.of(
                    "id", t.getId(),
                    "name", t.getName()
                ))
                .collect(Collectors.toList()),
            "groups", solution.getStudentGroups().stream()
                .map(g -> Map.of(
                    "id", g.getId(),
                    "name", g.getName()
                ))
                .collect(Collectors.toList()),
            "lessons", solution.getLessons().stream()
                .filter(l -> l.getTimeSlot() != null && l.getRoom() != null)
                .map(l -> {
                    Map<String, Object> lessonMap = new HashMap<>();
                    lessonMap.put("day", l.getTimeSlot().getDayOfWeek().toString());
                    lessonMap.put("startTime", l.getTimeSlot().getStartTime().format(TIME_FORMATTER));
                    lessonMap.put("endTime", l.getTimeSlot().getEndTime().format(TIME_FORMATTER));
                    lessonMap.put("course", l.getCourse().getName());
                    lessonMap.put("teacher", l.getTeacher().getName());
                    lessonMap.put("teacherId", l.getTeacher().getId());
                    lessonMap.put("group", l.getStudentGroup().getName());
                    lessonMap.put("groupId", l.getStudentGroup().getId());
                    lessonMap.put("room", l.getRoom().getName());
                    lessonMap.put("type", l.getSessionType());
                    lessonMap.put("batch", l.getBatch());
                    return lessonMap;
                })
                .collect(Collectors.toList())
        );

        // Write to file
        try (FileWriter writer = new FileWriter(outputFile)) {
            gson.toJson(data, writer);
        }
    }
} 