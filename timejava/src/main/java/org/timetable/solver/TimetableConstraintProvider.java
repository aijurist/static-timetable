package org.timetable.solver;

import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.timetable.domain.Lesson;

public class TimetableConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
            // Hard constraints
            roomConflict(constraintFactory),
            teacherConflict(constraintFactory),
            studentGroupConflict(constraintFactory),
            roomCapacity(constraintFactory),
            teacherUnavailability(constraintFactory),
            roomUnavailability(constraintFactory),
            courseDependency(constraintFactory),
            
            // Soft constraints
            teacherRoomStability(constraintFactory),
            teacherTimeEfficiency(constraintFactory),
            studentGroupSubjectVariety(constraintFactory)
        };
    }

    // Hard constraints

    // A room can accommodate at most one lesson at the same time
    private Constraint roomConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .join(Lesson.class,
                        Lesson::overlapsTime,
                        Lesson::sameRoom)
                .penalize("Room conflict", HardSoftScore.ONE_HARD);
    }

    // A teacher can teach at most one lesson at the same time
    private Constraint teacherConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .join(Lesson.class,
                        Lesson::overlapsTime,
                        Lesson::sameTeacher)
                .penalize("Teacher conflict", HardSoftScore.ONE_HARD);
    }

    // A student group can attend at most one lesson at the same time
    private Constraint studentGroupConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .join(Lesson.class,
                        Lesson::overlapsTime,
                        Lesson::sameStudentGroup)
                .penalize("Student group conflict", HardSoftScore.ONE_HARD);
    }

    // A room's capacity should be sufficient for all its lessons
    private Constraint roomCapacity(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getRoom() != null 
                        && lesson.getStudentGroup() != null 
                        && lesson.getRoom().getCapacity() < lesson.getStudentGroup().getStudentCount())
                .penalize("Room capacity", HardSoftScore.ONE_HARD,
                        lesson -> lesson.getStudentGroup().getStudentCount() - lesson.getRoom().getCapacity());
    }

    // A teacher should not be scheduled when they are unavailable
    private Constraint teacherUnavailability(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTeacher() != null && lesson.getTimeSlot() != null
                        && !lesson.getTeacher().isAvailable(lesson.getTimeSlot()))
                .penalize("Teacher unavailable", HardSoftScore.ONE_HARD);
    }

    // A room should not be scheduled when it is unavailable
    private Constraint roomUnavailability(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getRoom() != null && lesson.getTimeSlot() != null
                        && !lesson.getRoom().isAvailable(lesson.getTimeSlot()))
                .penalize("Room unavailable", HardSoftScore.ONE_HARD);
    }

    // Courses with dependencies must be scheduled in the correct order
    private Constraint courseDependency(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getCourse() != null && lesson.getCourse().getPrerequisite() != null)
                .join(Lesson.class,
                        lesson -> lesson.getCourse().getPrerequisite(),
                        (lesson, prerequisiteLesson) -> lesson.getTimeSlot().getDay() <= prerequisiteLesson.getTimeSlot().getDay())
                .penalize("Course dependency", HardSoftScore.ONE_HARD);
    }

    // Soft constraints

    // A teacher prefers to teach in a single room
    private Constraint teacherRoomStability(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .join(Lesson.class,
                        Lesson::sameTeacher,
                        Lesson::differentRoom)
                .penalize("Teacher room stability", HardSoftScore.ONE_SOFT);
    }

    // A teacher prefers to teach sequential lessons and dislikes gaps between lessons
    private Constraint teacherTimeEfficiency(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .join(Lesson.class,
                        Lesson::sameTeacher,
                        Lesson::sameDay,
                        (lesson1, lesson2) -> {
                            int between = Math.abs(lesson1.getTimeSlot().getStartTime() - lesson2.getTimeSlot().getStartTime());
                            return between > 1 && between < 4; // Penalize gaps of 1-3 periods
                        })
                .penalize("Teacher time efficiency", HardSoftScore.ONE_SOFT);
    }

    // Student groups should not have the same subject multiple times in a day
    private Constraint studentGroupSubjectVariety(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .join(Lesson.class,
                        Lesson::sameStudentGroup,
                        Lesson::sameDay,
                        Lesson::sameCourse)
                .penalize("Student group subject variety", HardSoftScore.ONE_SOFT);
    }
} 