package org.timetable.solver;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.timetable.domain.Lesson;

import java.time.DayOfWeek;
import java.util.function.Function;

public class TimetableConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
            // Hard constraints
            roomConflict(constraintFactory),
            teacherConflict(constraintFactory),
            studentGroupConflict(constraintFactory),
            roomCapacity(constraintFactory),
            labSessionInLabRoom(constraintFactory),
            theorySessionInTheoryRoom(constraintFactory),
            tutorialSessionInTheoryRoom(constraintFactory),
            
            // Soft constraints
            teacherRoomStability(constraintFactory),
            theorySessionsOnDifferentDays(constraintFactory),
            avoidLateClasses(constraintFactory)
        };
    }

    private Constraint roomConflict(ConstraintFactory constraintFactory) {
        // A room can accommodate at most one lesson at the same time
        return constraintFactory
            .forEachUniquePair(Lesson.class)
            .filter((lesson1, lesson2) -> 
                lesson1.getTimeSlot() != null && lesson2.getTimeSlot() != null &&
                lesson1.getRoom() != null && lesson2.getRoom() != null &&
                lesson1.getTimeSlot().equals(lesson2.getTimeSlot()) &&
                lesson1.getRoom().equals(lesson2.getRoom()))
            .penalize(HardSoftScore.ONE_HARD)
            .asConstraint("Room conflict");
    }

    private Constraint teacherConflict(ConstraintFactory constraintFactory) {
        // A teacher can teach at most one lesson at the same time
        return constraintFactory
            .forEachUniquePair(Lesson.class)
            .filter((lesson1, lesson2) -> 
                lesson1.getTeacher() != null && lesson2.getTeacher() != null &&
                lesson1.getTimeSlot() != null && lesson2.getTimeSlot() != null &&
                lesson1.getTeacher().equals(lesson2.getTeacher()) &&
                lesson1.getTimeSlot().equals(lesson2.getTimeSlot()))
            .penalize(HardSoftScore.ONE_HARD)
            .asConstraint("Teacher conflict");
    }

    private Constraint studentGroupConflict(ConstraintFactory constraintFactory) {
        // A student group can attend at most one lesson at the same time
        return constraintFactory
            .forEachUniquePair(Lesson.class)
            .filter((lesson1, lesson2) -> 
                lesson1.getStudentGroup() != null && lesson2.getStudentGroup() != null &&
                lesson1.getTimeSlot() != null && lesson2.getTimeSlot() != null &&
                lesson1.getStudentGroup().equals(lesson2.getStudentGroup()) &&
                lesson1.getTimeSlot().equals(lesson2.getTimeSlot()))
            .penalize(HardSoftScore.ONE_HARD)
            .asConstraint("Student group conflict");
    }

    private Constraint roomCapacity(ConstraintFactory constraintFactory) {
        // A room's capacity should be sufficient for all lessons taught in it
        return constraintFactory
            .forEach(Lesson.class)
            .filter(lesson -> lesson.getRoom() != null && lesson.getStudentGroup() != null 
                && lesson.getRoom().getCapacity() < lesson.getStudentGroup().getSize())
            .penalize(HardSoftScore.ONE_HARD)
            .asConstraint("Room capacity");
    }

    private Constraint labSessionInLabRoom(ConstraintFactory constraintFactory) {
        // Lab sessions should be held in lab rooms
        return constraintFactory
            .forEach(Lesson.class)
            .filter(lesson -> "lab".equals(lesson.getSessionType()) && lesson.getRoom() != null && !lesson.getRoom().isLab())
            .penalize(HardSoftScore.ONE_HARD)
            .asConstraint("Lab session in lab room");
    }

    private Constraint theorySessionInTheoryRoom(ConstraintFactory constraintFactory) {
        // Theory sessions should be held in theory rooms
        return constraintFactory
            .forEach(Lesson.class)
            .filter(lesson -> "lecture".equals(lesson.getSessionType()) && lesson.getRoom() != null && lesson.getRoom().isLab())
            .penalize(HardSoftScore.ONE_HARD)
            .asConstraint("Theory session in theory room");
    }
    
    private Constraint tutorialSessionInTheoryRoom(ConstraintFactory constraintFactory) {
        // Tutorial sessions should be held in theory rooms
        return constraintFactory
            .forEach(Lesson.class)
            .filter(lesson -> "tutorial".equals(lesson.getSessionType()) && lesson.getRoom() != null && lesson.getRoom().isLab())
            .penalize(HardSoftScore.ONE_HARD)
            .asConstraint("Tutorial session in theory room");
    }

    private Constraint teacherRoomStability(ConstraintFactory constraintFactory) {
        // A teacher should teach in the same room on the same day
        return constraintFactory
            .forEachUniquePair(Lesson.class)
            .filter((lesson1, lesson2) -> 
                lesson1.getTeacher() != null && lesson2.getTeacher() != null &&
                lesson1.getTeacher().equals(lesson2.getTeacher()) &&
                lesson1.getTimeSlot() != null && lesson2.getTimeSlot() != null &&
                lesson1.getTimeSlot().getDay().equals(lesson2.getTimeSlot().getDay()) &&
                lesson1.getRoom() != null && lesson2.getRoom() != null &&
                !lesson1.getRoom().equals(lesson2.getRoom()))
            .penalize(HardSoftScore.ONE_SOFT)
            .asConstraint("Teacher room stability");
    }

    private Constraint theorySessionsOnDifferentDays(ConstraintFactory constraintFactory) {
        // Theory sessions of the same course for the same student group should be on different days
        return constraintFactory
            .forEachUniquePair(Lesson.class)
            .filter((lesson1, lesson2) -> 
                lesson1.getCourse() != null && lesson2.getCourse() != null &&
                lesson1.getCourse().equals(lesson2.getCourse()) &&
                lesson1.getStudentGroup() != null && lesson2.getStudentGroup() != null &&
                lesson1.getStudentGroup().equals(lesson2.getStudentGroup()) &&
                "lecture".equals(lesson1.getSessionType()) &&
                "lecture".equals(lesson2.getSessionType()) &&
                lesson1.getTimeSlot() != null && lesson2.getTimeSlot() != null &&
                lesson1.getTimeSlot().getDay().equals(lesson2.getTimeSlot().getDay()))
            .penalize(HardSoftScore.ONE_SOFT)
            .asConstraint("Theory sessions on different days");
    }

    private Constraint avoidLateClasses(ConstraintFactory constraintFactory) {
        // Avoid scheduling classes late in the day
        return constraintFactory
            .forEach(Lesson.class)
            .filter(lesson -> lesson.getTimeSlot() != null 
                && lesson.getTimeSlot().getStartTime().getHour() >= 15) // After 3 PM
            .penalize(HardSoftScore.ONE_SOFT)
            .asConstraint("Avoid late classes");
    }
} 