package org.timetable.solver;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.Joiners;
import org.optaplanner.core.api.score.stream.ConstraintCollectors;
import org.timetable.domain.Lesson;
import org.timetable.domain.TimeSlot;

import java.time.Duration;
import java.time.LocalTime;

public class TimetableConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[]{
                // --- Hard Constraints ---
                roomConflict(factory),
                teacherConflict(factory),
                studentGroupConflict(factory),
                teacherMaxHours(factory),
                roomCapacity(factory),
                labInLabRoom(factory),
                theoryInClassroom(factory),
                labInLabTimeslot(factory),
                theoryInTheoryTimeslot(factory),
                lunchBreakConstraint(factory),
                morningBreakConstraint(factory),
                labBatchesInDifferentRooms(factory),
                unsplitLabRequiresLargeRoom(factory),

                // --- Soft Constraints ---
                teacherGaps(factory),
                studentGroupGaps(factory)
        };
    }

    // A lesson can't be in a room that is already occupied.
    private Constraint roomConflict(ConstraintFactory factory) {
        return factory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getTimeSlot),
                        Joiners.equal(Lesson::getRoom))
                .impact("Room conflict", HardSoftScore.ONE_HARD);
    }

    // A teacher can't teach two lessons at the same time.
    private Constraint teacherConflict(ConstraintFactory factory) {
        return factory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getTimeSlot),
                        Joiners.equal(Lesson::getTeacher))
                .impact("Teacher conflict", HardSoftScore.ONE_HARD);
    }

    // A student group can't have two lessons at the same time.
    private Constraint studentGroupConflict(ConstraintFactory factory) {
        return factory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getTimeSlot),
                        Joiners.equal(Lesson::getStudentGroup))
                .impact("Student group conflict", HardSoftScore.ONE_HARD);
    }

    // A teacher cannot work more than their specified maximum hours per week.
    private Constraint teacherMaxHours(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .groupBy(Lesson::getTeacher, ConstraintCollectors.sum(lesson -> (int)lesson.getDurationInHours()))
                .filter((teacher, totalHours) -> totalHours > teacher.getMaxHours())
                .impact("Teacher max hours exceeded", HardSoftScore.ONE_HARD,
                        (teacher, totalHours) -> (int) (totalHours - teacher.getMaxHours()));
    }
    
    // A room must have sufficient capacity for the lesson.
    private Constraint roomCapacity(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getRoom().getCapacity() < lesson.getRequiredCapacity())
                .impact("Room capacity exceeded", HardSoftScore.ONE_HARD,
                        lesson -> lesson.getRequiredCapacity() - lesson.getRoom().getCapacity());
    }
    
    // A lab session must be in a lab room.
    private Constraint labInLabRoom(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> "lab".equals(lesson.getSessionType()) && !lesson.getRoom().isLab())
                .impact("Lab session in a classroom", HardSoftScore.ONE_HARD);
    }
    
    // A theory or tutorial session must be in a classroom, not a lab.
    private Constraint theoryInClassroom(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> !"lab".equals(lesson.getSessionType()) && lesson.getRoom().isLab())
                .impact("Theory or tutorial in a lab room", HardSoftScore.ONE_HARD);
    }
    
    // A lab session must be assigned to a lab-designated timeslot.
    private Constraint labInLabTimeslot(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> "lab".equals(lesson.getSessionType()) && !lesson.getTimeSlot().isLab())
                .impact("Lab session in a theory timeslot", HardSoftScore.ONE_HARD);
    }
    
    // A theory or tutorial must be in a theory-designated timeslot.
    private Constraint theoryInTheoryTimeslot(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> !"lab".equals(lesson.getSessionType()) && lesson.getTimeSlot().isLab())
                .impact("Theory or tutorial in a lab timeslot", HardSoftScore.ONE_HARD);
    }
    
    // Enforce a common lunch break for all groups.
    private Constraint lunchBreakConstraint(ConstraintFactory factory) {
        LocalTime lunchStart = LocalTime.of(11, 50);
        LocalTime lunchEnd = LocalTime.of(12, 40);
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeSlot().getStartTime().isBefore(lunchEnd) &&
                                  lunchStart.isBefore(lesson.getTimeSlot().getEndTime()))
                .impact("Class scheduled during lunch break", HardSoftScore.ONE_HARD);
    }

    // Enforce a common morning break.
    private Constraint morningBreakConstraint(ConstraintFactory factory) {
        LocalTime breakStart = LocalTime.of(9, 50);
        LocalTime breakEnd = LocalTime.of(10, 10);
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeSlot().getStartTime().isBefore(breakEnd) &&
                                  breakStart.isBefore(lesson.getTimeSlot().getEndTime()))
                .impact("Class scheduled during morning break", HardSoftScore.ONE_HARD);
    }

    // When two batches of the same lab occur at the same time, they must be in different rooms.
    private Constraint labBatchesInDifferentRooms(ConstraintFactory factory) {
        return factory.forEachUniquePair(Lesson.class,
                Joiners.equal(Lesson::getCourse),
                Joiners.equal(Lesson::getStudentGroup),
                Joiners.equal(Lesson::getTimeSlot),
                Joiners.equal(Lesson::getRoom)
        )
        // Ensure they are different batches (e.g., B1 and B2)
        .filter((lesson1, lesson2) -> lesson1.getLabBatch() != null && !lesson1.getLabBatch().equals(lesson2.getLabBatch()))
        .impact("Same room for different lab batches at the same time", HardSoftScore.ONE_HARD);
    }
    
    // Unsplit labs (like the 6 P.H. ones) are for the full class and require a large room.
    private Constraint unsplitLabRequiresLargeRoom(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
            .filter(lesson -> "lab".equals(lesson.getSessionType()) &&
                              lesson.getLabBatch() == null && // Unsplit
                              lesson.getCourse().getPracticalHours() == 6 &&
                              lesson.getRoom().getCapacity() < 70)
            .impact("Unsplit 6 P.H. lab needs room with capacity >= 70", HardSoftScore.ONE_HARD);
    }
    
    // --- Soft Constraints ---

    // Penalize gaps in a teacher's schedule on the same day.
    private Constraint teacherGaps(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .join(Lesson.class,
                        Joiners.equal(Lesson::getTeacher),
                        Joiners.equal(lesson -> lesson.getTimeSlot().getDay()))
                .filter((lesson1, lesson2) -> {
                    Duration gap = Duration.between(lesson1.getTimeSlot().getEndTime(), lesson2.getTimeSlot().getStartTime());
                    return !gap.isNegative() && !gap.isZero();
                })
                .impact("Teacher has a gap between classes", HardSoftScore.ONE_SOFT,
                        (Lesson lesson1, Lesson lesson2) -> (int) Duration.between(lesson1.getTimeSlot().getEndTime(), lesson2.getTimeSlot().getStartTime()).toMinutes());
    }

    // Penalize gaps in a student group's schedule on the same day.
    private Constraint studentGroupGaps(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .join(Lesson.class,
                        Joiners.equal(Lesson::getStudentGroup),
                        Joiners.equal(lesson -> lesson.getTimeSlot().getDay()))
                .filter((lesson1, lesson2) -> {
                    Duration gap = Duration.between(lesson1.getTimeSlot().getEndTime(), lesson2.getTimeSlot().getStartTime());
                    return !gap.isNegative() && !gap.isZero();
                })
                .impact("Student group has a gap between classes", HardSoftScore.ONE_SOFT,
                        (Lesson lesson1, Lesson lesson2) -> (int) Duration.between(lesson1.getTimeSlot().getEndTime(), lesson2.getTimeSlot().getStartTime()).toMinutes());
    }
}