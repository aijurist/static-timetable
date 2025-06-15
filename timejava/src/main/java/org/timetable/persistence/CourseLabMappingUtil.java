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
 * Loads the mapping between a course (by course_code) and the list of lab rooms in which that course may be offered.
 * <p>
 * The CSV must follow the header structure: course_code,course_name,department,total_labs,lab_1,lab_2,lab_3
 * and should live in data/courses/course_lab_mapping.csv relative to the project root.
 */
public final class CourseLabMappingUtil {
    private static final Logger LOGGER = Logger.getLogger(CourseLabMappingUtil.class.getName());
    private static final String DEFAULT_PATH = "data/config/course_lab_mapping.csv";

    private static final Map<String, Set<String>> COURSE_TO_LABS = new HashMap<>();
    private static volatile boolean loaded = false;

    private CourseLabMappingUtil() {}

    /**
     * Returns the set of allowed lab room descriptions for a given course code.
     * Returns empty set if no mapping exists.
     */
    public static Set<String> getAllowedLabs(String courseCode) {
        ensureLoaded();
        return COURSE_TO_LABS.getOrDefault(courseCode, Collections.emptySet());
    }

    /**
     * Checks if a room (by description) is allowed for a given course code.
     */
    public static boolean isRoomAllowedForCourse(String courseCode, String roomDescription) {
        if (roomDescription == null || courseCode == null) return false;
        Set<String> allowedLabs = getAllowedLabs(courseCode);
        return allowedLabs.contains(roomDescription);
    }

    /**
     * Checks if a course is a core department course that requires specific lab mapping.
     */
    public static boolean isCoreLabCourse(String courseCode) {
        ensureLoaded();
        return COURSE_TO_LABS.containsKey(courseCode);
    }

    /**
     * Gets all courses that have lab mappings (for validation purposes).
     */
    public static Set<String> getAllMappedCourses() {
        ensureLoaded();
        return new HashSet<>(COURSE_TO_LABS.keySet());
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
                    List<String> labs = new ArrayList<>();
                    for (int i = 1; i <= 3; i++) {
                        String labName = rec.get("lab_" + i);
                        if (labName != null && !labName.isBlank()) {
                            labs.add(labName.trim());
                        }
                    }
                    if (!labs.isEmpty()) {
                        COURSE_TO_LABS.put(code, new HashSet<>(labs));
                    }
                }
                LOGGER.info("Loaded course-lab mapping for " + COURSE_TO_LABS.size() + " courses.");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load course_lab_mapping.csv", e);
        } finally {
            loaded = true;
        }
    }
} 