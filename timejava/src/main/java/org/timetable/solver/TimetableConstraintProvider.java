package org.timetable.solver;

import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Joiners;
import org.timetable.domain.Lesson;

public class TimetableConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
            // Hard constraints
            roomConflict(constraintFactory),
            teacherConflict(constraintFactory),
            studentGroupConflict(constraintFactory),
            roomCapacity(constraintFactory)
        };
    }

    // Hard constraints

    // A room can accommodate at most one lesson at the same time
    private Constraint roomConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.from(Lesson.class)
                .join(Lesson.class,
                        Joiners.equal(Lesson::getTimeSlot),
                        Joiners.equal(Lesson::getRoom),
                        Joiners.lessThan(Lesson::getId))
                .penalize("Room conflict", HardSoftScore.ONE_HARD);
    }

    // A teacher can teach at most one lesson at the same time
    private Constraint teacherConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.from(Lesson.class)
                .join(Lesson.class,
                        Joiners.equal(Lesson::getTimeSlot),
                        Joiners.equal(Lesson::getTeacher),
                        Joiners.lessThan(Lesson::getId))
                .penalize("Teacher conflict", HardSoftScore.ONE_HARD);
    }

    // A student group can attend at most one lesson at the same time
    private Constraint studentGroupConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.from(Lesson.class)
                .join(Lesson.class,
                        Joiners.equal(Lesson::getTimeSlot),
                        Joiners.equal(Lesson::getStudentGroup),
                        Joiners.lessThan(Lesson::getId))
                .penalize("Student group conflict", HardSoftScore.ONE_HARD);
    }

    // A room's capacity should be sufficient for all its lessons
    private Constraint roomCapacity(ConstraintFactory constraintFactory) {
        return constraintFactory.from(Lesson.class)
                .filter(lesson -> lesson.getRoom() != null 
                        && lesson.getStudentGroup() != null 
                        && lesson.getRoom().getCapacity() < lesson.getStudentGroup().getSize())
                .penalize("Room capacity", HardSoftScore.ONE_HARD,
                        lesson -> lesson.getStudentGroup().getSize() - lesson.getRoom().getCapacity());
    }
} 