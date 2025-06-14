package org.timetable.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analyzes timetable data to check for gaps (empty slots) in the schedules.
 */
public class TimetableGapAnalyzer {
    
    private static final String STUDENT_TIMETABLES_DIR = "output/student_timetables";
    private static final String TEACHER_TIMETABLES_DIR = "output/teacher_timetables";
    
    // Pattern for extracting time slots from HTML content
    private static final Pattern TIME_SLOT_PATTERN = Pattern.compile("<th>(\\d{1,2}:\\d{2} - \\d{1,2}:\\d{2})</th>");
    
    // Updated pattern to match day headers in the table
    private static final Pattern DAY_PATTERN = Pattern.compile("<th>(Monday|Tuesday|Wednesday|Thursday|Friday)</th>");
    
    // Pattern to match table rows
    private static final Pattern ROW_PATTERN = Pattern.compile("<tr>\\s*<th>(\\d{1,2}:\\d{2} - \\d{1,2}:\\d{2})</th>(.*?)</tr>", Pattern.DOTALL);
    
    private static final Pattern CELL_PATTERN = Pattern.compile("<td class=\"(\\w+|empty)\">([^<]*(?:<br>[^<]*)*)</td>");
    
    // Patterns for extracting information from filenames
    private static final Pattern STUDENT_PATTERN = Pattern.compile("timetable_([A-Z-]+)_Y(\\d)_([A-Z]).html");
    private static final Pattern TEACHER_PATTERN = Pattern.compile("timetable_teacher_([A-Z-]+)_Teacher_(\\d+).html");
    
    public static void main(String[] args) {
        System.out.println("Analyzing timetable gaps...");
        
        // Let's first check if we can read the files and understand their structure
        checkFiles();
        
        // Analyze student timetables for gaps
        analyzeStudentTimetableGaps();
        
        // Analyze teacher timetables for gaps
        analyzeTeacherTimetableGaps();
        
        // Check for class distribution across days
        analyzeClassDistribution();
    }
    
    /**
     * Checks if files can be read and prints some sample content.
     */
    private static void checkFiles() {
        System.out.println("\n=== FILE CHECK ===");
        
        // Check student timetables
        File studentDir = new File(STUDENT_TIMETABLES_DIR);
        if (studentDir.exists() && studentDir.isDirectory()) {
            File[] files = studentDir.listFiles((d, name) -> name.endsWith(".html"));
            if (files != null && files.length > 0) {
                System.out.println("Found " + files.length + " student timetable files.");
                
                // Print sample filename
                System.out.println("Sample student file: " + files[0].getName());
                
                try {
                    // Read the file
                    String content = new String(Files.readAllBytes(Paths.get(files[0].getAbsolutePath())));
                    System.out.println("Sample content (first 500 chars): " + 
                            content.substring(0, Math.min(500, content.length())));
                    
                    // Extract days from the header row
                    int headerRowStart = content.indexOf("<tr>");
                    int headerRowEnd = content.indexOf("</tr>", headerRowStart);
                    if (headerRowStart >= 0 && headerRowEnd >= 0) {
                        String headerRow = content.substring(headerRowStart, headerRowEnd);
                        System.out.println("\nHeader row: " + headerRow);
                        
                        // Extract days
                        Matcher dayMatcher = DAY_PATTERN.matcher(headerRow);
                        System.out.println("\nDays found:");
                        int dayCount = 0;
                        while (dayMatcher.find()) {
                            System.out.println("- " + dayMatcher.group(1));
                            dayCount++;
                        }
                        System.out.println("Total days found: " + dayCount);
                    }
                    
                    // Extract time slots and rows
                    Matcher rowMatcher = ROW_PATTERN.matcher(content);
                    System.out.println("\nRows found:");
                    int rowCount = 0;
                    while (rowMatcher.find() && rowCount < 3) { // Just show first 3 rows
                        String timeSlot = rowMatcher.group(1);
                        String rowContent = rowMatcher.group(2);
                        System.out.println("- Time slot: " + timeSlot);
                        
                        // Count cells in this row
                        Matcher cellMatcher = CELL_PATTERN.matcher(rowContent);
                        int cellCount = 0;
                        while (cellMatcher.find()) {
                            cellCount++;
                        }
                        System.out.println("  Cells in row: " + cellCount);
                        rowCount++;
                    }
                    System.out.println("Total rows matched: " + rowCount);
                    
                } catch (IOException e) {
                    System.out.println("Error reading file: " + files[0].getName());
                }
            } else {
                System.out.println("No student timetables found.");
            }
        } else {
            System.out.println("Student timetables directory not found.");
        }
    }
    
    /**
     * Analyzes student timetables for gaps.
     */
    private static void analyzeStudentTimetableGaps() {
        System.out.println("\n=== STUDENT TIMETABLE GAP ANALYSIS ===");
        
        File dir = new File(STUDENT_TIMETABLES_DIR);
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("Student timetables directory not found.");
            return;
        }
        
        File[] files = dir.listFiles((d, name) -> name.endsWith(".html"));
        if (files == null || files.length == 0) {
            System.out.println("No student timetables found.");
            return;
        }
        
        // Gap statistics by department and year
        Map<String, Map<String, Integer>> deptYearGaps = new HashMap<>();
        Map<String, Map<String, Integer>> deptYearClasses = new HashMap<>();
        
        // Day-wise gap statistics
        Map<String, Map<String, Integer>> dayGaps = new HashMap<>();
        
        // Process each file
        for (File file : files) {
            String filename = file.getName();
            Matcher matcher = STUDENT_PATTERN.matcher(filename);
            
            if (matcher.find()) {
                String dept = matcher.group(1);
                String year = matcher.group(2);
                String section = matcher.group(3);
                String deptYear = dept + "-Y" + year + "-" + section;
                
                try {
                    String content = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
                    
                    // Extract days from the header row
                    int headerRowStart = content.indexOf("<tr>");
                    int headerRowEnd = content.indexOf("</tr>", headerRowStart);
                    if (headerRowStart >= 0 && headerRowEnd >= 0) {
                        String headerRow = content.substring(headerRowStart, headerRowEnd);
                        
                        // Extract days
                        List<String> days = new ArrayList<>();
                        Matcher dayMatcher = DAY_PATTERN.matcher(headerRow);
                        while (dayMatcher.find()) {
                            days.add(dayMatcher.group(1));
                        }
                        
                        // Process each row (time slot)
                        Matcher rowMatcher = ROW_PATTERN.matcher(content);
                        while (rowMatcher.find()) {
                            String timeSlot = rowMatcher.group(1);
                            String rowContent = rowMatcher.group(2);
                            
                            // Extract cells for this row
                            Matcher cellMatcher = CELL_PATTERN.matcher(rowContent);
                            int cellIndex = 0;
                            
                            while (cellMatcher.find() && cellIndex < days.size()) {
                                String day = days.get(cellIndex);
                                String cellType = cellMatcher.group(1);
                                String cellContent = cellMatcher.group(2).trim();
                                
                                // Update statistics
                                if ("empty".equals(cellType) || cellContent.isEmpty()) {
                                    // Gap
                                    Map<String, Integer> yearGaps = deptYearGaps.computeIfAbsent(dept, k -> new HashMap<>());
                                    yearGaps.put(deptYear, yearGaps.getOrDefault(deptYear, 0) + 1);
                                    
                                    // Update day-wise gap statistics
                                    Map<String, Integer> deptDayGaps = dayGaps.computeIfAbsent(day, k -> new HashMap<>());
                                    deptDayGaps.put(deptYear, deptDayGaps.getOrDefault(deptYear, 0) + 1);
                                } else {
                                    // Class
                                    Map<String, Integer> yearClasses = deptYearClasses.computeIfAbsent(dept, k -> new HashMap<>());
                                    yearClasses.put(deptYear, yearClasses.getOrDefault(deptYear, 0) + 1);
                                }
                                
                                cellIndex++;
                            }
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Error reading file: " + file.getName());
                }
            }
        }
        
        // Print gap statistics by department and year
        System.out.println("\nGap Statistics by Department and Year:");
        System.out.println("------------------------------------");
        System.out.printf("%-15s %-10s %-10s %-10s%n", "Dept-Year", "Classes", "Gaps", "Gap %");
        System.out.println("--------------------------------------------------");
        
        for (String dept : new TreeSet<>(deptYearGaps.keySet())) {
            Map<String, Integer> yearGaps = deptYearGaps.get(dept);
            Map<String, Integer> yearClasses = deptYearClasses.get(dept);
            
            for (String deptYear : new TreeSet<>(yearGaps.keySet())) {
                int gaps = yearGaps.get(deptYear);
                int classes = yearClasses.getOrDefault(deptYear, 0);
                double gapPercentage = (double) gaps / (gaps + classes) * 100;
                
                System.out.printf("%-15s %-10d %-10d %-10.2f%n", 
                        deptYear, 
                        classes,
                        gaps,
                        gapPercentage);
            }
        }
        
        // Print day-wise gap statistics
        System.out.println("\nDay-wise Gap Statistics (Top 10 with most gaps):");
        System.out.println("--------------------------------------------");
        System.out.printf("%-15s %-10s %-10s%n", "Day", "Class", "Gaps");
        System.out.println("----------------------------------");
        
        // Create a list of all day-class combinations
        List<Map.Entry<String, Map.Entry<String, Integer>>> allDayClassGaps = new ArrayList<>();
        
        for (Map.Entry<String, Map<String, Integer>> dayEntry : dayGaps.entrySet()) {
            String day = dayEntry.getKey();
            Map<String, Integer> classGaps = dayEntry.getValue();
            
            for (Map.Entry<String, Integer> classEntry : classGaps.entrySet()) {
                allDayClassGaps.add(new AbstractMap.SimpleEntry<>(day, classEntry));
            }
        }
        
        // Sort by gap count (descending)
        allDayClassGaps.sort((a, b) -> b.getValue().getValue() - a.getValue().getValue());
        
        // Print top 10
        int count = 0;
        for (Map.Entry<String, Map.Entry<String, Integer>> entry : allDayClassGaps) {
            if (count >= 10) break;
            
            String day = entry.getKey();
            String deptYear = entry.getValue().getKey();
            int gaps = entry.getValue().getValue();
            
            System.out.printf("%-15s %-10s %-10d%n", day, deptYear, gaps);
            count++;
        }
    }
    
    /**
     * Analyzes teacher timetables for gaps.
     */
    private static void analyzeTeacherTimetableGaps() {
        // Similar implementation as analyzeStudentTimetableGaps but for teacher timetables
        System.out.println("\n=== TEACHER TIMETABLE GAP ANALYSIS ===");
        
        File dir = new File(TEACHER_TIMETABLES_DIR);
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("Teacher timetables directory not found.");
            return;
        }
        
        File[] files = dir.listFiles((d, name) -> name.endsWith(".html"));
        if (files == null || files.length == 0) {
            System.out.println("No teacher timetables found.");
            return;
        }
        
        // Gap statistics by teacher
        Map<String, Integer> teacherGaps = new HashMap<>();
        Map<String, Integer> teacherClasses = new HashMap<>();
        
        // Day-wise gap statistics
        Map<String, Map<String, Integer>> dayGaps = new HashMap<>();
        
        // Process each file
        for (File file : files) {
            String filename = file.getName();
            Matcher matcher = TEACHER_PATTERN.matcher(filename);
            
            if (matcher.find()) {
                String dept = matcher.group(1);
                String teacherNumber = matcher.group(2);
                String teacherId = dept + "_" + teacherNumber;
                
                try {
                    String content = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
                    
                    // Extract days from the header row
                    int headerRowStart = content.indexOf("<tr>");
                    int headerRowEnd = content.indexOf("</tr>", headerRowStart);
                    if (headerRowStart >= 0 && headerRowEnd >= 0) {
                        String headerRow = content.substring(headerRowStart, headerRowEnd);
                        
                        // Extract days
                        List<String> days = new ArrayList<>();
                        Matcher dayMatcher = DAY_PATTERN.matcher(headerRow);
                        while (dayMatcher.find()) {
                            days.add(dayMatcher.group(1));
                        }
                        
                        // Process each row (time slot)
                        Matcher rowMatcher = ROW_PATTERN.matcher(content);
                        while (rowMatcher.find()) {
                            String timeSlot = rowMatcher.group(1);
                            String rowContent = rowMatcher.group(2);
                            
                            // Extract cells for this row
                            Matcher cellMatcher = CELL_PATTERN.matcher(rowContent);
                            int cellIndex = 0;
                            
                            while (cellMatcher.find() && cellIndex < days.size()) {
                                String day = days.get(cellIndex);
                                String cellType = cellMatcher.group(1);
                                String cellContent = cellMatcher.group(2).trim();
                                
                                // Update statistics
                                if ("empty".equals(cellType) || cellContent.isEmpty()) {
                                    // Gap
                                    teacherGaps.put(teacherId, teacherGaps.getOrDefault(teacherId, 0) + 1);
                                    
                                    // Update day-wise gap statistics
                                    Map<String, Integer> teacherDayGaps = dayGaps.computeIfAbsent(day, k -> new HashMap<>());
                                    teacherDayGaps.put(teacherId, teacherDayGaps.getOrDefault(teacherId, 0) + 1);
                                } else {
                                    // Class
                                    teacherClasses.put(teacherId, teacherClasses.getOrDefault(teacherId, 0) + 1);
                                }
                                
                                cellIndex++;
                            }
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Error reading file: " + file.getName());
                }
            }
        }
        
        // Print gap statistics by teacher
        System.out.println("\nGap Statistics by Teacher:");
        System.out.println("-------------------------");
        System.out.printf("%-15s %-10s %-10s %-10s%n", "Teacher", "Classes", "Gaps", "Gap %");
        System.out.println("--------------------------------------------------");
        
        for (String teacherId : new TreeSet<>(teacherGaps.keySet())) {
            int gaps = teacherGaps.get(teacherId);
            int classes = teacherClasses.getOrDefault(teacherId, 0);
            double gapPercentage = (double) gaps / (gaps + classes) * 100;
            
            System.out.printf("%-15s %-10d %-10d %-10.2f%n", 
                    teacherId, 
                    classes,
                    gaps,
                    gapPercentage);
        }
        
        // Calculate average gap percentage
        double totalGapPercentage = 0;
        for (String teacherId : teacherGaps.keySet()) {
            int gaps = teacherGaps.get(teacherId);
            int classes = teacherClasses.getOrDefault(teacherId, 0);
            double gapPercentage = (double) gaps / (gaps + classes) * 100;
            totalGapPercentage += gapPercentage;
        }
        
        double averageGapPercentage = totalGapPercentage / teacherGaps.size();
        System.out.printf("\nAverage Gap Percentage: %.2f%%\n", averageGapPercentage);
    }
    
    /**
     * Analyzes class distribution across days.
     */
    private static void analyzeClassDistribution() {
        System.out.println("\n=== CLASS DISTRIBUTION ANALYSIS ===");
        
        // Student class distribution
        Map<String, Map<String, Integer>> studentDayClasses = new HashMap<>();
        Map<String, Integer> studentDayTotal = new HashMap<>();
        
        // Teacher class distribution
        Map<String, Map<String, Integer>> teacherDayClasses = new HashMap<>();
        Map<String, Integer> teacherDayTotal = new HashMap<>();
        
        // Process student timetables
        processFilesForClassDistribution(true, studentDayClasses, studentDayTotal);
        
        // Process teacher timetables
        processFilesForClassDistribution(false, teacherDayClasses, teacherDayTotal);
        
        // Print student class distribution
        System.out.println("\nStudent Class Distribution by Day:");
        System.out.println("--------------------------------");
        System.out.printf("%-15s %-15s %-10s %-10s%n", "Day", "Department", "Classes", "Percentage");
        System.out.println("--------------------------------------------------");
        
        printClassDistribution(studentDayClasses, studentDayTotal);
        
        // Print teacher class distribution
        System.out.println("\nTeacher Class Distribution by Day:");
        System.out.println("--------------------------------");
        System.out.printf("%-15s %-15s %-10s %-10s%n", "Day", "Department", "Classes", "Percentage");
        System.out.println("--------------------------------------------------");
        
        printClassDistribution(teacherDayClasses, teacherDayTotal);
    }
    
    /**
     * Processes files for class distribution analysis.
     */
    private static void processFilesForClassDistribution(boolean isStudent, 
            Map<String, Map<String, Integer>> dayClasses, Map<String, Integer> dayTotal) {
        
        String dirPath = isStudent ? STUDENT_TIMETABLES_DIR : TEACHER_TIMETABLES_DIR;
        File dir = new File(dirPath);
        
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println(isStudent ? "Student timetables directory not found." : "Teacher timetables directory not found.");
            return;
        }
        
        File[] files = dir.listFiles((d, name) -> name.endsWith(".html"));
        if (files == null || files.length == 0) {
            System.out.println(isStudent ? "No student timetables found." : "No teacher timetables found.");
            return;
        }
        
        // Process each file
        for (File file : files) {
            try {
                String content = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
                
                // Extract department
                String dept;
                if (isStudent) {
                    Matcher matcher = STUDENT_PATTERN.matcher(file.getName());
                    if (!matcher.find()) continue;
                    dept = matcher.group(1);
                } else {
                    Matcher matcher = TEACHER_PATTERN.matcher(file.getName());
                    if (!matcher.find()) continue;
                    dept = matcher.group(1);
                }
                
                // Extract days from the header row
                int headerRowStart = content.indexOf("<tr>");
                int headerRowEnd = content.indexOf("</tr>", headerRowStart);
                if (headerRowStart >= 0 && headerRowEnd >= 0) {
                    String headerRow = content.substring(headerRowStart, headerRowEnd);
                    
                    // Extract days
                    List<String> days = new ArrayList<>();
                    Matcher dayMatcher = DAY_PATTERN.matcher(headerRow);
                    while (dayMatcher.find()) {
                        days.add(dayMatcher.group(1));
                    }
                    
                    // Process each row (time slot)
                    Matcher rowMatcher = ROW_PATTERN.matcher(content);
                    while (rowMatcher.find()) {
                        String timeSlot = rowMatcher.group(1);
                        String rowContent = rowMatcher.group(2);
                        
                        // Extract cells for this row
                        Matcher cellMatcher = CELL_PATTERN.matcher(rowContent);
                        int cellIndex = 0;
                        
                        while (cellMatcher.find() && cellIndex < days.size()) {
                            String day = days.get(cellIndex);
                            String cellType = cellMatcher.group(1);
                            String cellContent = cellMatcher.group(2).trim();
                            
                            // Update statistics
                            if (!"empty".equals(cellType) && !cellContent.isEmpty()) {
                                // Class
                                Map<String, Integer> deptClasses = dayClasses.computeIfAbsent(day, k -> new HashMap<>());
                                deptClasses.put(dept, deptClasses.getOrDefault(dept, 0) + 1);
                                
                                // Update day total
                                dayTotal.put(day, dayTotal.getOrDefault(day, 0) + 1);
                            }
                            
                            cellIndex++;
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Error reading file: " + file.getName());
            }
        }
    }
    
    /**
     * Prints class distribution statistics.
     */
    private static void printClassDistribution(Map<String, Map<String, Integer>> dayClasses, Map<String, Integer> dayTotal) {
        int grandTotal = dayTotal.values().stream().mapToInt(Integer::intValue).sum();
        
        for (String day : Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")) {
            Map<String, Integer> deptClasses = dayClasses.getOrDefault(day, Collections.emptyMap());
            int daySum = dayTotal.getOrDefault(day, 0);
            double dayPercentage = grandTotal > 0 ? (double) daySum / grandTotal * 100 : 0;
            
            // Print day total
            System.out.printf("%-15s %-15s %-10d %-10.2f%n", day, "ALL", daySum, dayPercentage);
            
            // Print department breakdown
            for (String dept : new TreeSet<>(deptClasses.keySet())) {
                int classes = deptClasses.get(dept);
                double percentage = daySum > 0 ? (double) classes / daySum * 100 : 0;
                
                System.out.printf("%-15s %-15s %-10d %-10.2f%n", "", dept, classes, percentage);
            }
            
            System.out.println("--------------------------------------------------");
        }
    }
}