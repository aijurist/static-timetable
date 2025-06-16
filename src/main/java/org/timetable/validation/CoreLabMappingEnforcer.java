package org.timetable.validation;

import org.timetable.domain.Lesson;
import org.timetable.domain.Room;
import org.timetable.domain.TimetableProblem;
import org.timetable.persistence.CourseLabMappingUtil;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Enforces and validates that core department courses are only assigned to their designated labs
 * as specified in the course_lab_mapping.csv file.
 */
public class CoreLabMappingEnforcer {
    private static final Logger LOGGER = Logger.getLogger(CoreLabMappingEnforcer.class.getName());

    /**
     * Validates the lab mappings before solving starts.
     */
    public static void validateMappingsBeforeSolving(TimetableProblem problem) {
        LOGGER.info("=== VALIDATING CORE LAB MAPPINGS ===");
        
        // Get all available lab descriptions
        Set<String> availableLabs = problem.getRooms().stream()
                .filter(Room::isLab)
                .map(Room::getDescription)
                .collect(Collectors.toSet());
        
        // Check for unmapped labs
        Set<String> unmappedLabs = CourseLabMappingUtil.getUnmappedLabs(availableLabs);
        if (!unmappedLabs.isEmpty()) {
            LOGGER.severe("ERROR: The following labs are referenced in course_lab_mapping.csv but don't exist in lab_core.csv:");
            unmappedLabs.forEach(lab -> LOGGER.severe("  - " + lab));
            throw new IllegalStateException("Missing labs in lab_core.csv. Please add them before solving.");
        }
        
        // Print mapping info for debugging
        CourseLabMappingUtil.printMappingInfo();
        
        // Validate that all mapped courses have valid lab choices
        Set<String> mappedCourses = CourseLabMappingUtil.getAllMappedCourses();
        LOGGER.info("Found " + mappedCourses.size() + " courses with explicit lab mappings");
        
        for (String courseCode : mappedCourses) {
            Set<String> allowedLabs = CourseLabMappingUtil.getAllowedLabs(courseCode);
            LOGGER.info("Course " + courseCode + " can use labs: " + allowedLabs);
            
            // Verify all allowed labs exist
            for (String labName : allowedLabs) {
                if (!availableLabs.contains(labName)) {
                    LOGGER.severe("ERROR: Course " + courseCode + " references non-existent lab: " + labName);
                    throw new IllegalStateException("Invalid lab reference: " + labName);
                }
            }
        }
        
        LOGGER.info("✓ All lab mappings validated successfully");
        LOGGER.info("=== END VALIDATION ===");
    }

    /**
     * Validates the solution after solving to ensure no violations.
     */
    public static ValidationResult validateSolutionMappings(TimetableProblem solution) {
        LOGGER.info("=== VALIDATING SOLUTION LAB MAPPINGS ===");
        
        ValidationResult result = new ValidationResult();
        
        List<Lesson> labLessons = solution.getLessons().stream()
                .filter(lesson -> lesson.requiresLabRoom())
                .filter(lesson -> lesson.getRoom() != null)
                .collect(Collectors.toList());
        
        for (Lesson lesson : labLessons) {
            String courseCode = lesson.getCourse().getCode();
            
            if (CourseLabMappingUtil.isCoreLabCourse(courseCode)) {
                String roomDesc = lesson.getRoom().getDescription();
                
                if (!CourseLabMappingUtil.isRoomAllowedForCourse(courseCode, roomDesc)) {
                    result.addViolation(new MappingViolation(
                        courseCode,
                        lesson.getCourse().getName(),
                        roomDesc,
                        CourseLabMappingUtil.getAllowedLabs(courseCode)
                    ));
                }
            }
        }
        
        if (result.hasViolations()) {
            LOGGER.severe("Found " + result.getViolationCount() + " lab mapping violations:");
            for (MappingViolation violation : result.getViolations()) {
                LOGGER.severe("  Course " + violation.getCourseCode() + " (" + violation.getCourseName() + 
                             ") assigned to wrong lab: " + violation.getAssignedLab());
                LOGGER.severe("    Allowed labs: " + violation.getAllowedLabs());
            }
        } else {
            LOGGER.info("✓ No lab mapping violations found in solution");
        }
        
        LOGGER.info("=== END SOLUTION VALIDATION ===");
        return result;
    }

    /**
     * Filters available rooms for a course to only include allowed labs.
     */
    public static List<Room> getFilteredRoomsForCourse(String courseCode, List<Room> allRooms) {
        if (!CourseLabMappingUtil.isCoreLabCourse(courseCode)) {
            // Not a mapped course, return all lab rooms
            return allRooms.stream()
                    .filter(Room::isLab)
                    .collect(Collectors.toList());
        }
        
        Set<String> allowedLabs = CourseLabMappingUtil.getAllowedLabs(courseCode);
        return allRooms.stream()
                .filter(Room::isLab)
                .filter(room -> allowedLabs.contains(room.getDescription()))
                .collect(Collectors.toList());
    }

    public static class ValidationResult {
        private final List<MappingViolation> violations = new ArrayList<>();
        
        public void addViolation(MappingViolation violation) {
            violations.add(violation);
        }
        
        public boolean hasViolations() {
            return !violations.isEmpty();
        }
        
        public int getViolationCount() {
            return violations.size();
        }
        
        public List<MappingViolation> getViolations() {
            return new ArrayList<>(violations);
        }
    }

    public static class MappingViolation {
        private final String courseCode;
        private final String courseName;
        private final String assignedLab;
        private final Set<String> allowedLabs;
        
        public MappingViolation(String courseCode, String courseName, String assignedLab, Set<String> allowedLabs) {
            this.courseCode = courseCode;
            this.courseName = courseName;
            this.assignedLab = assignedLab;
            this.allowedLabs = new HashSet<>(allowedLabs);
        }
        
        public String getCourseCode() { return courseCode; }
        public String getCourseName() { return courseName; }
        public String getAssignedLab() { return assignedLab; }
        public Set<String> getAllowedLabs() { return allowedLabs; }
    }
} 