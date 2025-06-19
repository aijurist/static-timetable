package org.timetable.validation;

import org.timetable.domain.*;
import org.timetable.config.DepartmentWorkdayConfig;
import org.timetable.config.TimetableConfig;
import org.timetable.persistence.CourseLabMappingUtil;

import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyzes a completed timetable solution and reports detailed constraint violations.
 * This runs after solving is complete to provide comprehensive violation reports.
 */
public class ConstraintViolationAnalyzer {
    
    public static class ConstraintViolation {
        private final String constraintName;
        private final String violationType;
        private final Lesson lesson;
        private final String description;
        private final String severity;
        
        public ConstraintViolation(String constraintName, String violationType, Lesson lesson, String description, String severity) {
            this.constraintName = constraintName;
            this.violationType = violationType;
            this.lesson = lesson;
            this.description = description;
            this.severity = severity;
        }
        
        // Getters
        public String getConstraintName() { return constraintName; }
        public String getViolationType() { return violationType; }
        public Lesson getLesson() { return lesson; }
        public String getDescription() { return description; }
        public String getSeverity() { return severity; }
        
        @Override
        public String toString() {
            return String.format("[%s] %s - %s: %s (Lesson: %s)", 
                severity, constraintName, violationType, description, 
                lesson != null ? lesson.getId() : "N/A");
        }
    }
    
    public static class ViolationReport {
        private final List<ConstraintViolation> violations;
        private final Map<String, Integer> violationCountsByConstraint;
        private final Map<String, List<ConstraintViolation>> violationsByConstraint;
        
        public ViolationReport(List<ConstraintViolation> violations) {
            this.violations = violations;
            this.violationCountsByConstraint = violations.stream()
                .collect(Collectors.groupingBy(
                    ConstraintViolation::getConstraintName,
                    Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                ));
            this.violationsByConstraint = violations.stream()
                .collect(Collectors.groupingBy(ConstraintViolation::getConstraintName));
        }
        
        public List<ConstraintViolation> getViolations() { return violations; }
        public Map<String, Integer> getViolationCountsByConstraint() { return violationCountsByConstraint; }
        public Map<String, List<ConstraintViolation>> getViolationsByConstraint() { return violationsByConstraint; }
        
        public boolean hasCriticalViolations() {
            return violations.stream().anyMatch(v -> "CRITICAL".equals(v.getSeverity()));
        }
        
        public boolean hasHardViolations() {
            return violations.stream().anyMatch(v -> "HARD".equals(v.getSeverity()) || "CRITICAL".equals(v.getSeverity()));
        }
        
        public int getTotalViolations() { return violations.size(); }
        
        public void printSummary() {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("                    CONSTRAINT VIOLATION ANALYSIS REPORT");
            System.out.println("=".repeat(80));
            System.out.println(String.format("Total Violations: %d", getTotalViolations()));
            System.out.println(String.format("Critical Violations: %d", 
                violations.stream().mapToInt(v -> "CRITICAL".equals(v.getSeverity()) ? 1 : 0).sum()));
            System.out.println(String.format("Hard Violations: %d", 
                violations.stream().mapToInt(v -> "HARD".equals(v.getSeverity()) ? 1 : 0).sum()));
            System.out.println();
            
            if (violations.isEmpty()) {
                System.out.println("🎉 NO CONSTRAINT VIOLATIONS FOUND! The timetable is feasible.");
                return;
            }
            
            System.out.println("VIOLATIONS BY CONSTRAINT:");
            System.out.println("-".repeat(50));
            violationCountsByConstraint.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> {
                    System.out.println(String.format("%-40s: %d violations", entry.getKey(), entry.getValue()));
                });
        }
        
        public void printDetailedReport() {
            printSummary();
            
            if (violations.isEmpty()) return;
            
            System.out.println("\n" + "=".repeat(80));
            System.out.println("                        DETAILED VIOLATION REPORT");
            System.out.println("=".repeat(80));
            
            for (Map.Entry<String, List<ConstraintViolation>> entry : violationsByConstraint.entrySet()) {
                String constraintName = entry.getKey();
                List<ConstraintViolation> constraintViolations = entry.getValue();
                
                System.out.println(String.format("\n🚨 %s (%d violations)", constraintName, constraintViolations.size()));
                System.out.println("-".repeat(60));
                
                for (ConstraintViolation violation : constraintViolations) {
                    System.out.println(String.format("  • %s", violation.getDescription()));
                    if (violation.getLesson() != null) {
                        Lesson lesson = violation.getLesson();
                        System.out.println(String.format("    Lesson: %s | Course: %s | Group: %s | Time: %s | Room: %s",
                            lesson.getId(),
                            lesson.getCourse() != null ? lesson.getCourse().getCode() : "N/A",
                            lesson.getStudentGroup() != null ? lesson.getStudentGroup().getName() : "N/A",
                            lesson.getTimeSlot() != null ? lesson.getTimeSlot().toString() : "UNASSIGNED",
                            lesson.getRoom() != null ? lesson.getRoom().getName() : "UNASSIGNED"
                        ));
                    }
                    System.out.println();
                }
            }
        }
    }
    
    /**
     * Analyze a completed timetable solution for constraint violations
     */
    public static ViolationReport analyze(TimetableProblem problem) {
        List<ConstraintViolation> violations = new ArrayList<>();
        
        // Analyze each hard constraint
        violations.addAll(checkRoomConflicts(problem.getLessons()));
        violations.addAll(checkTeacherConflicts(problem.getLessons()));
        violations.addAll(checkStudentGroupConflicts(problem.getLessons()));
        violations.addAll(checkRoomCapacity(problem.getLessons()));
        violations.addAll(checkLabInLabRoom(problem.getLessons()));
        violations.addAll(checkTheoryInTheoryRoom(problem.getLessons()));
        violations.addAll(checkLabInLabSlot(problem.getLessons()));
        violations.addAll(checkTheoryInTheorySlot(problem.getLessons()));
        violations.addAll(checkLectureOrTutorialMustBeForFullGroup(problem.getLessons()));
        violations.addAll(checkLabForLargeGroupMustBeBatched(problem.getLessons()));
        violations.addAll(checkStrictCoreLabMappingEnforcement(problem.getLessons()));
        violations.addAll(checkCourseLabMustMatchMapping(problem.getLessons()));
        violations.addAll(checkDepartmentOutsideAllowedDays(problem.getLessons()));
        violations.addAll(checkSpecialRoomForAuto(problem.getLessons()));
        violations.addAll(checkMandatoryLunchBreak(problem.getLessons()));
        violations.addAll(checkPreventSameBatchOverlap(problem.getLessons()));
        
        return new ViolationReport(violations);
    }
    
    // Individual constraint check methods
    
    private static List<ConstraintViolation> checkRoomConflicts(List<Lesson> lessons) {
        List<ConstraintViolation> violations = new ArrayList<>();
        
        Map<String, List<Lesson>> roomTimeSlotMap = lessons.stream()
            .filter(lesson -> lesson.getTimeSlot() != null && lesson.getRoom() != null)
            .collect(Collectors.groupingBy(lesson -> 
                lesson.getRoom().getId() + "|" + lesson.getTimeSlot().getId()));
        
        for (Map.Entry<String, List<Lesson>> entry : roomTimeSlotMap.entrySet()) {
            List<Lesson> conflictingLessons = entry.getValue();
            if (conflictingLessons.size() > 1) {
                for (Lesson lesson : conflictingLessons) {
                    violations.add(new ConstraintViolation(
                        "Room Conflict",
                        "ROOM_DOUBLE_BOOKING",
                        lesson,
                        String.format("Room %s is double-booked at %s with %d other lessons", 
                            lesson.getRoom().getName(), 
                            lesson.getTimeSlot().toString(),
                            conflictingLessons.size() - 1),
                        "CRITICAL"
                    ));
                }
            }
        }
        
        return violations;
    }
    
    private static List<ConstraintViolation> checkTeacherConflicts(List<Lesson> lessons) {
        List<ConstraintViolation> violations = new ArrayList<>();
        
        for (int i = 0; i < lessons.size(); i++) {
            for (int j = i + 1; j < lessons.size(); j++) {
                Lesson lesson1 = lessons.get(i);
                Lesson lesson2 = lessons.get(j);
                
                if (lesson1.getTeacher() == null || lesson2.getTeacher() == null) continue;
                if (lesson1.getTimeSlot() == null || lesson2.getTimeSlot() == null) continue;
                
                if (lesson1.getTeacher().equals(lesson2.getTeacher()) &&
                    lesson1.getTimeSlot().getDayOfWeek().equals(lesson2.getTimeSlot().getDayOfWeek()) &&
                    timeSlotsOverlap(lesson1, lesson2)) {
                    
                    violations.add(new ConstraintViolation(
                        "Teacher Conflict",
                        "TEACHER_DOUBLE_BOOKING",
                        lesson1,
                        String.format("Teacher %s is double-booked: conflicts with lesson %s", 
                            lesson1.getTeacher().getName(), lesson2.getId()),
                        "CRITICAL"
                    ));
                }
            }
        }
        
        return violations;
    }
    
    private static List<ConstraintViolation> checkStudentGroupConflicts(List<Lesson> lessons) {
        List<ConstraintViolation> violations = new ArrayList<>();
        
        for (int i = 0; i < lessons.size(); i++) {
            for (int j = i + 1; j < lessons.size(); j++) {
                Lesson lesson1 = lessons.get(i);
                Lesson lesson2 = lessons.get(j);
                
                if (lesson1.getStudentGroup() == null || lesson2.getStudentGroup() == null) continue;
                if (lesson1.getTimeSlot() == null || lesson2.getTimeSlot() == null) continue;
                
                if (lesson1.getStudentGroup().equals(lesson2.getStudentGroup()) &&
                    lesson1.getTimeSlot().getDayOfWeek().equals(lesson2.getTimeSlot().getDayOfWeek()) &&
                    timeSlotsOverlap(lesson1, lesson2)) {
                    
                    // Check batching logic
                    boolean l1HasBatch = lesson1.isSplitBatch();
                    boolean l2HasBatch = lesson2.isSplitBatch();
                    boolean isConflict = false;
                    
                    if (!l1HasBatch && !l2HasBatch) {
                        isConflict = true; // Two full-group lessons
                    } else if (l1HasBatch != l2HasBatch) {
                        isConflict = true; // Full-group and batched lesson
                    } else if (l1HasBatch && l2HasBatch && lesson1.getLabBatch().equals(lesson2.getLabBatch())) {
                        isConflict = true; // Same batch
                    }
                    
                    if (isConflict) {
                        violations.add(new ConstraintViolation(
                            "Student Group Conflict",
                            "STUDENT_GROUP_DOUBLE_BOOKING",
                            lesson1,
                            String.format("Student group %s is double-booked: conflicts with lesson %s", 
                                lesson1.getStudentGroup().getName(), lesson2.getId()),
                            "CRITICAL"
                        ));
                    }
                }
            }
        }
        
        return violations;
    }
    
    private static List<ConstraintViolation> checkRoomCapacity(List<Lesson> lessons) {
        List<ConstraintViolation> violations = new ArrayList<>();
        
        for (Lesson lesson : lessons) {
            if (lesson.getRoom() != null && 
                lesson.getRoom().getCapacity() < lesson.getRequiredCapacity()) {
                
                violations.add(new ConstraintViolation(
                    "Room Capacity",
                    "INSUFFICIENT_CAPACITY",
                    lesson,
                    String.format("Room %s (capacity: %d) cannot accommodate %d students", 
                        lesson.getRoom().getName(), 
                        lesson.getRoom().getCapacity(),
                        lesson.getRequiredCapacity()),
                    "HARD"
                ));
            }
        }
        
        return violations;
    }
    
    private static List<ConstraintViolation> checkLabInLabRoom(List<Lesson> lessons) {
        List<ConstraintViolation> violations = new ArrayList<>();
        
        for (Lesson lesson : lessons) {
            if (lesson.requiresLabRoom() && 
                lesson.getRoom() != null && !lesson.getRoom().isLab()) {
                
                violations.add(new ConstraintViolation(
                    "Lab in Lab Room",
                    "LAB_IN_THEORY_ROOM",
                    lesson,
                    String.format("Lab session assigned to theory room %s", lesson.getRoom().getName()),
                    "HARD"
                ));
            }
        }
        
        return violations;
    }
    
    private static List<ConstraintViolation> checkTheoryInTheoryRoom(List<Lesson> lessons) {
        List<ConstraintViolation> violations = new ArrayList<>();
        
        for (Lesson lesson : lessons) {
            if (lesson.requiresTheoryRoom() && 
                lesson.getRoom() != null && lesson.getRoom().isLab()) {
                
                violations.add(new ConstraintViolation(
                    "Theory in Theory Room",
                    "THEORY_IN_LAB_ROOM",
                    lesson,
                    String.format("Theory session assigned to lab room %s", lesson.getRoom().getName()),
                    "HARD"
                ));
            }
        }
        
        return violations;
    }
    
    private static List<ConstraintViolation> checkLabInLabSlot(List<Lesson> lessons) {
        List<ConstraintViolation> violations = new ArrayList<>();
        
        for (Lesson lesson : lessons) {
            if (lesson.requiresLabRoom() && 
                lesson.getTimeSlot() != null && !lesson.getTimeSlot().isLab()) {
                
                violations.add(new ConstraintViolation(
                    "Lab in Lab Slot",
                    "LAB_IN_THEORY_SLOT",
                    lesson,
                    String.format("Lab session assigned to theory time slot %s", lesson.getTimeSlot().toString()),
                    "HARD"
                ));
            }
        }
        
        return violations;
    }
    
    private static List<ConstraintViolation> checkTheoryInTheorySlot(List<Lesson> lessons) {
        List<ConstraintViolation> violations = new ArrayList<>();
        
        for (Lesson lesson : lessons) {
            if (lesson.requiresTheoryRoom() && 
                lesson.getTimeSlot() != null && lesson.getTimeSlot().isLab()) {
                
                violations.add(new ConstraintViolation(
                    "Theory in Theory Slot",
                    "THEORY_IN_LAB_SLOT",
                    lesson,
                    String.format("Theory session assigned to lab time slot %s", lesson.getTimeSlot().toString()),
                    "HARD"
                ));
            }
        }
        
        return violations;
    }
    
    private static List<ConstraintViolation> checkLectureOrTutorialMustBeForFullGroup(List<Lesson> lessons) {
        List<ConstraintViolation> violations = new ArrayList<>();
        
        for (Lesson lesson : lessons) {
            if (lesson.requiresTheoryRoom() && lesson.isSplitBatch()) {
                violations.add(new ConstraintViolation(
                    "Theory Must Be Full Group",
                    "THEORY_BATCHED",
                    lesson,
                    String.format("Theory/Tutorial session %s-%s assigned to batch %s (should be full group)", 
                        lesson.getStudentGroup().getName(), 
                        lesson.getCourse().getCode(),
                        lesson.getLabBatch()),
                    "CRITICAL"
                ));
            }
        }
        
        return violations;
    }
    
    private static List<ConstraintViolation> checkLabForLargeGroupMustBeBatched(List<Lesson> lessons) {
        List<ConstraintViolation> violations = new ArrayList<>();
        
        for (Lesson lesson : lessons) {
            if (lesson.requiresLabRoom() &&
                lesson.getStudentGroup() != null &&
                lesson.getStudentGroup().getSize() > TimetableConfig.LAB_BATCH_SIZE &&
                !lesson.isSplitBatch()) {
                
                violations.add(new ConstraintViolation(
                    "Lab Must Be Batched",
                    "LARGE_GROUP_NOT_BATCHED",
                    lesson,
                    String.format("Large group (%d students) lab not batched", lesson.getStudentGroup().getSize()),
                    "HARD"
                ));
            }
        }
        
        return violations;
    }
    
    private static List<ConstraintViolation> checkStrictCoreLabMappingEnforcement(List<Lesson> lessons) {
        List<ConstraintViolation> violations = new ArrayList<>();
        
        for (Lesson lesson : lessons) {
            if (lesson.getSessionType().equals("lab") &&
                lesson.getRoom() != null &&
                CourseLabMappingUtil.isCoreLabCourse(lesson.getCourse().getCode()) &&
                !CourseLabMappingUtil.isRoomAllowedForCourse(lesson.getCourse().getCode(), lesson.getRoom().getDescription())) {
                
                violations.add(new ConstraintViolation(
                    "Core Lab Mapping",
                    "INVALID_CORE_LAB_ASSIGNMENT",
                    lesson,
                    String.format("Course %s assigned to disallowed lab %s. Allowed labs: %s", 
                        lesson.getCourse().getCode(), 
                        lesson.getRoom().getDescription(),
                        CourseLabMappingUtil.getPriorityOrderedLabs(lesson.getCourse().getCode())),
                    "CRITICAL"
                ));
            }
        }
        
        return violations;
    }
    
    private static List<ConstraintViolation> checkCourseLabMustMatchMapping(List<Lesson> lessons) {
        List<ConstraintViolation> violations = new ArrayList<>();
        
        final Set<String> ALWAYS_COMPUTER = Set.of("CD23321", "CS19P23", "CS19P21");
        
        for (Lesson lesson : lessons) {
            if (lesson.getSessionType().equals("lab") && lesson.getRoom() != null) {
                String courseCode = lesson.getCourse().getCode();
                String roomLabType = lesson.getRoom().getLabType();
                
                // Skip core lab courses - handled by strictCoreLabMappingEnforcement
                if (CourseLabMappingUtil.isCoreLabCourse(courseCode)) continue;
                
                boolean violation = false;
                String violationReason = "";
                
                // Check CSV lab_type
                String courseLabType = lesson.getCourse().getLabType();
                if (courseLabType != null) {
                    if ("computer".equals(courseLabType) && !"computer".equals(roomLabType)) {
                        violation = true;
                        violationReason = "Course requires computer lab but assigned to " + roomLabType;
                    } else if ("core".equals(courseLabType) && !"core".equals(roomLabType)) {
                        violation = true;
                        violationReason = "Course requires core lab but assigned to " + roomLabType;
                    } else if (!courseLabType.equals(roomLabType)) {
                        violation = true;
                        violationReason = "Course requires " + courseLabType + " lab but assigned to " + roomLabType;
                    }
                }
                
                // Check hardcoded computer courses
                if (ALWAYS_COMPUTER.contains(courseCode) && !"computer".equals(roomLabType)) {
                    violation = true;
                    violationReason = "Hardcoded computer course assigned to " + roomLabType + " lab";
                }
                
                if (violation) {
                    violations.add(new ConstraintViolation(
                        "Lab Type Matching",
                        "LAB_TYPE_MISMATCH",
                        lesson,
                        violationReason,
                        "HARD"
                    ));
                }
            }
        }
        
        return violations;
    }
    
    private static List<ConstraintViolation> checkDepartmentOutsideAllowedDays(List<Lesson> lessons) {
        List<ConstraintViolation> violations = new ArrayList<>();
        
        for (Lesson lesson : lessons) {
            if (lesson.getTimeSlot() != null && lesson.getStudentGroup() != null &&
                !DepartmentWorkdayConfig.isAllowedDay(
                    lesson.getStudentGroup().getDepartment(), 
                    lesson.getTimeSlot().getDayOfWeek())) {
                
                violations.add(new ConstraintViolation(
                    "Department Working Days",
                    "OUTSIDE_ALLOWED_DAYS",
                    lesson,
                    String.format("Department %s scheduled on %s (not in allowed working days)", 
                        lesson.getStudentGroup().getDepartment(), 
                        lesson.getTimeSlot().getDayOfWeek()),
                    "HARD"
                ));
            }
        }
        
        return violations;
    }
    
    private static List<ConstraintViolation> checkSpecialRoomForAuto(List<Lesson> lessons) {
        List<ConstraintViolation> violations = new ArrayList<>();
        Set<String> SPECIAL_ROOMS = Set.of("61", "62", "C108");
        
        for (Lesson lesson : lessons) {
            if (lesson.getRoom() != null && 
                SPECIAL_ROOMS.contains(lesson.getRoom().getName()) &&
                !"AUTO".equalsIgnoreCase(lesson.getStudentGroup().getDepartment())) {
                
                violations.add(new ConstraintViolation(
                    "Special Room for AUTO",
                    "NON_AUTO_IN_SPECIAL_ROOM",
                    lesson,
                    String.format("Non-AUTO department %s assigned to AUTO-only room %s", 
                        lesson.getStudentGroup().getDepartment(), 
                        lesson.getRoom().getName()),
                    "HARD"
                ));
            }
        }
        
        return violations;
    }
    
    private static List<ConstraintViolation> checkMandatoryLunchBreak(List<Lesson> lessons) {
        List<ConstraintViolation> violations = new ArrayList<>();
        
        // Group by student group and day
        Map<String, List<Lesson>> groupDayLessons = lessons.stream()
            .filter(lesson -> lesson.getTimeSlot() != null && lesson.getStudentGroup() != null)
            .collect(Collectors.groupingBy(lesson -> 
                lesson.getStudentGroup().getId() + "|" + lesson.getTimeSlot().getDayOfWeek()));
        
        for (Map.Entry<String, List<Lesson>> entry : groupDayLessons.entrySet()) {
            List<Lesson> dayLessons = entry.getValue();
            if (violatesLunchBreakRule(dayLessons)) {
                for (Lesson lesson : dayLessons) {
                    LocalTime lessonStart = lesson.getTimeSlot().getStartTime();
                    LocalTime lessonEnd = lesson.getTimeSlot().getEndTime();
                    
                    // Check if lesson overlaps critical lunch period
                    LocalTime criticalStart = LocalTime.of(11, 50);
                    LocalTime criticalEnd = LocalTime.of(13, 0);
                    
                    if (lessonStart.isBefore(criticalEnd) && lessonEnd.isAfter(criticalStart)) {
                        violations.add(new ConstraintViolation(
                            "Mandatory Lunch Break",
                            "LUNCH_BREAK_VIOLATION",
                            lesson,
                            String.format("Student group %s has insufficient lunch break on %s", 
                                lesson.getStudentGroup().getName(), 
                                lesson.getTimeSlot().getDayOfWeek()),
                            "HARD"
                        ));
                    }
                }
            }
        }
        
        return violations;
    }
    
    private static List<ConstraintViolation> checkPreventSameBatchOverlap(List<Lesson> lessons) {
        List<ConstraintViolation> violations = new ArrayList<>();
        
        for (int i = 0; i < lessons.size(); i++) {
            for (int j = i + 1; j < lessons.size(); j++) {
                Lesson lesson1 = lessons.get(i);
                Lesson lesson2 = lessons.get(j);
                
                if (lesson1.getStudentGroup() == null || lesson2.getStudentGroup() == null) continue;
                if (lesson1.getCourse() == null || lesson2.getCourse() == null) continue;
                if (lesson1.getTimeSlot() == null || lesson2.getTimeSlot() == null) continue;
                
                if (lesson1.getStudentGroup().equals(lesson2.getStudentGroup()) &&
                    lesson1.getCourse().equals(lesson2.getCourse()) &&
                    lesson1.isSplitBatch() && lesson2.isSplitBatch() &&
                    lesson1.getLabBatch().equals(lesson2.getLabBatch()) &&
                    lesson1.getTimeSlot().getDayOfWeek().equals(lesson2.getTimeSlot().getDayOfWeek()) &&
                    timeSlotsOverlap(lesson1, lesson2)) {
                    
                    violations.add(new ConstraintViolation(
                        "Same Batch Overlap",
                        "SAME_BATCH_SIMULTANEOUS",
                        lesson1,
                        String.format("Same batch %s scheduled simultaneously: conflicts with lesson %s", 
                            lesson1.getLabBatch(), lesson2.getId()),
                        "CRITICAL"
                    ));
                }
            }
        }
        
        return violations;
    }
    
    // Helper methods
    
    private static boolean timeSlotsOverlap(Lesson lesson1, Lesson lesson2) {
        LocalTime start1 = lesson1.getTimeSlot().getStartTime();
        LocalTime end1 = lesson1.getTimeSlot().getEndTime();
        LocalTime start2 = lesson2.getTimeSlot().getStartTime();
        LocalTime end2 = lesson2.getTimeSlot().getEndTime();
        
        return start1.isBefore(end2) && start2.isBefore(end1);
    }
    
    private static boolean violatesLunchBreakRule(List<Lesson> lessons) {
        if (lessons.size() < 2) return false;
        
        // Sort lessons by start time
        lessons.sort(Comparator.comparing(lesson -> lesson.getTimeSlot().getStartTime()));
        
        LocalTime lunchPeriodStart = LocalTime.of(11, 0);
        LocalTime lunchPeriodEnd = LocalTime.of(14, 0);
        
        // Find lessons that overlap with lunch period
        List<Lesson> lunchPeriodLessons = lessons.stream()
            .filter(lesson -> {
                LocalTime lessonStart = lesson.getTimeSlot().getStartTime();
                LocalTime lessonEnd = lesson.getTimeSlot().getEndTime();
                return lessonStart.isBefore(lunchPeriodEnd) && lessonEnd.isAfter(lunchPeriodStart);
            })
            .collect(Collectors.toList());
        
        if (lunchPeriodLessons.size() < 2) return false;
        
        // Check for continuous scheduling without adequate break
        for (int i = 0; i < lunchPeriodLessons.size() - 1; i++) {
            Lesson current = lunchPeriodLessons.get(i);
            Lesson next = lunchPeriodLessons.get(i + 1);
            
            LocalTime currentEnd = current.getTimeSlot().getEndTime();
            LocalTime nextStart = next.getTimeSlot().getStartTime();
            
            if (currentEnd.equals(nextStart) || 
                Duration.between(currentEnd, nextStart).toMinutes() < 50) {
                
                LocalTime criticalLunchStart = LocalTime.of(11, 50);
                LocalTime criticalLunchEnd = LocalTime.of(13, 0);
                
                if ((currentEnd.isAfter(criticalLunchStart) || currentEnd.equals(criticalLunchStart)) &&
                    (nextStart.isBefore(criticalLunchEnd) || nextStart.equals(criticalLunchEnd))) {
                    return true;
                }
            }
        }
        
        return false;
    }
} 