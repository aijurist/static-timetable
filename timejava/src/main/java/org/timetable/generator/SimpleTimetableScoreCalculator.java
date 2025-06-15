package org.timetable.generator;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.calculator.EasyScoreCalculator;
import org.timetable.domain.*;

import java.util.*;

public class SimpleTimetableScoreCalculator implements EasyScoreCalculator<TimetableProblem, HardSoftScore> {

    @Override
    public HardSoftScore calculateScore(TimetableProblem timetableProblem) {
        int hardScore = 0;
        int softScore = 0;
        
        List<Lesson> lessons = timetableProblem.getLessons();
        
        // Hard constraints (must be satisfied)
        
        // 1. Room conflicts: same room, same time slot
        for (int i = 0; i < lessons.size(); i++) {
            Lesson lesson1 = lessons.get(i);
            if (lesson1.getRoom() == null || lesson1.getTimeSlot() == null) continue;
            
            for (int j = i + 1; j < lessons.size(); j++) {
                Lesson lesson2 = lessons.get(j);
                if (lesson2.getRoom() == null || lesson2.getTimeSlot() == null) continue;
                
                if (lesson1.getRoom().equals(lesson2.getRoom()) && 
                    lesson1.getTimeSlot().equals(lesson2.getTimeSlot())) {
                    hardScore -= 10; // Reduced penalty to allow some flexibility
                }
            }
        }
        
        // 2. Teacher conflicts: same teacher, same time slot
        for (int i = 0; i < lessons.size(); i++) {
            Lesson lesson1 = lessons.get(i);
            if (lesson1.getTeacher() == null || lesson1.getTimeSlot() == null) continue;
            
            for (int j = i + 1; j < lessons.size(); j++) {
                Lesson lesson2 = lessons.get(j);
                if (lesson2.getTeacher() == null || lesson2.getTimeSlot() == null) continue;
                
                if (lesson1.getTeacher().equals(lesson2.getTeacher()) && 
                    lesson1.getTimeSlot().equals(lesson2.getTimeSlot())) {
                    hardScore -= 10; // Reduced penalty
                }
            }
        }
        
        // 3. Student group conflicts: same student group, same time slot
        for (int i = 0; i < lessons.size(); i++) {
            Lesson lesson1 = lessons.get(i);
            if (lesson1.getStudentGroup() == null || lesson1.getTimeSlot() == null) continue;
            
            for (int j = i + 1; j < lessons.size(); j++) {
                Lesson lesson2 = lessons.get(j);
                if (lesson2.getStudentGroup() == null || lesson2.getTimeSlot() == null) continue;
                
                if (lesson1.getStudentGroup().equals(lesson2.getStudentGroup()) && 
                    lesson1.getTimeSlot().equals(lesson2.getTimeSlot())) {
                    hardScore -= 10; // Reduced penalty
                }
            }
        }
        
        // Soft constraints (preferences)
        
        // 1. Encourage assignment of all lessons (heavily penalize unassigned lessons)
        for (Lesson lesson : lessons) {
            if (lesson.getRoom() == null || lesson.getTimeSlot() == null) {
                softScore -= 100; // Heavy penalty for unassigned lessons
            }
        }
        
        // 2. Day distribution: encourage lessons to be spread across all days
        Map<StudentGroup, Map<Integer, Integer>> studentGroupDayCount = new HashMap<>();
        for (Lesson lesson : lessons) {
            if (lesson.getStudentGroup() != null && lesson.getTimeSlot() != null) {
                studentGroupDayCount
                    .computeIfAbsent(lesson.getStudentGroup(), k -> new HashMap<>())
                    .merge(lesson.getTimeSlot().getDay(), 1, Integer::sum);
            }
        }
        
        // Encourage distribution across days but don't make it too strict
        for (Map<Integer, Integer> dayCount : studentGroupDayCount.values()) {
            if (!dayCount.isEmpty()) {
                int maxLessonsPerDay = Collections.max(dayCount.values());
                int minLessonsPerDay = Collections.min(dayCount.values());
                int daySpread = dayCount.size();
                
                // Reward spreading across more days
                softScore += daySpread * 2;
                
                // Small penalty for uneven distribution
                softScore -= (maxLessonsPerDay - minLessonsPerDay);
            }
        }
        
        // 3. Room type preference (soft constraint)
        for (Lesson lesson : lessons) {
            if (lesson.getRoom() != null && lesson.getCourse() != null) {
                String courseType = lesson.getCourse().getCourseType().toLowerCase();
                boolean roomIsLab = lesson.getRoom().isLab();
                
                // Prefer matching room types but don't make it mandatory
                if ((courseType.equals("lab") && !roomIsLab) ||
                    (courseType.equals("theory") && roomIsLab)) {
                    softScore -= 2; // Small penalty for mismatched room types
                }
            }
        }
        
        // 4. Teacher workload balance
        Map<Teacher, Integer> teacherWorkload = new HashMap<>();
        for (Lesson lesson : lessons) {
            if (lesson.getTeacher() != null && lesson.getTimeSlot() != null) {
                teacherWorkload.merge(lesson.getTeacher(), 1, Integer::sum);
            }
        }
        
        // Encourage balanced workload
        if (!teacherWorkload.isEmpty()) {
            int maxWorkload = Collections.max(teacherWorkload.values());
            int minWorkload = Collections.min(teacherWorkload.values());
            softScore -= (maxWorkload - minWorkload) / 2; // Small penalty for imbalance
        }
        
        return HardSoftScore.of(hardScore, softScore);
    }
} 