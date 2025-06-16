package org.timetable.solver;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.Joiners;
import org.timetable.domain.Lesson;
import org.timetable.domain.Teacher;
import org.timetable.domain.TimeSlot;
import org.timetable.config.TimetableConfig;
import org.timetable.config.DepartmentWorkdayConfig;
import org.timetable.persistence.CourseLabMappingUtil;
import org.timetable.domain.Room;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;

import static org.optaplanner.core.api.score.stream.ConstraintCollectors.*;

/**
 * Optimized constraint provider with enhanced scheduling logic
 * Focuses on core timetabling constraints without A105-specific requirements
 */
public class OptimizedTimetableConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[]{
                // --- Core Hard Constraints ---
                roomConflict(constraintFactory),
                teacherConflict(constraintFactory),
                studentGroupConflict(constraintFactory),
                roomCapacity(constraintFactory),
                labInLabRoom(constraintFactory),
                theoryInTheoryRoom(constraintFactory),
                labInLabSlot(constraintFactory),
                theoryInTheorySlot(constraintFactory),
                lectureOrTutorialMustBeForFullGroup(constraintFactory),
                labForLargeGroupMustBeBatched(constraintFactory),
                
                // --- Enhanced Hard Constraints ---
                sameTeacherSameCourseConflict(constraintFactory),
                labTheoryOverlapConflict(constraintFactory),
                minimumBreakBetweenClasses(constraintFactory),
                teacherMaxConsecutiveHours(constraintFactory),
                
                // --- Department Workday Constraints ---
                departmentOutsideAllowedDays(constraintFactory),
                
                // --- Soft Constraints (Performance Optimized) ---
                teacherMaxWeeklyHours(constraintFactory),
                teacherWorkdaySpan(constraintFactory),
                penalizePairedLabInDifferentSlots(constraintFactory),
                
                // --- Enhanced Soft Constraints ---
                preferTeacherTimePreferences(constraintFactory),
                balanceTeacherDailyLoad(constraintFactory),
                minimizeTeacherTravelTime(constraintFactory),
                preferConsecutiveLessons(constraintFactory),
                specialRoomForAuto(constraintFactory),
                computerDepartmentsMustUseComputerLabs(constraintFactory),
                coreDepartmentsMustUseCoreOrComputerLabs(constraintFactory),
                courseLabMustMatchMapping(constraintFactory),
                preventRandomLabAssignmentForMappedCourses(constraintFactory),
                studentGroupShiftPattern(constraintFactory),
                
                // --- Department Workday Preferences ---
                preferHotspotLabsOnMonday(constraintFactory)
        };
    }

    // ############################################################################
    // Core Hard Constraints (Unchanged for Stability)
    // ############################################################################

    private Constraint roomConflict(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getTimeSlot),
                        Joiners.equal(Lesson::getRoom))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Room conflict");
    }

    private Constraint teacherConflict(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getTeacher),
                        Joiners.equal(l -> l.getTimeSlot().getDayOfWeek()),
                        Joiners.overlapping(Lesson::getStartTime, Lesson::getEndTime))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Teacher conflict");
    }

    private Constraint studentGroupConflict(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getStudentGroup),
                        Joiners.equal(l -> l.getTimeSlot().getDayOfWeek()),
                        Joiners.overlapping(Lesson::getStartTime, Lesson::getEndTime))
                .filter((lesson1, lesson2) -> {
                    boolean l1HasBatch = lesson1.isSplitBatch();
                    boolean l2HasBatch = lesson2.isSplitBatch();

                    if (!l1HasBatch && !l2HasBatch) { // Two full-group lessons (L/T). CONFLICT.
                        return true;
                    }
                    if (l1HasBatch != l2HasBatch) { // A full-group lesson and a batched lab. CONFLICT.
                        return true;
                    }
                    if (l1HasBatch && l2HasBatch) { // Two batched labs. Conflict only if for the SAME batch.
                        return lesson1.getLabBatch().equals(lesson2.getLabBatch());
                    }
                    return false;
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Student group conflict");
    }

    private Constraint roomCapacity(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Lesson.class)
                .filter(lesson -> lesson.getRoom() != null && 
                        lesson.getRoom().getCapacity() < lesson.getRequiredCapacity())
                .penalize(HardSoftScore.ONE_HARD,
                        (lesson) -> lesson.getRequiredCapacity() - lesson.getRoom().getCapacity())
                .asConstraint("Room capacity");
    }

    private Constraint labInLabRoom(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Lesson.class)
                .filter(lesson -> lesson.requiresLabRoom() && 
                        lesson.getRoom() != null && !lesson.getRoom().isLab())
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Lab in a theory room");
    }

    private Constraint theoryInTheoryRoom(ConstraintFactory constraintFactory) {  
        return constraintFactory
                .forEach(Lesson.class)
                .filter(lesson -> lesson.requiresTheoryRoom() && 
                        lesson.getRoom() != null && lesson.getRoom().isLab())
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Theory in a lab room");
    }

    private Constraint labInLabSlot(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Lesson.class)
                .filter(lesson -> lesson.requiresLabRoom() && 
                        lesson.getTimeSlot() != null && !lesson.getTimeSlot().isLab())
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Lab in theory slot");
    }

    private Constraint theoryInTheorySlot(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Lesson.class)
                .filter(lesson -> lesson.requiresTheoryRoom() && 
                        lesson.getTimeSlot() != null && lesson.getTimeSlot().isLab())
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Theory in lab slot");
    }

    private Constraint lectureOrTutorialMustBeForFullGroup(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Lesson.class)
                .filter(lesson -> lesson.requiresTheoryRoom() && lesson.isSplitBatch())
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Lecture/Tutorial assigned to a batch");
    }

    private Constraint labForLargeGroupMustBeBatched(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Lesson.class)
                .filter(lesson -> lesson.requiresLabRoom()
                        && lesson.getStudentGroup().getSize() > TimetableConfig.LAB_BATCH_SIZE
                        && !lesson.isSplitBatch())
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Lab for large group must be batched");
    }

    // ############################################################################
    // Enhanced Hard Constraints
    // ############################################################################

    /**
     * Prevent the same teacher from teaching the same course at the same time
     */
    private Constraint sameTeacherSameCourseConflict(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getTeacher),
                        Joiners.equal(Lesson::getCourse),
                        Joiners.equal(Lesson::getTimeSlot))
                .penalize(HardSoftScore.of(5, 0)) // Reduced from 10
                .asConstraint("Same teacher same course conflict");
    }

    /**
     * Prevent lab and theory sessions of the same course from overlapping for the same student group
     */
    private Constraint labTheoryOverlapConflict(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getStudentGroup),
                        Joiners.equal(Lesson::getCourse),
                        Joiners.equal(l -> l.getTimeSlot().getDayOfWeek()),
                        Joiners.overlapping(Lesson::getStartTime, Lesson::getEndTime))
                .filter((lesson1, lesson2) -> 
                        lesson1.requiresLabRoom() != lesson2.requiresLabRoom()) // One is lab, other is theory
                .penalize(HardSoftScore.of(3, 0)) // Reduced from 10
                .asConstraint("Lab theory overlap conflict");
    }

    /**
     * Ensure minimum break between classes for teachers (15 minutes)
     */
    private Constraint minimumBreakBetweenClasses(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getTeacher),
                        Joiners.equal(l -> l.getTimeSlot().getDayOfWeek()))
                .filter((lesson1, lesson2) -> {
                    if (lesson1.getTimeSlot() == null || lesson2.getTimeSlot() == null) return false;
                    
                    java.time.LocalTime end1 = lesson1.getTimeSlot().getEndTime();
                    java.time.LocalTime start2 = lesson2.getTimeSlot().getStartTime();
                    java.time.LocalTime end2 = lesson2.getTimeSlot().getEndTime();
                    java.time.LocalTime start1 = lesson1.getTimeSlot().getStartTime();
                    
                    // Check if lessons are adjacent with no break
                    return (end1.equals(start2) || end2.equals(start1)) ||
                           (Duration.between(end1, start2).toMinutes() < 15 && Duration.between(end1, start2).toMinutes() > 0) ||
                           (Duration.between(end2, start1).toMinutes() < 15 && Duration.between(end2, start1).toMinutes() > 0);
                })
                .penalize(HardSoftScore.of(1, 0)) // Light penalty
                .asConstraint("Minimum break between classes");
    }

    /**
     * Limit teacher's consecutive hours to avoid fatigue
     */
    private Constraint teacherMaxConsecutiveHours(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getTeacher),
                        Joiners.equal(l -> l.getTimeSlot().getDayOfWeek()))
                .filter((lesson1, lesson2) -> areConsecutiveLessons(lesson1, lesson2))
                .groupBy((lesson1, lesson2) -> lesson1.getTeacher(),
                        (lesson1, lesson2) -> lesson1.getTimeSlot().getDayOfWeek(),
                        countBi())
                .filter((teacher, day, consecutiveCount) -> consecutiveCount > 3)
                .penalize(HardSoftScore.of(2, 0), // Moderate penalty
                        (teacher, day, consecutiveCount) -> consecutiveCount - 3)
                .asConstraint("Teacher max consecutive hours");
    }

    // ############################################################################
    // Soft Constraints (Performance Optimized)
    // ############################################################################

    private Constraint teacherMaxWeeklyHours(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Lesson.class)
                .groupBy(Lesson::getTeacher, sum(Lesson::getEffectiveHours))
                .filter((teacher, totalHours) -> totalHours > teacher.getMaxHours())
                .penalize(HardSoftScore.ONE_SOFT,
                        (teacher, totalHours) -> totalHours - teacher.getMaxHours())
                .asConstraint("Teacher max weekly hours");
    }

    private Constraint teacherWorkdaySpan(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Lesson.class)
                .groupBy(Lesson::getTeacher, 
                        (Lesson lesson) -> lesson.getTimeSlot().getDayOfWeek(),
                        min((Lesson lesson) -> lesson.getTimeSlot().getStartTime()),
                        max((Lesson lesson) -> lesson.getTimeSlot().getEndTime())
                )
                .filter((teacher, day, minTime, maxTime) -> 
                        minTime != null && maxTime != null && 
                        Duration.between(minTime, maxTime).toHours() > 8)
                .penalize(HardSoftScore.ONE_SOFT,
                        (teacher, day, minTime, maxTime) -> 
                                (int) (Duration.between(minTime, maxTime).toMinutes() - 480))
                .asConstraint("Teacher workday span too long");
    }

    private Constraint penalizePairedLabInDifferentSlots(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getStudentGroup),
                        Joiners.equal(Lesson::getCourse),
                        Joiners.filtering((l1, l2) -> l1.isSplitBatch() && l2.isSplitBatch() && 
                                !l1.getLabBatch().equals(l2.getLabBatch()))
                )
                .filter((lesson1, lesson2) -> !Objects.equals(lesson1.getTimeSlot(), lesson2.getTimeSlot()))
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Paired lab batches in different slots");
    }

    // ############################################################################
    // Enhanced Soft Constraints
    // ############################################################################

    /**
     * Prefer teacher time preferences (morning vs afternoon)
     */
    private Constraint preferTeacherTimePreferences(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeSlot() != null && 
                        !isPreferredTimeForTeacher(lesson.getTeacher(), lesson.getTimeSlot()))
                .penalize(HardSoftScore.of(0, 5)) // Reduced penalty
                .asConstraint("Teacher time preferences");
    }

    /**
     * Balance teacher daily load
     */
    private Constraint balanceTeacherDailyLoad(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Lesson.class)
                .groupBy(Lesson::getTeacher, 
                        l -> l.getTimeSlot().getDayOfWeek(),
                        sum(Lesson::getEffectiveHours))
                .filter((teacher, day, dailyHours) -> dailyHours > 6)
                .penalize(HardSoftScore.of(0, 3), // Reduced penalty
                        (teacher, day, dailyHours) -> dailyHours - 6)
                .asConstraint("Balance teacher daily load");
    }

    /**
     * Minimize teacher travel time between rooms
     */
    private Constraint minimizeTeacherTravelTime(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getTeacher),
                        Joiners.equal(l -> l.getTimeSlot().getDayOfWeek()))
                .filter((lesson1, lesson2) -> {
                    if (lesson1.getRoom() == null || lesson2.getRoom() == null) return false;
                    return areConsecutiveLessons(lesson1, lesson2) && 
                           !lesson1.getRoom().getBlock().equals(lesson2.getRoom().getBlock());
                })
                .penalize(HardSoftScore.of(0, 2)) // Reduced penalty
                .asConstraint("Minimize teacher travel time");
    }

    /**
     * Prefer consecutive lessons for the same course
     */
    private Constraint preferConsecutiveLessons(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getStudentGroup),
                        Joiners.equal(Lesson::getCourse),
                        Joiners.equal(l -> l.getTimeSlot().getDayOfWeek()))
                .filter((lesson1, lesson2) -> areConsecutiveLessons(lesson1, lesson2))
                .reward(HardSoftScore.of(0, 1)) // Reduced reward
                .asConstraint("Prefer consecutive lessons");
    }

    /**
     * HARD: Room numbers 61, 62, C108 in block C are restricted to Automobile department (AUTO).
     */
    private Constraint specialRoomForAuto(ConstraintFactory constraintFactory) {
        Set<String> SPECIAL_ROOMS = java.util.Set.of("61", "62", "C108");
        return constraintFactory
                .forEach(Lesson.class)
                .filter(lesson -> lesson.getRoom() != null && SPECIAL_ROOMS.contains(lesson.getRoom().getName()))
                .filter(lesson -> !"AUTO".equalsIgnoreCase(lesson.getStudentGroup().getDepartment()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Special room AUTO only");
    }

    // NEW: Encourage a 2-2-1 shift distribution (permutation of two days in two different shifts and one day in the third)
    private Constraint teacherShiftPattern(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Lesson.class)
                .groupBy(Lesson::getTeacher, toList())
                .filter((teacher, lessons) -> calculateShiftPenalty(lessons) > 0)
                .penalize(HardSoftScore.of(0, 1),
                        (teacher, lessons) -> calculateShiftPenalty(lessons))
                .asConstraint("Teacher shift pattern");
    }

    /**
     * Calculates a penalty based on how far a teacher's weekly timetable deviates from the desired 2-2-1 shift distribution.
     * <p>
     * Shift buckets:
     *   0 – Early shift  (first lesson starts before 10:00)  →  8-3 shift
     *   1 – Mid shift    (first lesson starts between 10:00 ‑ 11:59) → 10-5 shift
     *   2 – Late shift   (first lesson starts at/after 12:00) → 12-7 shift
     * <p>
     * For every week day we only consider the EARLIEST lesson start time when classifying the shift for that day.
     * The ideal distribution for the five working days is two days in two of the buckets and one day in the remaining bucket (any permutation).
     * The function returns 0 if the ideal distribution is achieved; otherwise it returns a positive integer representing the magnitude of deviation.
     */
    private static int calculateShiftPenalty(java.util.List<Lesson> lessons) {
        // Earliest start time per day
        java.util.EnumMap<java.time.DayOfWeek, java.time.LocalTime> earliestByDay = new java.util.EnumMap<>(java.time.DayOfWeek.class);
        for (Lesson l : lessons) {
            if (l.getTimeSlot() == null) continue;
            var ts = l.getTimeSlot();
            var day = ts.getDayOfWeek();
            var start = ts.getStartTime();
            var current = earliestByDay.get(day);
            if (current == null || start.isBefore(current)) {
                earliestByDay.put(day, start);
            }
        }
        // Count shifts based on earliest appearance per day
        int[] shiftCounts = new int[3]; // [early, mid, late]
        for (java.time.LocalTime start : earliestByDay.values()) {
            int bucket = classifyShift(start);
            shiftCounts[bucket]++;
        }
        // Desired pattern after sorting should be [1,2,2]
        int[] sorted = java.util.Arrays.stream(shiftCounts).sorted().toArray();
        if (sorted[0] == 1 && sorted[1] == 2 && sorted[2] == 2) {
            return 0; // perfect match
        }
        // Otherwise compute deviation penalty.
        int penalty = 0;
        for (int c : shiftCounts) {
            if (c == 0) {
                penalty += 2; // missing a shift bucket entirely
            } else if (c > 2) {
                penalty += c - 2; // more than 2 days in same shift
            }
            // count==1 or 2 is acceptable, so no penalty
        }
        return penalty * 10;
    }

    // Helper to map a lesson start time to a shift bucket (0,1,2)
    private static int classifyShift(java.time.LocalTime start) {
        int hour = start.getHour();
        if (hour < 10) {
            return 0; // 8-3 shift
        } else if (hour < 12) {
            return 1; // 10-5 shift
        } else {
            return 2; // 12-7 shift
        }
    }

    // ############################################################################
    // Helper Methods
    // ############################################################################

    private boolean isPreferredTimeForTeacher(Teacher teacher, TimeSlot timeSlot) {
        // Assume teachers prefer morning sessions (before 12 PM)
        return timeSlot.getStartTime().getHour() < 12;
    }

    private boolean areConsecutiveLessons(Lesson lesson1, Lesson lesson2) {
        if (lesson1.getTimeSlot() == null || lesson2.getTimeSlot() == null) return false;
        if (!lesson1.getTimeSlot().getDayOfWeek().equals(lesson2.getTimeSlot().getDayOfWeek())) return false;
        
        java.time.LocalTime end1 = lesson1.getTimeSlot().getEndTime();
        java.time.LocalTime start2 = lesson2.getTimeSlot().getStartTime();
        java.time.LocalTime end2 = lesson2.getTimeSlot().getEndTime();
        java.time.LocalTime start1 = lesson1.getTimeSlot().getStartTime();
        
        return end1.equals(start2) || end2.equals(start1);
    }

    // Lab type matching constraints
    Constraint computerDepartmentsMustUseComputerLabs(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getSessionType().equals("lab"))
                .filter(lesson -> isComputerDepartment(lesson.getStudentGroup().getDepartment()))
                .join(Room.class, Joiners.equal(Lesson::getRoom, room -> room))
                .filter((lesson, room) -> !"computer".equals(room.getLabType()))
                .penalize(HardSoftScore.ONE_HARD.multiply(100))
                .asConstraint("Computer departments must use computer labs");
    }

    Constraint coreDepartmentsMustUseCoreOrComputerLabs(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getSessionType().equals("lab"))
                .filter(lesson -> !isComputerDepartment(lesson.getStudentGroup().getDepartment()))
                .join(Room.class, Joiners.equal(Lesson::getRoom, room -> room))
                .filter((lesson, room) -> {
                    // Core departments can use core labs or computer labs (if mapped)
                    String labType = room.getLabType();
                    if ("core".equals(labType)) {
                        return false; // Core labs are always allowed
                    }
                    if ("computer".equals(labType)) {
                        // Check if this course is explicitly mapped to computer labs
                        return !CourseLabMappingUtil.isRoomAllowedForCourse(lesson.getCourse().getCode(), room.getDescription());
                    }
                    return true; // Other lab types not allowed
                })
                .penalize(HardSoftScore.ONE_HARD.multiply(50))
                .asConstraint("Core departments must use appropriate labs");
    }

    // Helper method to identify computer departments
    private boolean isComputerDepartment(String department) {
        return "CSE".equals(department) || "IT".equals(department) || "AIDS".equals(department) || "CSBS".equals(department);
    }

    // Enforce that courses with explicit lab mappings must be scheduled in one of their allowed labs
    private Constraint courseLabMustMatchMapping(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getSessionType().equals("lab"))
                .filter(lesson -> CourseLabMappingUtil.isCoreLabCourse(lesson.getCourse().getCode()))
                .filter(lesson -> lesson.getRoom() != null) // Only check assigned lessons
                .filter(lesson -> !CourseLabMappingUtil.isRoomAllowedForCourse(
                        lesson.getCourse().getCode(), lesson.getRoom().getDescription()))
                .penalize(HardSoftScore.ONE_HARD.multiply(10000)) // Maximum penalty
                .asConstraint("Course lab must match mapping");
    }

    // Prevent any lab assignment for mapped courses that's not in their allowed list
    private Constraint preventRandomLabAssignmentForMappedCourses(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.requiresLabRoom()) // Any lab lesson
                .filter(lesson -> CourseLabMappingUtil.isCoreLabCourse(lesson.getCourse().getCode()))
                .filter(lesson -> lesson.getRoom() != null)
                .filter(lesson -> {
                    // Check if this room is in the allowed list for this course
                    String courseCode = lesson.getCourse().getCode();
                    String roomDesc = lesson.getRoom().getDescription();
                    return !CourseLabMappingUtil.isRoomAllowedForCourse(courseCode, roomDesc);
                })
                .penalize(HardSoftScore.ONE_HARD.multiply(50000)) // Even higher penalty
                .asConstraint("Prevent random lab assignment for mapped courses");
    }

    // FIXED: Student group shift pattern now as soft constraint with improved penalty calculation
    private Constraint studentGroupShiftPattern(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Lesson.class)
                .groupBy(Lesson::getStudentGroup, toList())
                .filter((studentGroup, lessons) -> calculateStudentGroupShiftPenalty(lessons) > 0)
                .penalize(HardSoftScore.ONE_SOFT, // Changed from HARD to SOFT
                        (studentGroup, lessons) -> calculateStudentGroupShiftPenalty(lessons))
                .asConstraint("Student group shift pattern");
    }

    /**
     * Calculates a penalty based on how far a student group's weekly timetable deviates from the desired 2-2-1 shift distribution.
     * <p>
     * Shift buckets:
     *   0 – Early shift  (first lesson starts before 10:00)  →  8-3 shift
     *   1 – Mid shift    (first lesson starts between 10:00 ‑ 11:59) → 10-5 shift
     *   2 – Late shift   (first lesson starts at/after 12:00) → 12-7 shift
     * <p>
     * For every week day we only consider the EARLIEST lesson start time when classifying the shift for that day.
     * The ideal distribution for the five working days is two days in two of the buckets and one day in the remaining bucket (any permutation).
     * The function returns 0 if the ideal distribution is achieved; otherwise it returns a positive integer representing the magnitude of deviation.
     */
    private static int calculateStudentGroupShiftPenalty(java.util.List<Lesson> lessons) {
        // Earliest start time per day
        java.util.EnumMap<java.time.DayOfWeek, java.time.LocalTime> earliestByDay = new java.util.EnumMap<>(java.time.DayOfWeek.class);
        for (Lesson l : lessons) {
            if (l.getTimeSlot() == null) continue;
            var ts = l.getTimeSlot();
            var day = ts.getDayOfWeek();
            var start = ts.getStartTime();
            var current = earliestByDay.get(day);
            if (current == null || start.isBefore(current)) {
                earliestByDay.put(day, start);
            }
        }
        // Count shifts based on earliest appearance per day
        int[] shiftCounts = new int[3]; // [early, mid, late]
        for (java.time.LocalTime start : earliestByDay.values()) {
            int bucket = classifyShift(start);
            shiftCounts[bucket]++;
        }
        // Desired pattern after sorting should be [1,2,2]
        int[] sorted = java.util.Arrays.stream(shiftCounts).sorted().toArray();
        if (sorted[0] == 1 && sorted[1] == 2 && sorted[2] == 2) {
            return 0; // perfect match
        }
        // Otherwise compute deviation penalty.
        int penalty = 0;
        for (int c : shiftCounts) {
            if (c == 0) {
                penalty += 2; // missing a shift bucket entirely
            } else if (c > 2) {
                penalty += c - 2; // more than 2 days in same shift
            }
            // count==1 or 2 is acceptable, so no penalty
        }
        return penalty * 10;
    }

    // ############################################################################
    // Department Workday Constraints
    // ############################################################################

    /**
     * HARD: Ensure departments only schedule lessons on their allowed working days
     */
    private Constraint departmentOutsideAllowedDays(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeSlot() != null && lesson.getStudentGroup() != null)
                .filter(lesson -> !DepartmentWorkdayConfig.isAllowedDay(
                        lesson.getStudentGroup().getDepartment(), 
                        lesson.getTimeSlot().getDayOfWeek()))
                .penalize(HardSoftScore.ONE_HARD.multiply(1000)) // Very high penalty
                .asConstraint("Department outside allowed working days");
    }

    /**
     * SOFT: Prefer hotspot labs on Monday for Monday-Friday departments
     */
    private Constraint preferHotspotLabsOnMonday(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeSlot() != null && 
                        lesson.getRoom() != null && 
                        lesson.getStudentGroup() != null)
                .filter(lesson -> lesson.getTimeSlot().getDayOfWeek() == java.time.DayOfWeek.MONDAY)
                .filter(lesson -> DepartmentWorkdayConfig.isMondayFridayDepartment(
                        lesson.getStudentGroup().getDepartment()))
                .filter(lesson -> DepartmentWorkdayConfig.isHotspotLab(
                        lesson.getRoom().getDescription()))
                .reward(HardSoftScore.of(0, 50)) // Moderate reward
                .asConstraint("Prefer hotspot labs on Monday");
    }
}