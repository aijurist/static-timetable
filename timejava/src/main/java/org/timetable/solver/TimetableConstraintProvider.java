package org.timetable.solver;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.Joiners;
import org.timetable.config.TimetableConfig;
import org.timetable.domain.*;

import java.time.Duration;
import java.util.Objects;

import static org.optaplanner.core.api.score.stream.ConstraintCollectors.*;

public class TimetableConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[]{
                // --- Hard Constraints ---
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
                teacherMaxWeeklyHours(constraintFactory),
                teacherWorkdaySpan(constraintFactory),
                penalizePairedLabInDifferentSlots(constraintFactory)
        };
    }

    // ############################################################################
    // Hard Constraints
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
                .filter(lesson -> lesson.getRoom().getCapacity() < lesson.getRequiredCapacity())
                .penalize(HardSoftScore.ONE_HARD,
                        (lesson) -> lesson.getRequiredCapacity() - lesson.getRoom().getCapacity())
                .asConstraint("Room capacity");
    }

    private Constraint labInLabRoom(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Lesson.class)
                .filter(lesson -> lesson.requiresLabRoom() && !lesson.getRoom().isLab())
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Lab in a theory room");
    }

    private Constraint theoryInTheoryRoom(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Lesson.class)
                .filter(lesson -> lesson.requiresTheoryRoom() && lesson.getRoom().isLab())
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Theory in a lab room");
    }

    private Constraint labInLabSlot(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Lesson.class)
                .filter(lesson -> lesson.requiresLabRoom() && !lesson.getTimeSlot().isLab())
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Lab in theory slot");
    }

    private Constraint theoryInTheorySlot(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Lesson.class)
                .filter(lesson -> lesson.requiresTheoryRoom() && lesson.getTimeSlot().isLab())
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