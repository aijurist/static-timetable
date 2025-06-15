package org.timetable.util;

import java.io.*;
import java.util.*;

/**
 * Simplified timetable exporter for basic domain classes
 */
public class TimetableExporter {
    
    // Simple classes for export
    public static class SimpleStudentGroup {
        private String id;
        private String name;
        private String department;
        
        public SimpleStudentGroup(String id, String name, String department) {
            this.id = id;
            this.name = name;
            this.department = department;
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public String getDepartment() { return department; }
    }
    
    public static class SimpleTeacher {
        private String id;
        private String name;
        
        public SimpleTeacher(String id, String name) {
            this.id = id;
            this.name = name;
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
    }
    
    public static void exportStudentTimetable(SimpleStudentGroup group, Map<String, Map<String, String>> timetable) {
        try {
            File outputDir = new File("output/student_timetables");
            outputDir.mkdirs();
            
            String filename = "timetable_" + group.getDepartment() + "_" + group.getId() + ".html";
            File outputFile = new File(outputDir, filename);
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
                writer.println(generateStudentHTML(group, timetable));
            }
            
            System.out.println("Generated student timetable: " + outputFile.getPath());
        } catch (IOException e) {
            System.err.println("Error exporting student timetable: " + e.getMessage());
        }
    }
    
    public static void exportTeacherTimetable(SimpleTeacher teacher, Map<String, Map<String, String>> timetable) {
        try {
            File outputDir = new File("output/teacher_timetables");
            outputDir.mkdirs();
            
            String filename = "timetable_teacher_" + teacher.getId() + ".html";
            File outputFile = new File(outputDir, filename);
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
                writer.println(generateTeacherHTML(teacher, timetable));
            }
            
            System.out.println("Generated teacher timetable: " + outputFile.getPath());
        } catch (IOException e) {
            System.err.println("Error exporting teacher timetable: " + e.getMessage());
        }
    }
    
    private static String generateStudentHTML(SimpleStudentGroup group, Map<String, Map<String, String>> timetable) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html>\n<head>\n");
        html.append("<title>Timetable - ").append(group.getName()).append("</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; }\n");
        html.append("table { border-collapse: collapse; width: 100%; }\n");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: center; }\n");
        html.append("th { background-color: #f2f2f2; }\n");
        html.append(".lab { background-color: #e8f5e8; }\n");
        html.append(".theory { background-color: #f0f8ff; }\n");
        html.append(".tutorial { background-color: #fff8dc; }\n");
        html.append("</style>\n</head>\n<body>\n");
        
        html.append("<h1>Timetable for ").append(group.getName()).append("</h1>\n");
        html.append("<p>Department: ").append(group.getDepartment()).append("</p>\n");
        
        html.append("<table>\n<tr><th>Time</th>");
        for (String day : TimetableConstants.DAYS) {
            html.append("<th>").append(day).append("</th>");
        }
        html.append("</tr>\n");
        
        // Get all time slots
        Set<String> allTimeSlots = new TreeSet<>();
        for (String day : TimetableConstants.DAYS) {
            if (timetable.containsKey(day)) {
                allTimeSlots.addAll(timetable.get(day).keySet());
            }
        }
        
        for (String timeSlot : allTimeSlots) {
            html.append("<tr><td><strong>").append(timeSlot).append("</strong></td>");
            for (String day : TimetableConstants.DAYS) {
                String classInfo = "";
                if (timetable.containsKey(day) && timetable.get(day).containsKey(timeSlot)) {
                    classInfo = timetable.get(day).get(timeSlot);
                }
                
                String cssClass = getCssClass(classInfo);
                html.append("<td class=\"").append(cssClass).append("\">").append(classInfo).append("</td>");
            }
            html.append("</tr>\n");
        }
        
        html.append("</table>\n</body>\n</html>");
        return html.toString();
    }
    
    private static String generateTeacherHTML(SimpleTeacher teacher, Map<String, Map<String, String>> timetable) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html>\n<head>\n");
        html.append("<title>Teacher Timetable - ").append(teacher.getName()).append("</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; }\n");
        html.append("table { border-collapse: collapse; width: 100%; }\n");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: center; }\n");
        html.append("th { background-color: #f2f2f2; }\n");
        html.append(".lab { background-color: #e8f5e8; }\n");
        html.append(".theory { background-color: #f0f8ff; }\n");
        html.append(".tutorial { background-color: #fff8dc; }\n");
        html.append("</style>\n</head>\n<body>\n");
        
        html.append("<h1>Teacher Timetable - ").append(teacher.getName()).append("</h1>\n");
        
        html.append("<table>\n<tr><th>Time</th>");
        for (String day : TimetableConstants.DAYS) {
            html.append("<th>").append(day).append("</th>");
        }
        html.append("</tr>\n");
        
        // Get all time slots
        Set<String> allTimeSlots = new TreeSet<>();
        for (String day : TimetableConstants.DAYS) {
            if (timetable.containsKey(day)) {
                allTimeSlots.addAll(timetable.get(day).keySet());
            }
        }
        
        for (String timeSlot : allTimeSlots) {
            html.append("<tr><td><strong>").append(timeSlot).append("</strong></td>");
            for (String day : TimetableConstants.DAYS) {
                String classInfo = "";
                if (timetable.containsKey(day) && timetable.get(day).containsKey(timeSlot)) {
                    classInfo = timetable.get(day).get(timeSlot);
                }
                
                String cssClass = getCssClass(classInfo);
                html.append("<td class=\"").append(cssClass).append("\">").append(classInfo).append("</td>");
            }
            html.append("</tr>\n");
        }
        
        html.append("</table>\n</body>\n</html>");
        return html.toString();
    }
    
    private static String getCssClass(String classInfo) {
        if (classInfo.contains("<!-- lab -->")) {
            return "lab";
        } else if (classInfo.contains("<!-- tutorial -->")) {
            return "tutorial";
        } else if (classInfo.contains("<!-- theory -->")) {
            return "theory";
        }
        return "theory"; // default
    }
} 