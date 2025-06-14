package org.timetable.solver;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.calculator.EasyScoreCalculator;
import org.timetable.domain.Lesson;
import org.timetable.domain.TimetableProblem;

import java.util.List;

public class TimetableConstraintProvider implements EasyScoreCalculator<TimetableProblem, HardSoftScore> {

    @Override
    public HardSoftScore calculateScore(TimetableProblem solution) {
        int hardScore = 0;
        int softScore = 0;
        
        List<Lesson> lessons = solution.getLessons();
        
        // Apply hard constraints
        hardScore += calculateRoomConflict(lessons);
        hardScore += calculateTeacherConflict(lessons);
        hardScore += calculateStudentGroupConflict(lessons);
        hardScore += calculateRoomCapacity(lessons);
        hardScore += calculateLabSessionInLabRoom(lessons);
        hardScore += calculateTheorySessionInTheoryRoom(lessons);
        hardScore += calculateTutorialSessionInTheoryRoom(lessons);
        
        // Apply soft constraints
        softScore += calculateTeacherRoomStability(lessons);
        softScore += calculateTheorySessionsOnDifferentDays(lessons);
        softScore += calculateAvoidLateClasses(lessons);
        
        return HardSoftScore.of(hardScore, softScore);
    }

    private int calculateRoomConflict(List<Lesson> lessons) {
        int score = 0;
        
        // Check each pair of lessons for room conflicts
        for (int i = 0; i < lessons.size(); i++) {
            Lesson lesson1 = lessons.get(i);
            if (lesson1.getTimeSlot() == null || lesson1.getRoom() == null) {
                continue;
            }
            
            for (int j = i + 1; j < lessons.size(); j++) {
                Lesson lesson2 = lessons.get(j);
                if (lesson2.getTimeSlot() == null || lesson2.getRoom() == null) {
                    continue;
                }
                
                if (lesson1.getTimeSlot().equals(lesson2.getTimeSlot()) &&
                    lesson1.getRoom().equals(lesson2.getRoom())) {
                    score -= 1; // Penalize for room conflict
                }
            }
        }
        
        return score;
    }

    private int calculateTeacherConflict(List<Lesson> lessons) {
        int score = 0;
        
        // Check each pair of lessons for teacher conflicts
        for (int i = 0; i < lessons.size(); i++) {
            Lesson lesson1 = lessons.get(i);
            if (lesson1.getTimeSlot() == null || lesson1.getTeacher() == null) {
                continue;
            }
            
            for (int j = i + 1; j < lessons.size(); j++) {
                Lesson lesson2 = lessons.get(j);
                if (lesson2.getTimeSlot() == null || lesson2.getTeacher() == null) {
                    continue;
                }
                
                if (lesson1.getTimeSlot().equals(lesson2.getTimeSlot()) &&
                    lesson1.getTeacher().equals(lesson2.getTeacher())) {
                    score -= 1; // Penalize for teacher conflict
                }
            }
        }
        
        return score;
    }

    private int calculateStudentGroupConflict(List<Lesson> lessons) {
        int score = 0;
        
        // Check each pair of lessons for student group conflicts
        for (int i = 0; i < lessons.size(); i++) {
            Lesson lesson1 = lessons.get(i);
            if (lesson1.getTimeSlot() == null || lesson1.getStudentGroup() == null) {
                continue;
            }
            
            for (int j = i + 1; j < lessons.size(); j++) {
                Lesson lesson2 = lessons.get(j);
                if (lesson2.getTimeSlot() == null || lesson2.getStudentGroup() == null) {
                    continue;
                }
                
                if (lesson1.getTimeSlot().equals(lesson2.getTimeSlot()) &&
                    lesson1.getStudentGroup().equals(lesson2.getStudentGroup())) {
                    score -= 1; // Penalize for student group conflict
                }
            }
        }
        
        return score;
    }

    private int calculateRoomCapacity(List<Lesson> lessons) {
        int score = 0;
        
        for (Lesson lesson : lessons) {
            if (lesson.getRoom() != null && lesson.getStudentGroup() != null) {
                if (lesson.getRoom().getCapacity() < lesson.getStudentGroup().getSize()) {
                    score -= 1; // Penalize for insufficient room capacity
                }
            }
        }
        
        return score;
    }

    private int calculateLabSessionInLabRoom(List<Lesson> lessons) {
        int score = 0;
        
        for (Lesson lesson : lessons) {
            if ("lab".equals(lesson.getSessionType()) && lesson.getRoom() != null && !lesson.getRoom().isLab()) {
                score -= 1; // Penalize for lab session not in lab room
            }
        }
        
        return score;
    }

    private int calculateTheorySessionInTheoryRoom(List<Lesson> lessons) {
        int score = 0;
        
        for (Lesson lesson : lessons) {
            if ("lecture".equals(lesson.getSessionType()) && lesson.getRoom() != null && lesson.getRoom().isLab()) {
                score -= 1; // Penalize for theory session in lab room
            }
        }
        
        return score;
    }
    
    private int calculateTutorialSessionInTheoryRoom(List<Lesson> lessons) {
        int score = 0;
        
        for (Lesson lesson : lessons) {
            if ("tutorial".equals(lesson.getSessionType()) && lesson.getRoom() != null && lesson.getRoom().isLab()) {
                score -= 1; // Penalize for tutorial session in lab room
            }
        }
        
        return score;
    }

    private int calculateTeacherRoomStability(List<Lesson> lessons) {
        int score = 0;
        
        for (int i = 0; i < lessons.size(); i++) {
            Lesson lesson1 = lessons.get(i);
            if (lesson1.getTimeSlot() == null || lesson1.getRoom() == null || lesson1.getTeacher() == null) {
                continue;
            }
            
            for (int j = i + 1; j < lessons.size(); j++) {
                Lesson lesson2 = lessons.get(j);
                if (lesson2.getTimeSlot() == null || lesson2.getRoom() == null || lesson2.getTeacher() == null) {
                    continue;
                }
                
                if (lesson1.getTeacher().equals(lesson2.getTeacher()) &&
                    lesson1.getTimeSlot().getDay().equals(lesson2.getTimeSlot().getDay()) &&
                    !lesson1.getRoom().equals(lesson2.getRoom())) {
                    score -= 1; // Penalize for teacher room instability
                }
            }
        }
        
        return score;
    }

    private int calculateTheorySessionsOnDifferentDays(List<Lesson> lessons) {
        int score = 0;
        
        for (int i = 0; i < lessons.size(); i++) {
            Lesson lesson1 = lessons.get(i);
            if (lesson1.getTimeSlot() == null || lesson1.getCourse() == null || lesson1.getStudentGroup() == null || 
                !"lecture".equals(lesson1.getSessionType())) {
                continue;
            }
            
            for (int j = i + 1; j < lessons.size(); j++) {
                Lesson lesson2 = lessons.get(j);
                if (lesson2.getTimeSlot() == null || lesson2.getCourse() == null || lesson2.getStudentGroup() == null || 
                    !"lecture".equals(lesson2.getSessionType())) {
                    continue;
                }
                
                if (lesson1.getCourse().equals(lesson2.getCourse()) &&
                    lesson1.getStudentGroup().equals(lesson2.getStudentGroup()) &&
                    lesson1.getTimeSlot().getDay().equals(lesson2.getTimeSlot().getDay())) {
                    score -= 1; // Penalize for theory sessions on same day
                }
            }
        }
        
        return score;
    }

    private int calculateAvoidLateClasses(List<Lesson> lessons) {
        int score = 0;
        
        for (Lesson lesson : lessons) {
            if (lesson.getTimeSlot() != null && lesson.getTimeSlot().getStartTime().getHour() >= 15) {
                score -= 1; // Penalize for late classes
            }
        }
        
        return score;
    }
} 