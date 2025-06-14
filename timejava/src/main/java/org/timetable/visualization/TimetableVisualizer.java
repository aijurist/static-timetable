package org.timetable.visualization;

import org.timetable.domain.*;
import org.timetable.util.TimetableConstants;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;

public class TimetableVisualizer {

    // Constants for visualization
    private static final int CELL_WIDTH = 180;
    private static final int CELL_HEIGHT = 80;
    private static final int HEADER_HEIGHT = 50;
    private static final int TIME_COLUMN_WIDTH = 100;
    private static final int MARGIN = 20;
    private static final Font HEADER_FONT = new Font("Arial", Font.BOLD, 14);
    private static final Font CELL_FONT = new Font("Arial", Font.PLAIN, 12);
    private static final Font TITLE_FONT = new Font("Arial", Font.BOLD, 20);
    
    // Colors
    private static final Color HEADER_BG = new Color(60, 60, 60);
    private static final Color HEADER_TEXT = Color.WHITE;
    private static final Color THEORY_BG = new Color(230, 230, 255);
    private static final Color LAB_BG = new Color(255, 230, 230);
    private static final Color TUTORIAL_BG = new Color(230, 255, 230);
    private static final Color BORDER_COLOR = new Color(180, 180, 180);
    private static final Color TEXT_COLOR = Color.BLACK;
    
    private static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
    private static final String[] TIMES = {
        "8:00-8:50", "9:00-9:50", "10:00-10:50", "11:00-11:50", "12:00-12:50",
        "13:00-13:50", "14:00-14:50", "15:00-15:50", "16:00-16:50", "17:00-17:50", "18:00-18:50"
    };
    
    /**
     * Generate timetables for all student groups.
     * 
     * @param solution The solved timetable problem
     * @param outputDir The directory to save the timetables
     */
    public void generateStudentTimetables(TimetableProblem solution, String outputDir) {
        List<StudentGroup> studentGroups = solution.getStudentGroups();
        List<Lesson> lessons = solution.getLessons();
        
        // Create output directory if it doesn't exist
        File directory = new File(outputDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        
        // Generate a timetable for each student group
        for (StudentGroup group : studentGroups) {
            List<Lesson> groupLessons = new ArrayList<>();
            for (Lesson lesson : lessons) {
                if (lesson.getStudentGroup() != null && lesson.getStudentGroup().equals(group)) {
                    groupLessons.add(lesson);
                }
            }
            
            String filename = getStudentGroupFilename(group);
            generateTimetableHtml(group.getName(), groupLessons, new File(directory, filename));
        }
    }
    
    /**
     * Generate timetables for all teachers.
     * 
     * @param solution The solved timetable problem
     * @param outputDir The directory to save the timetables
     */
    public void generateTeacherTimetables(TimetableProblem solution, String outputDir) {
        List<Teacher> teachers = solution.getTeachers();
        List<Lesson> lessons = solution.getLessons();
        
        // Create output directory if it doesn't exist
        File directory = new File(outputDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        
        // Generate a timetable for each teacher
        for (Teacher teacher : teachers) {
            List<Lesson> teacherLessons = new ArrayList<>();
            for (Lesson lesson : lessons) {
                if (lesson.getTeacher() != null && lesson.getTeacher().equals(teacher)) {
                    teacherLessons.add(lesson);
                }
            }
            
            String filename = getTeacherFilename(teacher);
            generateTimetableHtml(teacher.getName(), teacherLessons, new File(directory, filename));
        }
    }
    
    /**
     * Generate a filename for a student group timetable.
     */
    private String getStudentGroupFilename(StudentGroup group) {
        String deptCode = group.getDepartment() != null ? group.getDepartment().name() : "UNKNOWN";
        return "timetable_" + deptCode + "_Y" + group.getYear() + "_" + group.getSection() + ".html";
    }
    
    /**
     * Generate a filename for a teacher timetable.
     */
    private String getTeacherFilename(Teacher teacher) {
        return "timetable_teacher_" + teacher.getName().replaceAll("\\s+", "_") + ".html";
    }
    
    /**
     * Generate an HTML timetable for the given lessons.
     * 
     * @param title The title of the timetable
     * @param lessons The lessons to include in the timetable
     * @param outputFile The file to save the timetable to
     */
    private void generateTimetableHtml(String title, List<Lesson> lessons, File outputFile) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write("<!DOCTYPE html>\n");
            writer.write("<html>\n");
            writer.write("<head>\n");
            writer.write("  <title>Timetable for " + title + "</title>\n");
            writer.write("  <style>\n");
            writer.write("    table { border-collapse: collapse; width: 100%; }\n");
            writer.write("    th, td { border: 1px solid #ddd; padding: 8px; text-align: center; }\n");
            writer.write("    th { background-color: #f2f2f2; }\n");
            writer.write("    .lecture { background-color: #e6f7ff; }\n");
            writer.write("    .lab { background-color: #ffe6e6; }\n");
            writer.write("    .tutorial { background-color: #e6ffe6; }\n");
            writer.write("  </style>\n");
            writer.write("</head>\n");
            writer.write("<body>\n");
            writer.write("  <h1>Timetable for " + title + "</h1>\n");
            writer.write("  <table>\n");
            writer.write("    <tr>\n");
            writer.write("      <th>Time</th>\n");
            
            // Write day headers
            for (String day : DAYS) {
                writer.write("      <th>" + day + "</th>\n");
            }
            writer.write("    </tr>\n");
            
            // Write time slots
            for (String time : TIMES) {
                writer.write("    <tr>\n");
                writer.write("      <td>" + time + "</td>\n");
                
                // For each day
                for (int day = 0; day < DAYS.length; day++) {
                    Lesson lessonAtTimeSlot = findLessonAtTimeSlot(lessons, day, time);
                    if (lessonAtTimeSlot != null) {
                        String cellClass = lessonAtTimeSlot.getLessonType().toLowerCase();
                        writer.write("      <td class=\"" + cellClass + "\">\n");
                        writer.write("        " + lessonAtTimeSlot.getCourse().getName() + "<br>\n");
                        writer.write("        " + lessonAtTimeSlot.getTeacher().getName() + "<br>\n");
                        writer.write("        " + lessonAtTimeSlot.getRoom().getName() + "<br>\n");
                        writer.write("        (" + lessonAtTimeSlot.getLessonType() + ")\n");
                        writer.write("      </td>\n");
                    } else {
                        writer.write("      <td></td>\n");
                    }
                }
                
                writer.write("    </tr>\n");
            }
            
            writer.write("  </table>\n");
            writer.write("</body>\n");
            writer.write("</html>\n");
        } catch (IOException e) {
            System.err.println("Error writing timetable to " + outputFile + ": " + e.getMessage());
        }
    }
    
    /**
     * Find a lesson at the given day and time.
     */
    private Lesson findLessonAtTimeSlot(List<Lesson> lessons, int day, String timeSlot) {
        for (Lesson lesson : lessons) {
            TimeSlot lessonSlot = lesson.getTimeSlot();
            if (lessonSlot != null && lessonSlot.getDay() == day) {
                String lessonTimeStr = lessonSlot.getStartTimeStr() + "-" + lessonSlot.getEndTimeStr();
                if (lessonTimeStr.equals(timeSlot)) {
                    return lesson;
                }
            }
        }
        return null;
    }
    
    /**
     * Generate a summary of the timetable solution.
     * 
     * @param solution The solved timetable problem
     * @param outputFile The file to save the summary to
     */
    public void generateSolutionSummary(TimetableProblem solution, File outputFile) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write("Timetable Solution Summary\n");
            writer.write("=========================\n\n");
            
            writer.write("Score: " + solution.getScore() + "\n\n");
            
            writer.write("Statistics:\n");
            writer.write("- Total lessons: " + solution.getLessons().size() + "\n");
            writer.write("- Total student groups: " + solution.getStudentGroups().size() + "\n");
            writer.write("- Total teachers: " + solution.getTeachers().size() + "\n");
            writer.write("- Total rooms: " + solution.getRooms().size() + "\n\n");
            
            // Group lessons by day
            Map<Integer, List<TimeSlot>> timeSlotsByDay = new HashMap<>();
            for (TimeSlot slot : solution.getTimeSlots()) {
                timeSlotsByDay.computeIfAbsent(slot.getDay(), k -> new ArrayList<>()).add(slot);
            }
            
            // Write lessons by day
            for (int day = 0; day < DAYS.length; day++) {
                writer.write(DAYS[day] + ":\n");
                List<Lesson> lessonsOnDay = new ArrayList<>();
                for (Lesson lesson : solution.getLessons()) {
                    if (lesson.getTimeSlot() != null && lesson.getTimeSlot().getDay() == day) {
                        lessonsOnDay.add(lesson);
                    }
                }
                
                // Sort by start time
                lessonsOnDay.sort(Comparator.comparing(lesson -> 
                        lesson.getTimeSlot().getStartTime()));
                
                for (Lesson lesson : lessonsOnDay) {
                    TimeSlot slot = lesson.getTimeSlot();
                    String timeStr = slot.getStartTimeStr() + "-" + slot.getEndTimeStr();
                    writer.write("  " + timeStr + ": " + 
                            lesson.getCourse().getName() + " (" + lesson.getLessonType() + ") - " +
                            lesson.getTeacher().getName() + " - " +
                            lesson.getStudentGroup().getName() + " - " +
                            lesson.getRoom().getName() + "\n");
                }
                writer.write("\n");
            }
        } catch (IOException e) {
            System.err.println("Error writing solution summary to " + outputFile + ": " + e.getMessage());
        }
    }
    
    /**
     * Generate timetable PNG for a student group
     */
    public static void generateStudentTimetable(TimetableProblem solution, StudentGroup group, String outputDir) {
        try {
            // Get all lessons for this student group
            List<Lesson> groupLessons = new ArrayList<>();
            for (Lesson lesson : solution.getLessons()) {
                if (lesson.getStudentGroup().equals(group) && lesson.getTimeSlot() != null) {
                    groupLessons.add(lesson);
                }
            }
            
            // Get all time slots organized by day and time
            Map<DayOfWeek, List<TimeSlot>> timeSlotsByDay = organizeTimeSlotsByDay(solution.getTimeSlots());
            
            // Create the image
            BufferedImage image = createTimetableImage(group.toString(), timeSlotsByDay, groupLessons);
            
            // Save the image
            File outputFile = new File(outputDir + "/student_" + group.getId() + ".png");
            outputFile.getParentFile().mkdirs();
            ImageIO.write(image, "PNG", outputFile);
            
            System.out.println("Generated timetable for " + group);
        } catch (IOException e) {
            System.err.println("Error generating timetable for " + group + ": " + e.getMessage());
        }
    }
    
    /**
     * Generate timetable PNG for a teacher
     */
    public static void generateTeacherTimetable(TimetableProblem solution, Teacher teacher, String outputDir) {
        try {
            // Get all lessons for this teacher
            List<Lesson> teacherLessons = new ArrayList<>();
            for (Lesson lesson : solution.getLessons()) {
                if (lesson.getTeacher().equals(teacher) && lesson.getTimeSlot() != null) {
                    teacherLessons.add(lesson);
                }
            }
            
            // Get all time slots organized by day and time
            Map<DayOfWeek, List<TimeSlot>> timeSlotsByDay = organizeTimeSlotsByDay(solution.getTimeSlots());
            
            // Create the image
            BufferedImage image = createTimetableImage(teacher.getName(), timeSlotsByDay, teacherLessons);
            
            // Save the image
            File outputFile = new File(outputDir + "/teacher_" + teacher.getId() + ".png");
            outputFile.getParentFile().mkdirs();
            ImageIO.write(image, "PNG", outputFile);
            
            System.out.println("Generated timetable for " + teacher.getName());
        } catch (IOException e) {
            System.err.println("Error generating timetable for " + teacher.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Generate a dashboard with all timetables
     */
    public static void generateDashboard(TimetableProblem solution, String outputDir) {
        try {
            // Create department-wise organization of student groups
            Map<String, List<StudentGroup>> groupsByDept = new HashMap<>();
            for (StudentGroup group : solution.getStudentGroups()) {
                String deptCode = group.getDepartment() != null ? group.getDepartment().getCode() : "UNKNOWN";
                groupsByDept.computeIfAbsent(deptCode, k -> new ArrayList<>()).add(group);
            }
            
            // Sort student groups within each department
            for (List<StudentGroup> groups : groupsByDept.values()) {
                groups.sort(Comparator.comparing(StudentGroup::getId));
            }
            
            // Create the dashboard HTML
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n");
            html.append("<html lang=\"en\">\n");
            html.append("<head>\n");
            html.append("  <meta charset=\"UTF-8\">\n");
            html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            html.append("  <title>Timetable Dashboard</title>\n");
            html.append("  <style>\n");
            html.append("    body { font-family: Arial, sans-serif; margin: 20px; }\n");
            html.append("    h1 { color: #333; }\n");
            html.append("    h2 { color: #555; margin-top: 30px; }\n");
            html.append("    .timetable-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 20px; }\n");
            html.append("    .timetable-card { border: 1px solid #ddd; border-radius: 8px; padding: 15px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n");
            html.append("    .timetable-card h3 { margin-top: 0; color: #444; }\n");
            html.append("    .timetable-card img { max-width: 100%; height: auto; }\n");
            html.append("    .dept-section { margin-bottom: 40px; }\n");
            html.append("  </style>\n");
            html.append("</head>\n");
            html.append("<body>\n");
            html.append("  <h1>Timetable Dashboard</h1>\n");
            
            // Add department sections
            for (Map.Entry<String, List<StudentGroup>> entry : groupsByDept.entrySet()) {
                String deptCode = entry.getKey();
                List<StudentGroup> groups = entry.getValue();
                
                html.append("  <div class=\"dept-section\">\n");
                html.append("    <h2>").append(deptCode).append("</h2>\n");
                html.append("    <div class=\"timetable-grid\">\n");
                
                for (StudentGroup group : groups) {
                    html.append("      <div class=\"timetable-card\">\n");
                    html.append("        <h3>").append(group).append("</h3>\n");
                    html.append("        <img src=\"../student_timetables/student_").append(group.getId()).append(".png\" alt=\"Timetable for ").append(group).append("\">\n");
                    html.append("      </div>\n");
                }
                
                html.append("    </div>\n");
                html.append("  </div>\n");
            }
            
            html.append("</body>\n");
            html.append("</html>\n");
            
            // Write the HTML file
            File outputFile = new File(outputDir + "/index.html");
            outputFile.getParentFile().mkdirs();
            java.nio.file.Files.write(outputFile.toPath(), html.toString().getBytes());
            
            System.out.println("Generated dashboard at " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error generating dashboard: " + e.getMessage());
        }
    }
    
    /**
     * Create the timetable image
     */
    private static BufferedImage createTimetableImage(String title, Map<DayOfWeek, List<TimeSlot>> timeSlotsByDay, List<Lesson> lessons) {
        // Get sorted days
        List<DayOfWeek> days = new ArrayList<>(timeSlotsByDay.keySet());
        Collections.sort(days);
        
        // Get all unique time slots
        Set<String> uniqueTimeSlots = new HashSet<>();
        for (List<TimeSlot> slots : timeSlotsByDay.values()) {
            for (TimeSlot slot : slots) {
                uniqueTimeSlots.add(slot.getTimeString());
            }
        }
        List<String> sortedTimeSlots = new ArrayList<>(uniqueTimeSlots);
        Collections.sort(sortedTimeSlots);
        
        // Calculate image dimensions
        int width = TIME_COLUMN_WIDTH + (days.size() * CELL_WIDTH) + (2 * MARGIN);
        int height = HEADER_HEIGHT + (sortedTimeSlots.size() * CELL_HEIGHT) + HEADER_HEIGHT + (2 * MARGIN);
        
        // Create the image
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        
        // Enable anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Fill background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        
        // Draw title
        g2d.setFont(TITLE_FONT);
        g2d.setColor(TEXT_COLOR);
        FontMetrics titleMetrics = g2d.getFontMetrics();
        int titleWidth = titleMetrics.stringWidth(title);
        g2d.drawString(title, (width - titleWidth) / 2, MARGIN + titleMetrics.getAscent());
        
        // Draw day headers
        g2d.setFont(HEADER_FONT);
        g2d.setColor(HEADER_BG);
        for (int i = 0; i < days.size(); i++) {
            int x = MARGIN + TIME_COLUMN_WIDTH + (i * CELL_WIDTH);
            int y = MARGIN + HEADER_HEIGHT;
            g2d.fillRect(x, y, CELL_WIDTH, HEADER_HEIGHT);
            
            // Day name
            String dayName = days.get(i).getDisplayName(TextStyle.FULL, Locale.getDefault());
            g2d.setColor(HEADER_TEXT);
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(dayName);
            g2d.drawString(dayName, x + (CELL_WIDTH - textWidth) / 2, y + ((HEADER_HEIGHT + fm.getAscent()) / 2) - 2);
            g2d.setColor(HEADER_BG);
        }
        
        // Draw time column header
        g2d.setColor(HEADER_BG);
        g2d.fillRect(MARGIN, MARGIN + HEADER_HEIGHT, TIME_COLUMN_WIDTH, HEADER_HEIGHT);
        g2d.setColor(HEADER_TEXT);
        g2d.drawString("Time", MARGIN + 10, MARGIN + HEADER_HEIGHT + 30);
        
        // Draw time slots and grid
        g2d.setFont(CELL_FONT);
        for (int i = 0; i < sortedTimeSlots.size(); i++) {
            String timeSlot = sortedTimeSlots.get(i);
            int y = MARGIN + HEADER_HEIGHT + HEADER_HEIGHT + (i * CELL_HEIGHT);
            
            // Time label
            g2d.setColor(TEXT_COLOR);
            g2d.drawString(timeSlot, MARGIN + 10, y + 30);
            
            // Grid lines
            g2d.setColor(BORDER_COLOR);
            g2d.drawLine(MARGIN, y, width - MARGIN, y);
            
            // Draw cells for each day
            for (int j = 0; j < days.size(); j++) {
                DayOfWeek day = days.get(j);
                int x = MARGIN + TIME_COLUMN_WIDTH + (j * CELL_WIDTH);
                
                // Draw cell border
                g2d.drawRect(x, y, CELL_WIDTH, CELL_HEIGHT);
                
                // Find lesson for this day and time
                Lesson lessonForCell = null;
                for (Lesson lesson : lessons) {
                    TimeSlot lessonSlot = lesson.getTimeSlot();
                    if (lessonSlot != null && lessonSlot.getDay() == day && lessonSlot.getTimeString().equals(timeSlot)) {
                        lessonForCell = lesson;
                        break;
                    }
                }
                
                // Draw lesson if found
                if (lessonForCell != null) {
                    // Fill cell with appropriate color
                    if ("lab".equals(lessonForCell.getLessonType())) {
                        g2d.setColor(LAB_BG);
                    } else if ("tutorial".equals(lessonForCell.getLessonType())) {
                        g2d.setColor(TUTORIAL_BG);
                    } else {
                        g2d.setColor(THEORY_BG);
                    }
                    g2d.fillRect(x + 1, y + 1, CELL_WIDTH - 2, CELL_HEIGHT - 2);
                    
                    // Draw lesson details
                    g2d.setColor(TEXT_COLOR);
                    String courseCode = lessonForCell.getCourse().getCourseCode();
                    String roomName = lessonForCell.getRoom() != null ? lessonForCell.getRoom().getName() : "TBA";
                    String teacherName = lessonForCell.getTeacher().getName();
                    String lessonType = lessonForCell.getLessonType();
                    
                    g2d.drawString(courseCode, x + 5, y + 20);
                    g2d.drawString(roomName, x + 5, y + 40);
                    g2d.drawString(teacherName, x + 5, y + 60);
                    g2d.drawString("(" + lessonType + ")", x + CELL_WIDTH - 60, y + 60);
                }
            }
        }
        
        // Draw vertical grid lines
        g2d.setColor(BORDER_COLOR);
        g2d.drawLine(MARGIN + TIME_COLUMN_WIDTH, MARGIN + HEADER_HEIGHT, MARGIN + TIME_COLUMN_WIDTH, height - MARGIN);
        for (int i = 0; i <= days.size(); i++) {
            int x = MARGIN + TIME_COLUMN_WIDTH + (i * CELL_WIDTH);
            g2d.drawLine(x, MARGIN + HEADER_HEIGHT, x, height - MARGIN);
        }
        
        // Draw horizontal grid lines
        for (int i = 0; i <= sortedTimeSlots.size(); i++) {
            int y = MARGIN + HEADER_HEIGHT + HEADER_HEIGHT + (i * CELL_HEIGHT);
            g2d.drawLine(MARGIN, y, width - MARGIN, y);
        }
        
        g2d.dispose();
        return image;
    }
    
    /**
     * Organize time slots by day
     */
    private static Map<DayOfWeek, List<TimeSlot>> organizeTimeSlotsByDay(List<TimeSlot> timeSlots) {
        Map<DayOfWeek, List<TimeSlot>> timeSlotsByDay = new HashMap<>();
        
        for (TimeSlot slot : timeSlots) {
            timeSlotsByDay.computeIfAbsent(slot.getDay(), k -> new ArrayList<>()).add(slot);
        }
        
        // Sort time slots within each day
        for (List<TimeSlot> daySlots : timeSlotsByDay.values()) {
            daySlots.sort(Comparator.comparing(TimeSlot::getStartTime));
        }
        
        return timeSlotsByDay;
    }
} 