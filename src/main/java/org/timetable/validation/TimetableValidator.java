package org.timetable.validation;

import org.timetable.domain.*;
import org.timetable.config.TimetableConfig;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Comprehensive timetable validation and analytics system
 */
public class TimetableValidator {
    
    private final TimetableProblem solution;
    private final ValidationReport report;
    private final List<Lesson> assignedLessons;
    private final List<Lesson> unassignedLessons;
    
    public TimetableValidator(TimetableProblem solution) {
        this.solution = solution;
        this.report = new ValidationReport();
        this.assignedLessons = solution.getLessons().stream()
                .filter(lesson -> lesson.getTimeSlot() != null && lesson.getRoom() != null)
                .collect(Collectors.toList());
        this.unassignedLessons = solution.getLessons().stream()
                .filter(lesson -> lesson.getTimeSlot() == null || lesson.getRoom() == null)
                .collect(Collectors.toList());
    }
    
    /**
     * Run comprehensive validation and return detailed report
     */
    public ValidationReport validate() {
        report.setTotalLessons(solution.getLessons().size());
        report.setAssignedLessons(assignedLessons.size());
        report.setUnassignedLessons(unassignedLessons.size());
        
        // Hard constraint validations
        validateRoomConflicts();
        validateTeacherConflicts();
        validateStudentGroupConflicts();
        validateRoomCapacity();
        validateLabTheoryRoomAssignment();
        validateLabTheoryTimeSlotAssignment();
        validateSameTeacherSameCourseConflicts();
        validateLabTheoryOverlaps();
        
        // Soft constraint validations
        validateTeacherWorkload();
        validateTeacherWorkdaySpan();
        validateTeacherBreaks();
        validateLateClasses();
        validateTeacherTravelTime();
        
        // Additional analytics
        analyzeResourceUtilization();
        analyzeTeacherLoad();
        analyzeStudentGroupSchedules();
        
        return report;
    }
    
    // ############################################################################
    // Hard Constraint Validations
    // ############################################################################
    
    private void validateRoomConflicts() {
        Map<String, List<Lesson>> roomTimeMap = new HashMap<>();
        
        for (Lesson lesson : assignedLessons) {
            String key = lesson.getRoom().getId() + "_" + lesson.getTimeSlot().getId();
            roomTimeMap.computeIfAbsent(key, k -> new ArrayList<>()).add(lesson);
        }
        
        for (Map.Entry<String, List<Lesson>> entry : roomTimeMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                ConstraintViolation violation = new ConstraintViolation(
                    "Room Conflict",
                    "HARD",
                    String.format("Room %s has %d lessons at %s", 
                        entry.getValue().get(0).getRoom().getName(),
                        entry.getValue().size(),
                        entry.getValue().get(0).getTimeSlot().toString()),
                    entry.getValue()
                );
                report.addViolation(violation);
            }
        }
    }
    
    private void validateTeacherConflicts() {
        // Check for overlapping lessons for same teacher
        for (Teacher teacher : solution.getTeachers()) {
            List<Lesson> teacherLessons = assignedLessons.stream()
                .filter(lesson -> lesson.getTeacher().equals(teacher))
                .collect(Collectors.toList());
                
            for (DayOfWeek day : DayOfWeek.values()) {
                List<Lesson> dayLessons = teacherLessons.stream()
                    .filter(lesson -> lesson.getTimeSlot().getDayOfWeek().equals(day))
                    .sorted(Comparator.comparing(lesson -> lesson.getTimeSlot().getStartTime()))
                    .collect(Collectors.toList());
                    
                for (int i = 0; i < dayLessons.size() - 1; i++) {
                    Lesson current = dayLessons.get(i);
                    Lesson next = dayLessons.get(i + 1);
                    
                    if (current.getTimeSlot().getEndTime().isAfter(next.getTimeSlot().getStartTime())) {
                        ConstraintViolation violation = new ConstraintViolation(
                            "Teacher Conflict",
                            "HARD",
                            String.format("Teacher %s has overlapping lessons on %s: %s and %s",
                                teacher.getName(), day,
                                current.getTimeSlot().toString(),
                                next.getTimeSlot().toString()),
                            Arrays.asList(current, next)
                        );
                        report.addViolation(violation);
                    }
                }
            }
        }
    }
    
    private void validateStudentGroupConflicts() {
        for (StudentGroup group : solution.getStudentGroups()) {
            List<Lesson> groupLessons = assignedLessons.stream()
                .filter(lesson -> lesson.getStudentGroup().equals(group))
                .collect(Collectors.toList());
                
            for (DayOfWeek day : DayOfWeek.values()) {
                List<Lesson> dayLessons = groupLessons.stream()
                    .filter(lesson -> lesson.getTimeSlot().getDayOfWeek().equals(day))
                    .sorted(Comparator.comparing(lesson -> lesson.getTimeSlot().getStartTime()))
                    .collect(Collectors.toList());
                    
                for (int i = 0; i < dayLessons.size() - 1; i++) {
                    Lesson current = dayLessons.get(i);
                    Lesson next = dayLessons.get(i + 1);
                    
                    // Check for overlaps considering batch splits
                    if (hasStudentGroupConflict(current, next)) {
                        ConstraintViolation violation = new ConstraintViolation(
                            "Student Group Conflict",
                            "HARD",
                            String.format("Student group %s has conflicting lessons on %s: %s and %s",
                                group.getName(), day,
                                getLessonDescription(current),
                                getLessonDescription(next)),
                            Arrays.asList(current, next)
                        );
                        report.addViolation(violation);
                    }
                }
            }
        }
    }
    
    private void validateRoomCapacity() {
        for (Lesson lesson : assignedLessons) {
            if (lesson.getRoom().getCapacity() < lesson.getRequiredCapacity()) {
                ConstraintViolation violation = new ConstraintViolation(
                    "Room Capacity",
                    "HARD",
                    String.format("Room %s (capacity: %d) is too small for lesson requiring %d students",
                        lesson.getRoom().getName(),
                        lesson.getRoom().getCapacity(),
                        lesson.getRequiredCapacity()),
                    Arrays.asList(lesson)
                );
                report.addViolation(violation);
            }
        }
    }
    
    private void validateLabTheoryRoomAssignment() {
        for (Lesson lesson : assignedLessons) {
            if (lesson.requiresLabRoom() && !lesson.getRoom().isLab()) {
                ConstraintViolation violation = new ConstraintViolation(
                    "Lab in Theory Room",
                    "HARD",
                    String.format("Lab lesson %s is assigned to theory room %s",
                        getLessonDescription(lesson),
                        lesson.getRoom().getName()),
                    Arrays.asList(lesson)
                );
                report.addViolation(violation);
            }
            
            if (lesson.requiresTheoryRoom() && lesson.getRoom().isLab()) {
                ConstraintViolation violation = new ConstraintViolation(
                    "Theory in Lab Room",
                    "HARD",
                    String.format("Theory lesson %s is assigned to lab room %s",
                        getLessonDescription(lesson),
                        lesson.getRoom().getName()),
                    Arrays.asList(lesson)
                );
                report.addViolation(violation);
            }
        }
    }
    
    private void validateLabTheoryTimeSlotAssignment() {
        for (Lesson lesson : assignedLessons) {
            if (lesson.requiresLabRoom() && !lesson.getTimeSlot().isLab()) {
                ConstraintViolation violation = new ConstraintViolation(
                    "Lab in Theory Slot",
                    "HARD",
                    String.format("Lab lesson %s is assigned to theory time slot %s",
                        getLessonDescription(lesson),
                        lesson.getTimeSlot().toString()),
                    Arrays.asList(lesson)
                );
                report.addViolation(violation);
            }
            
            if (lesson.requiresTheoryRoom() && lesson.getTimeSlot().isLab()) {
                ConstraintViolation violation = new ConstraintViolation(
                    "Theory in Lab Slot",
                    "HARD",
                    String.format("Theory lesson %s is assigned to lab time slot %s",
                        getLessonDescription(lesson),
                        lesson.getTimeSlot().toString()),
                    Arrays.asList(lesson)
                );
                report.addViolation(violation);
            }
        }
    }
    
    private void validateSameTeacherSameCourseConflicts() {
        Map<String, List<Lesson>> teacherCourseTimeMap = new HashMap<>();
        
        for (Lesson lesson : assignedLessons) {
            String key = lesson.getTeacher().getId() + "_" + 
                        lesson.getCourse().getId() + "_" + 
                        lesson.getTimeSlot().getId();
            teacherCourseTimeMap.computeIfAbsent(key, k -> new ArrayList<>()).add(lesson);
        }
        
        for (Map.Entry<String, List<Lesson>> entry : teacherCourseTimeMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                ConstraintViolation violation = new ConstraintViolation(
                    "Same Teacher Same Course Conflict",
                    "HARD",
                    String.format("Teacher %s has %d instances of course %s at the same time",
                        entry.getValue().get(0).getTeacher().getName(),
                        entry.getValue().size(),
                        entry.getValue().get(0).getCourse().getName()),
                    entry.getValue()
                );
                report.addViolation(violation);
            }
        }
    }
    
    private void validateLabTheoryOverlaps() {
        for (StudentGroup group : solution.getStudentGroups()) {
            List<Lesson> groupLessons = assignedLessons.stream()
                .filter(lesson -> lesson.getStudentGroup().equals(group))
                .collect(Collectors.toList());
                
            for (Course course : solution.getCourses()) {
                List<Lesson> courseLessons = groupLessons.stream()
                    .filter(lesson -> lesson.getCourse().equals(course))
                    .collect(Collectors.toList());
                    
                for (DayOfWeek day : DayOfWeek.values()) {
                    List<Lesson> dayCourseLessons = courseLessons.stream()
                        .filter(lesson -> lesson.getTimeSlot().getDayOfWeek().equals(day))
                        .collect(Collectors.toList());
                        
                    for (int i = 0; i < dayCourseLessons.size() - 1; i++) {
                        for (int j = i + 1; j < dayCourseLessons.size(); j++) {
                            Lesson lesson1 = dayCourseLessons.get(i);
                            Lesson lesson2 = dayCourseLessons.get(j);
                            
                            if (lesson1.requiresLabRoom() != lesson2.requiresLabRoom() &&
                                lessonsOverlap(lesson1, lesson2)) {
                                ConstraintViolation violation = new ConstraintViolation(
                                    "Lab-Theory Overlap",
                                    "HARD",
                                    String.format("Lab and theory sessions of course %s overlap for group %s on %s",
                                        course.getName(), group.getName(), day),
                                    Arrays.asList(lesson1, lesson2)
                                );
                                report.addViolation(violation);
                            }
                        }
                    }
                }
            }
        }
    }
    
    // ############################################################################
    // Soft Constraint Validations
    // ############################################################################
    
    private void validateTeacherWorkload() {
        for (Teacher teacher : solution.getTeachers()) {
            List<Lesson> teacherLessons = assignedLessons.stream()
                .filter(lesson -> lesson.getTeacher().equals(teacher))
                .collect(Collectors.toList());
                
            int totalHours = teacherLessons.stream()
                .mapToInt(Lesson::getEffectiveHours)
                .sum();
                
            if (totalHours > teacher.getMaxHours()) {
                ConstraintViolation violation = new ConstraintViolation(
                    "Teacher Workload Exceeded",
                    "SOFT",
                    String.format("Teacher %s has %d hours assigned (max: %d)",
                        teacher.getName(), totalHours, teacher.getMaxHours()),
                    teacherLessons
                );
                report.addViolation(violation);
            }
        }
    }
    
    private void validateTeacherWorkdaySpan() {
        for (Teacher teacher : solution.getTeachers()) {
            List<Lesson> teacherLessons = assignedLessons.stream()
                .filter(lesson -> lesson.getTeacher().equals(teacher))
                .collect(Collectors.toList());
                
            for (DayOfWeek day : DayOfWeek.values()) {
                List<Lesson> dayLessons = teacherLessons.stream()
                    .filter(lesson -> lesson.getTimeSlot().getDayOfWeek().equals(day))
                    .collect(Collectors.toList());
                    
                if (!dayLessons.isEmpty()) {
                    LocalTime earliest = dayLessons.stream()
                        .map(lesson -> lesson.getTimeSlot().getStartTime())
                        .min(LocalTime::compareTo)
                        .orElse(LocalTime.MIDNIGHT);
                        
                    LocalTime latest = dayLessons.stream()
                        .map(lesson -> lesson.getTimeSlot().getEndTime())
                        .max(LocalTime::compareTo)
                        .orElse(LocalTime.MIDNIGHT);
                        
                    Duration span = Duration.between(earliest, latest);
                    if (span.toHours() > 8) {
                        ConstraintViolation violation = new ConstraintViolation(
                            "Teacher Workday Span Too Long",
                            "SOFT",
                            String.format("Teacher %s has %.1f hour span on %s (%s to %s)",
                                teacher.getName(), span.toMinutes() / 60.0, day, earliest, latest),
                            dayLessons
                        );
                        report.addViolation(violation);
                    }
                }
            }
        }
    }
    
    private void validateTeacherBreaks() {
        for (Teacher teacher : solution.getTeachers()) {
            List<Lesson> teacherLessons = assignedLessons.stream()
                .filter(lesson -> lesson.getTeacher().equals(teacher))
                .collect(Collectors.toList());
                
            for (DayOfWeek day : DayOfWeek.values()) {
                List<Lesson> dayLessons = teacherLessons.stream()
                    .filter(lesson -> lesson.getTimeSlot().getDayOfWeek().equals(day))
                    .sorted(Comparator.comparing(lesson -> lesson.getTimeSlot().getStartTime()))
                    .collect(Collectors.toList());
                    
                for (int i = 0; i < dayLessons.size() - 1; i++) {
                    Lesson current = dayLessons.get(i);
                    Lesson next = dayLessons.get(i + 1);
                    
                    Duration gap = Duration.between(current.getTimeSlot().getEndTime(), 
                                                  next.getTimeSlot().getStartTime());
                                                  
                    if (gap.toMinutes() < 10 && gap.toMinutes() > 0) {
                        ConstraintViolation violation = new ConstraintViolation(
                            "Insufficient Break",
                            "SOFT",
                            String.format("Teacher %s has only %d minutes break between lessons on %s",
                                teacher.getName(), gap.toMinutes(), day),
                            Arrays.asList(current, next)
                        );
                        report.addViolation(violation);
                    }
                }
            }
        }
    }
    
    private void validateLateClasses() {
        for (Lesson lesson : assignedLessons) {
            if (lesson.getTimeSlot().getStartTime().getHour() >= 17) {
                ConstraintViolation violation = new ConstraintViolation(
                    "Late Class",
                    "SOFT",
                    String.format("Lesson %s starts at %s (after 5 PM)",
                        getLessonDescription(lesson),
                        lesson.getTimeSlot().getStartTime()),
                    Arrays.asList(lesson)
                );
                report.addViolation(violation);
            }
        }
    }
    
    private void validateTeacherTravelTime() {
        for (Teacher teacher : solution.getTeachers()) {
            List<Lesson> teacherLessons = assignedLessons.stream()
                .filter(lesson -> lesson.getTeacher().equals(teacher))
                .collect(Collectors.toList());
                
            for (DayOfWeek day : DayOfWeek.values()) {
                List<Lesson> dayLessons = teacherLessons.stream()
                    .filter(lesson -> lesson.getTimeSlot().getDayOfWeek().equals(day))
                    .sorted(Comparator.comparing(lesson -> lesson.getTimeSlot().getStartTime()))
                    .collect(Collectors.toList());
                    
                for (int i = 0; i < dayLessons.size() - 1; i++) {
                    Lesson current = dayLessons.get(i);
                    Lesson next = dayLessons.get(i + 1);
                    
                    Duration gap = Duration.between(current.getTimeSlot().getEndTime(), 
                                                  next.getTimeSlot().getStartTime());
                                                  
                    if (gap.toMinutes() == 0 && 
                        !current.getRoom().getBlock().equals(next.getRoom().getBlock())) {
                        ConstraintViolation violation = new ConstraintViolation(
                            "Teacher Travel Time",
                            "SOFT",
                            String.format("Teacher %s has consecutive lessons in different blocks: %s to %s on %s",
                                teacher.getName(), 
                                current.getRoom().getBlock(),
                                next.getRoom().getBlock(),
                                day),
                            Arrays.asList(current, next)
                        );
                        report.addViolation(violation);
                    }
                }
            }
        }
    }
    
    // ############################################################################
    // Analytics
    // ############################################################################
    
    private void analyzeResourceUtilization() {
        // Room utilization
        Map<Room, Integer> roomUsage = new HashMap<>();
        for (Room room : solution.getRooms()) {
            long usage = assignedLessons.stream()
                .filter(lesson -> lesson.getRoom().equals(room))
                .count();
            roomUsage.put(room, (int) usage);
        }
        report.setRoomUtilization(roomUsage);
        
        // Time slot utilization
        Map<TimeSlot, Integer> slotUsage = new HashMap<>();
        for (TimeSlot slot : solution.getTimeSlots()) {
            long usage = assignedLessons.stream()
                .filter(lesson -> lesson.getTimeSlot().equals(slot))
                .count();
            slotUsage.put(slot, (int) usage);
        }
        report.setTimeSlotUtilization(slotUsage);
    }
    
    private void analyzeTeacherLoad() {
        Map<Teacher, TeacherAnalytics> teacherAnalytics = new HashMap<>();
        
        for (Teacher teacher : solution.getTeachers()) {
            List<Lesson> teacherLessons = assignedLessons.stream()
                .filter(lesson -> lesson.getTeacher().equals(teacher))
                .collect(Collectors.toList());
                
            TeacherAnalytics analytics = new TeacherAnalytics();
            analytics.setTotalLessons(teacherLessons.size());
            analytics.setTotalHours(teacherLessons.stream().mapToInt(Lesson::getEffectiveHours).sum());
            
            // Count lessons per day
            Map<DayOfWeek, Integer> dailyLoad = new HashMap<>();
            for (DayOfWeek day : DayOfWeek.values()) {
                int dayLessons = (int) teacherLessons.stream()
                    .filter(lesson -> lesson.getTimeSlot().getDayOfWeek().equals(day))
                    .count();
                dailyLoad.put(day, dayLessons);
            }
            analytics.setDailyLoad(dailyLoad);
            
            // Count different courses
            Set<Course> courses = teacherLessons.stream()
                .map(Lesson::getCourse)
                .collect(Collectors.toSet());
            analytics.setDifferentCourses(courses.size());
            
            teacherAnalytics.put(teacher, analytics);
        }
        
        report.setTeacherAnalytics(teacherAnalytics);
    }
    
    private void analyzeStudentGroupSchedules() {
        Map<StudentGroup, StudentGroupAnalytics> groupAnalytics = new HashMap<>();
        
        for (StudentGroup group : solution.getStudentGroups()) {
            List<Lesson> groupLessons = assignedLessons.stream()
                .filter(lesson -> lesson.getStudentGroup().equals(group))
                .collect(Collectors.toList());
                
            StudentGroupAnalytics analytics = new StudentGroupAnalytics();
            analytics.setTotalLessons(groupLessons.size());
            
            // Count lessons per day
            Map<DayOfWeek, Integer> dailyLoad = new HashMap<>();
            for (DayOfWeek day : DayOfWeek.values()) {
                int dayLessons = (int) groupLessons.stream()
                    .filter(lesson -> lesson.getTimeSlot().getDayOfWeek().equals(day))
                    .count();
                dailyLoad.put(day, dayLessons);
            }
            analytics.setDailyLoad(dailyLoad);
            
            // Count different courses
            Set<Course> courses = groupLessons.stream()
                .map(Lesson::getCourse)
                .collect(Collectors.toSet());
            analytics.setDifferentCourses(courses.size());
            
            groupAnalytics.put(group, analytics);
        }
        
        report.setStudentGroupAnalytics(groupAnalytics);
    }
    
    // ############################################################################
    // Helper Methods
    // ############################################################################
    
    private boolean hasStudentGroupConflict(Lesson lesson1, Lesson lesson2) {
        if (!lessonsOverlap(lesson1, lesson2)) {
            return false;
        }
        
        boolean l1HasBatch = lesson1.isSplitBatch();
        boolean l2HasBatch = lesson2.isSplitBatch();
        
        if (!l1HasBatch && !l2HasBatch) {
            return true; // Two full-group lessons overlap
        }
        if (l1HasBatch != l2HasBatch) {
            return true; // Full-group and batched lesson overlap
        }
        if (l1HasBatch && l2HasBatch) {
            return lesson1.getLabBatch().equals(lesson2.getLabBatch()); // Same batch overlap
        }
        return false;
    }
    
    private boolean lessonsOverlap(Lesson lesson1, Lesson lesson2) {
        LocalTime start1 = lesson1.getTimeSlot().getStartTime();
        LocalTime end1 = lesson1.getTimeSlot().getEndTime();
        LocalTime start2 = lesson2.getTimeSlot().getStartTime();
        LocalTime end2 = lesson2.getTimeSlot().getEndTime();
        
        return start1.isBefore(end2) && start2.isBefore(end1);
    }
    
    private String getLessonDescription(Lesson lesson) {
        return String.format("%s - %s (%s%s)",
            lesson.getCourse().getName(),
            lesson.getSessionType(),
            lesson.isSplitBatch() ? "Batch " + lesson.getLabBatch() : "Full Group",
            lesson.getTimeSlot() != null ? " at " + lesson.getTimeSlot().toString() : "");
    }
} 