package org.timetable.validation;

import org.timetable.domain.Lesson;
import org.timetable.domain.TimetableProblem;
import org.timetable.persistence.CourseLabMappingUtil;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Validates that core department courses are properly assigned to their designated labs
 * and not incorrectly placed in computer labs.
 */
public class CoreLabMappingValidator {
    private static final Logger LOGGER = Logger.getLogger(CoreLabMappingValidator.class.getName());
    
    private static final Set<String> COMPUTER_LAB_PATTERNS = Set.of(
        "Computer Lab", "Programming Lab", "Software Lab", "CS Lab", "IT Lab",
        "Data Structures Lab", "Algorithm Lab", "Database Lab", "Network Lab"
    );
    
    private static final Set<String> CS_DEPARTMENTS = Set.of(
        "CSE", "IT", "AIML", "AIDS", "CSBS", "CSD", "CSE-CS"
    );

    public static void validateCoreLabMappings(TimetableProblem problem) {
        LOGGER.info("=== CORE LAB MAPPING VALIDATION ===");
        
        List<Lesson> assignedLessons = problem.getLessons().stream()
                .filter(lesson -> lesson.getRoom() != null && lesson.requiresLabRoom())
                .collect(Collectors.toList());
        
        validateMappedCourseViolations(assignedLessons);
        validateComputerLabMisuse(assignedLessons);
        validateCoreLabUtilization(assignedLessons);
        
        LOGGER.info("=== END CORE LAB MAPPING VALIDATION ===");
    }
    
    private static void validateMappedCourseViolations(List<Lesson> lessons) {
        LOGGER.info("--- Checking Course-Lab Mapping Violations ---");
        
        int violations = 0;
        Map<String, List<String>> violationsByCourse = new HashMap<>();
        
        for (Lesson lesson : lessons) {
            String courseCode = lesson.getCourse().getCode();
            
            if (CourseLabMappingUtil.isCoreLabCourse(courseCode)) {
                String roomDesc = lesson.getRoom().getDescription();
                
                if (!CourseLabMappingUtil.isRoomAllowedForCourse(courseCode, roomDesc)) {
                    violations++;
                    violationsByCourse.computeIfAbsent(courseCode, k -> new ArrayList<>())
                            .add(roomDesc);
                }
            }
        }
        
        if (violations > 0) {
            LOGGER.warning("Found " + violations + " course-lab mapping violations:");
            violationsByCourse.forEach((course, rooms) -> {
                Set<String> allowedLabs = CourseLabMappingUtil.getAllowedLabs(course);
                LOGGER.warning("  Course " + course + " assigned to wrong labs: " + rooms);
                LOGGER.warning("    Allowed labs: " + allowedLabs);
            });
        } else {
            LOGGER.info("✓ No course-lab mapping violations found");
        }
    }
    
    private static void validateComputerLabMisuse(List<Lesson> lessons) {
        LOGGER.info("--- Checking Core Courses in Computer Labs ---");
        
        int violations = 0;
        Map<String, Set<String>> violationsByDept = new HashMap<>();
        
        for (Lesson lesson : lessons) {
            String dept = lesson.getStudentGroup().getDepartment();
            String roomDesc = lesson.getRoom().getDescription();
            String courseCode = lesson.getCourse().getCode();
            
            // Check if core department course is in computer lab
            if (!CS_DEPARTMENTS.contains(dept) && isComputerLab(roomDesc)) {
                // Allow if explicitly mapped
                if (!CourseLabMappingUtil.isRoomAllowedForCourse(courseCode, roomDesc)) {
                    violations++;
                    violationsByDept.computeIfAbsent(dept, k -> new HashSet<>())
                            .add(courseCode + " → " + roomDesc);
                }
            }
        }
        
        if (violations > 0) {
            LOGGER.warning("Found " + violations + " core courses incorrectly in computer labs:");
            violationsByDept.forEach((dept, courses) -> {
                LOGGER.warning("  " + dept + " department: " + courses);
            });
        } else {
            LOGGER.info("✓ No core courses found in computer labs");
        }
    }
    
    private static void validateCoreLabUtilization(List<Lesson> lessons) {
        LOGGER.info("--- Core Lab Utilization Analysis ---");
        
        Map<String, Integer> coreLabUsage = new HashMap<>();
        Map<String, Set<String>> labToDepartments = new HashMap<>();
        
        for (Lesson lesson : lessons) {
            String dept = lesson.getStudentGroup().getDepartment();
            String roomDesc = lesson.getRoom().getDescription();
            
            if (!CS_DEPARTMENTS.contains(dept)) {
                coreLabUsage.merge(roomDesc, 1, Integer::sum);
                labToDepartments.computeIfAbsent(roomDesc, k -> new HashSet<>()).add(dept);
            }
        }
        
        LOGGER.info("Core lab usage summary:");
        coreLabUsage.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> {
                    String lab = entry.getKey();
                    int usage = entry.getValue();
                    Set<String> depts = labToDepartments.get(lab);
                    LOGGER.info("  " + lab + ": " + usage + " sessions (" + depts + ")");
                });
    }
    
    private static boolean isComputerLab(String roomDescription) {
        return COMPUTER_LAB_PATTERNS.stream()
                .anyMatch(pattern -> roomDescription.toLowerCase().contains(pattern.toLowerCase()));
    }
} 