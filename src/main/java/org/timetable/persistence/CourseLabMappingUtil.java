package org.timetable.persistence;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads the mapping between a course (by course_code) and the prioritized list of lab rooms in which that course may be offered.
 * <p>
 * The CSV must follow the header structure: course_code,course_name,department,total_labs,lab_1,lab_2,lab_3
 * Lab priority: lab_1 is highest priority, lab_2 is medium priority, lab_3 is lowest priority
 * and should live in data/config/course_lab_mapping.csv relative to the project root.
 */
public final class CourseLabMappingUtil {
    private static final Logger LOGGER = Logger.getLogger(CourseLabMappingUtil.class.getName());
    private static final String DEFAULT_PATH = "data/config/course_lab_mapping.csv";

    // Store labs in priority order: first element is highest priority (lab_1)
    private static final Map<String, List<String>> COURSE_TO_PRIORITY_LABS = new HashMap<>();
    private static volatile boolean loaded = false;

    private CourseLabMappingUtil() {}

    /**
     * Returns the set of allowed lab room descriptions for a given course code.
     * Returns empty set if no mapping exists.
     * @deprecated Use getPriorityOrderedLabs() for priority-aware logic
     */
    @Deprecated
    public static Set<String> getAllowedLabs(String courseCode) {
        ensureLoaded();
        List<String> priorityLabs = COURSE_TO_PRIORITY_LABS.getOrDefault(courseCode, Collections.emptyList());
        return new HashSet<>(priorityLabs);
    }

    /**
     * Returns the lab room descriptions in priority order for a given course code.
     * First element is highest priority (lab_1), second is medium priority (lab_2), third is lowest priority (lab_3).
     * Returns empty list if no mapping exists.
     */
    public static List<String> getPriorityOrderedLabs(String courseCode) {
        ensureLoaded();
        return new ArrayList<>(COURSE_TO_PRIORITY_LABS.getOrDefault(courseCode, Collections.emptyList()));
    }

    /**
     * Gets the highest priority lab (lab_1) for a course.
     * Returns null if no mapping exists or no labs are defined.
     */
    public static String getHighestPriorityLab(String courseCode) {
        List<String> labs = getPriorityOrderedLabs(courseCode);
        return labs.isEmpty() ? null : labs.get(0);
    }

    /**
     * Gets the priority level of a specific lab for a course.
     * Returns 1 for lab_1 (highest), 2 for lab_2 (medium), 3 for lab_3 (lowest).
     * Returns -1 if the lab is not allowed for this course or course not found.
     */
    public static int getLabPriority(String courseCode, String roomDescription) {
        if (roomDescription == null || courseCode == null) return -1;
        List<String> priorityLabs = getPriorityOrderedLabs(courseCode);
        int index = priorityLabs.indexOf(roomDescription);
        return index == -1 ? -1 : index + 1; // Convert 0-based index to 1-based priority
    }

    /**
     * Checks if a room (by description) is allowed for a given course code.
     */
    public static boolean isRoomAllowedForCourse(String courseCode, String roomDescription) {
        if (roomDescription == null || courseCode == null) return false;
        return getLabPriority(courseCode, roomDescription) > 0;
    }

    /**
     * Checks if a course is a core department course that requires specific lab mapping.
     */
    public static boolean isCoreLabCourse(String courseCode) {
        ensureLoaded();
        return COURSE_TO_PRIORITY_LABS.containsKey(courseCode);
    }

    /**
     * Gets all courses that have lab mappings (for validation purposes).
     */
    public static Set<String> getAllMappedCourses() {
        ensureLoaded();
        return new HashSet<>(COURSE_TO_PRIORITY_LABS.keySet());
    }

    /**
     * Gets detailed mapping information for debugging.
     */
    public static void printMappingInfo() {
        ensureLoaded();
        LOGGER.info("=== COURSE LAB MAPPING INFO ===");
        LOGGER.info("Total mapped courses: " + COURSE_TO_PRIORITY_LABS.size());
        
        COURSE_TO_PRIORITY_LABS.forEach((course, labs) -> {
            LOGGER.info("Course " + course + " -> Priority Labs: " + labs + 
                       " (1=highest priority, " + labs.size() + "=lowest priority)");
        });
        
        LOGGER.info("=== END MAPPING INFO ===");
    }

    /**
     * Validates that all mapped labs exist in the system.
     */
    public static Set<String> getUnmappedLabs(Set<String> availableLabs) {
        ensureLoaded();
        Set<String> allMappedLabs = new HashSet<>();
        COURSE_TO_PRIORITY_LABS.values().forEach(allMappedLabs::addAll);
        
        Set<String> unmappedLabs = new HashSet<>(allMappedLabs);
        unmappedLabs.removeAll(availableLabs);
        
        return unmappedLabs;
    }

    /**
     * Returns the required lab type for a course if specified ("computer" or "core").
     * Currently returns null for all courses as lab type is not stored in the mapping.
     * This method exists to support constraint logic that may reference it.
     */
    public static String getRequiredLabType(String courseCode) {
        return null; // Not implemented yet - can be extended later
    }

    /**
     * Calculates the priority penalty for assigning a course to a specific lab.
     * Lower penalty is better (priority 1 = penalty 0, priority 2 = penalty 1, priority 3 = penalty 2).
     * Returns 1000 if the lab is not allowed for this course (severe penalty).
     */
    public static int getPriorityPenalty(String courseCode, String roomDescription) {
        int priority = getLabPriority(courseCode, roomDescription);
        if (priority == -1) {
            return 1000; // Severe penalty for disallowed assignment
        }
        return priority - 1; // Convert priority 1,2,3 to penalty 0,1,2
    }

    private static synchronized void ensureLoaded() {
        if (loaded) return;
        try {
            if (!Files.exists(Paths.get(DEFAULT_PATH))) {
                LOGGER.warning("course_lab_mapping.csv not found at " + DEFAULT_PATH);
                loaded = true; // prevent re-attempts
                return;
            }
            try (Reader reader = new FileReader(DEFAULT_PATH);
                 CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
                for (CSVRecord rec : parser) {
                    String code = rec.get("course_code").trim();
                    List<String> labs = new ArrayList<>(); // Maintain order for priority
                    
                    // Add labs in priority order: lab_1, lab_2, lab_3
                    for (int i = 1; i <= 3; i++) {
                        String labName = rec.get("lab_" + i);
                        if (labName != null && !labName.isBlank()) {
                            labs.add(labName.trim());
                        }
                    }
                    
                    if (!labs.isEmpty()) {
                        COURSE_TO_PRIORITY_LABS.put(code, labs);
                        LOGGER.fine("Loaded course " + code + " with priority labs: " + labs);
                    }
                }
                LOGGER.info("Loaded course-lab mapping for " + COURSE_TO_PRIORITY_LABS.size() + " courses with priority ordering.");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load course_lab_mapping.csv", e);
        } finally {
            loaded = true;
        }
    }
} 