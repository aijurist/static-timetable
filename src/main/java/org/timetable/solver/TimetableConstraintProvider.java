package org.timetable.solver;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.Joiners;
import org.timetable.config.TimetableConfig;
import org.timetable.config.DepartmentBlockConfig;
import org.timetable.domain.*;
import org.timetable.persistence.CourseLabMappingUtil;

import java.time.Duration;
import java.util.Objects;

import static org.optaplanner.core.api.score.stream.ConstraintCollectors.*;

public class TimetableConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[]{
                // --- Critical Hard Constraints ---
                
                // HIGHEST PRIORITY: Core lab mapping must be enforced
                strictCoreLabMapping(constraintFactory),
                
                // Basic hard constraints
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

                // --- Soft Constraints ---
                
                // Core lab priority preferences (prefer lab_1 over lab_2 over lab_3)
                coreLabPriorityPreference(constraintFactory),
                
                // Department block preferences (minimize student travel time)
                departmentBlockPreference(constraintFactory),
                
                teacherMaxWeeklyHours(constraintFactory),
                teacherWorkdaySpan(constraintFactory),
                penalizePairedLabInDifferentSlots(constraintFactory)
        };
    }

    // ############################################################################
    // Critical Hard Constraints
    // ############################################################################

    /**
     * CRITICAL: Enforce core lab mapping - core courses MUST use their designated labs
     * This is the highest priority constraint to ensure no violations
     */
    private Constraint strictCoreLabMapping(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getSessionType().equals("lab"))
                .filter(lesson -> lesson.getRoom() != null)
                .filter(lesson -> CourseLabMappingUtil.isCoreLabCourse(lesson.getCourse().getCode()))
                .filter(lesson -> {
                    String courseCode = lesson.getCourse().getCode();
                    String roomDesc = lesson.getRoom().getDescription();
                    
                    // Check if this room is allowed for this course
                    boolean isAllowed = CourseLabMappingUtil.isRoomAllowedForCourse(courseCode, roomDesc);
                    
                //     if (!isAllowed) {
                //         // Log violation for debugging
                //         System.err.println("CORE LAB VIOLATION: Course " + courseCode + 
                //                          " assigned to disallowed lab: " + roomDesc + 
                //                          ". Allowed: " + CourseLabMappingUtil.getPriorityOrderedLabs(courseCode));
                //     }
                    
                    return !isAllowed; // Return true if this is a violation
                })
                .penalize(HardSoftScore.ONE_HARD.multiply(1000000)) // Maximum penalty
                .asConstraint("CRITICAL: Core lab mapping");
    }

    // ############################################################################
    // Basic Hard Constraints
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
    // Soft Constraints
    // ############################################################################

    /**
     * SOFT: Prefer higher priority labs for core courses (lab_1 > lab_2 > lab_3)
     */
    private Constraint coreLabPriorityPreference(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getSessionType().equals("lab"))
                .filter(lesson -> lesson.getRoom() != null)
                .filter(lesson -> CourseLabMappingUtil.isCoreLabCourse(lesson.getCourse().getCode()))
                .penalize(HardSoftScore.ONE_SOFT.multiply(50), 
                        lesson -> CourseLabMappingUtil.getPriorityPenalty(
                                lesson.getCourse().getCode(), 
                                lesson.getRoom().getDescription()))
                .asConstraint("Core lab priority preference");
    }

    /**
     * SOFT: Prefer assigning theory/tutorial sessions to rooms in the department's preferred block
     * to minimize student travel time between classes.
     */
    private Constraint departmentBlockPreference(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.requiresTheoryRoom()) // Only theory and tutorial sessions
                .filter(lesson -> lesson.getRoom() != null && lesson.getStudentGroup() != null)
                .filter(lesson -> DepartmentBlockConfig.hasBlockPreference(lesson.getStudentGroup().getDepartment()))
                .penalize(HardSoftScore.ONE_SOFT.multiply(10), 
                        lesson -> DepartmentBlockConfig.getBlockPreferencePenalty(
                                lesson.getStudentGroup().getDepartment(), 
                                lesson.getRoom().getBlock()))
                .asConstraint("Department block preference");
    }

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
                .filter((teacher, day, minTime, maxTime) -> Duration.between(minTime, maxTime).toHours() > 8)
                .penalize(HardSoftScore.ONE_SOFT,
                        (teacher, day, minTime, maxTime) -> (int) (Duration.between(minTime, maxTime).toMinutes() - 480))
                .asConstraint("Teacher workday span too long");
    }

    private Constraint penalizePairedLabInDifferentSlots(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getStudentGroup),
                        Joiners.equal(Lesson::getCourse),
                        Joiners.filtering((l1, l2) -> l1.isSplitBatch() && l2.isSplitBatch() && !l1.getLabBatch().equals(l2.getLabBatch()))
                )
                .filter((lesson1, lesson2) -> !Objects.equals(lesson1.getTimeSlot(), lesson2.getTimeSlot()))
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Paired lab batches in different slots");
    }
}