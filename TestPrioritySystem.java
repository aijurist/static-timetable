import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

// Simplified version of CourseLabMappingUtil for testing
class SimpleCourseLabMappingUtil {
    private static final String DEFAULT_PATH = "data/config/course_lab_mapping.csv";
    private static final Map<String, List<String>> COURSE_TO_PRIORITY_LABS = new HashMap<>();
    private static boolean loaded = false;

    public static List<String> getPriorityOrderedLabs(String courseCode) {
        ensureLoaded();
        return new ArrayList<>(COURSE_TO_PRIORITY_LABS.getOrDefault(courseCode, Collections.emptyList()));
    }

    public static int getLabPriority(String courseCode, String roomDescription) {
        if (roomDescription == null || courseCode == null) return -1;
        List<String> priorityLabs = getPriorityOrderedLabs(courseCode);
        int index = priorityLabs.indexOf(roomDescription);
        return index == -1 ? -1 : index + 1;
    }

    public static int getPriorityPenalty(String courseCode, String roomDescription) {
        int priority = getLabPriority(courseCode, roomDescription);
        if (priority == -1) {
            return 1000; // Severe penalty for disallowed assignment
        }
        return priority - 1; // Convert priority 1,2,3 to penalty 0,1,2
    }

    public static boolean isCoreLabCourse(String courseCode) {
        ensureLoaded();
        return COURSE_TO_PRIORITY_LABS.containsKey(courseCode);
    }

    private static void ensureLoaded() {
        if (loaded) return;
        try {
            if (!Files.exists(Paths.get(DEFAULT_PATH))) {
                System.out.println("WARNING: course_lab_mapping.csv not found at " + DEFAULT_PATH);
                loaded = true;
                return;
            }
            
            // Simple CSV parsing
            try (Scanner scanner = new Scanner(Paths.get(DEFAULT_PATH))) {
                boolean isHeader = true;
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine().trim();
                    if (isHeader) {
                        isHeader = false;
                        continue;
                    }
                    if (line.isEmpty()) continue;
                    
                    String[] parts = line.split(",");
                    if (parts.length >= 5) {
                        String code = parts[0].trim();
                        List<String> labs = new ArrayList<>();
                        
                        // Add labs in priority order: lab_1, lab_2, lab_3
                        for (int i = 4; i <= 6 && i < parts.length; i++) {
                            String labName = parts[i].trim();
                            if (!labName.isEmpty()) {
                                labs.add(labName);
                            }
                        }
                        
                        if (!labs.isEmpty()) {
                            COURSE_TO_PRIORITY_LABS.put(code, labs);
                            System.out.println("Loaded course " + code + " with priority labs: " + labs);
                        }
                    }
                }
                System.out.println("Loaded course-lab mapping for " + COURSE_TO_PRIORITY_LABS.size() + " courses with priority ordering.");
            }
        } catch (Exception e) {
            System.err.println("Failed to load course_lab_mapping.csv: " + e.getMessage());
        } finally {
            loaded = true;
        }
    }
}

public class TestPrioritySystem {
    public static void main(String[] args) {
        System.out.println("=== Testing Core Lab Priority System ===");
        
        // Test a few sample courses from the CSV
        String[] testCourses = {
            "AV23121", // AVIONICS INTEGRATION LABORATORY - should have Avionics Lab as priority 1
            "AE23333", // Aero Engineering Thermodynamics - should have Thermal Lab 1, Thermal Lab 2
            "GE23121"  // Engineering Practices – Civil and Mechanical - should have 3 priorities
        };
        
        for (String courseCode : testCourses) {
            System.out.println("\n--- Testing Course: " + courseCode + " ---");
            
            if (SimpleCourseLabMappingUtil.isCoreLabCourse(courseCode)) {
                List<String> priorityLabs = SimpleCourseLabMappingUtil.getPriorityOrderedLabs(courseCode);
                System.out.println("Priority Labs: " + priorityLabs);
                
                for (int i = 0; i < priorityLabs.size(); i++) {
                    String lab = priorityLabs.get(i);
                    int priority = SimpleCourseLabMappingUtil.getLabPriority(courseCode, lab);
                    int penalty = SimpleCourseLabMappingUtil.getPriorityPenalty(courseCode, lab);
                    System.out.println("  Lab " + (i+1) + ": " + lab + 
                                     " (Priority: " + priority + ", Penalty: " + penalty + ")");
                }
                
                // Test with an invalid lab
                int invalidPenalty = SimpleCourseLabMappingUtil.getPriorityPenalty(courseCode, "Non-existent Lab");
                System.out.println("  Invalid Lab Penalty: " + invalidPenalty);
                
            } else {
                System.out.println("Not a core lab course");
            }
        }
        
        System.out.println("\n=== Priority System Test Complete ===");
    }
}
