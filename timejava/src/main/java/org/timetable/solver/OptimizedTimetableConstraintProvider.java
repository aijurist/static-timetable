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

import java.time.Duration;
import java.util.Objects;

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
                
                // --- Soft Constraints (Performance Optimized) ---
                teacherMaxWeeklyHours(constraintFactory),
                teacherWorkdaySpan(constraintFactory),
                penalizePairedLabInDifferentSlots(constraintFactory),
                
                // --- Enhanced Soft Constraints ---
                preferTeacherTimePreferences(constraintFactory),
                balanceTeacherDailyLoad(constraintFactory),
                minimizeTeacherTravelTime(constraintFactory),
                preferConsecutiveLessons(constraintFactory),
                avoidLateClasses(constraintFactory)
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
     * Avoid late classes (after 5 PM)
     */
    private Constraint avoidLateClasses(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeSlot() != null && 
                        lesson.getTimeSlot().getStartTime().getHour() >= 17)
                .penalize(HardSoftScore.of(0, 1))
                .asConstraint("Avoid late classes");
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
} 