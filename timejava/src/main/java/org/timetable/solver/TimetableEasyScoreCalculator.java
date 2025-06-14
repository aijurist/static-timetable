package org.timetable.solver;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.calculator.EasyScoreCalculator;
import org.timetable.domain.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calculates the score for a timetable solution.
 */
public class TimetableEasyScoreCalculator implements EasyScoreCalculator<TimetableProblem, HardSoftScore> {

    @Override
    public HardSoftScore calculateScore(TimetableProblem solution) {
        int hardScore = 0;
        int softScore = 0;
        
        List<Lesson> lessons = solution.getLessons();
        
        // Hard constraints
        hardScore += calculateRoomConflicts(lessons);
        hardScore += calculateTeacherConflicts(lessons);
        hardScore += calculateStudentGroupConflicts(lessons);
        hardScore += calculateRoomCapacityConflicts(lessons);
        hardScore += calculateRoomTypeConflicts(lessons);
        
        // Soft constraints
        softScore += calculateTeacherTimePreferences(lessons);
        softScore += calculateMinGapsBetweenLessons(lessons);
        softScore += calculateRoomStability(lessons);
        
        return HardSoftScore.of(hardScore, softScore);
    }
    
    /**
     * No two lessons can be in the same room at the same time.
     */
    private int calculateRoomConflicts(List<Lesson> lessons) {
        int score = 0;
        for (int i = 0; i < lessons.size(); i++) {
            Lesson lesson1 = lessons.get(i);
            if (lesson1.getRoom() == null || lesson1.getTimeSlot() == null) {
                score -= 10; // Penalize unassigned lessons
                continue;
            }
            
            for (int j = i + 1; j < lessons.size(); j++) {
                Lesson lesson2 = lessons.get(j);
                if (lesson2.getRoom() == null || lesson2.getTimeSlot() == null) {
                    continue;
                }
                
                if (lesson1.getRoom().equals(lesson2.getRoom()) && 
                    lesson1.getTimeSlot().equals(lesson2.getTimeSlot())) {
                    score -= 100; // Room conflict
                }
            }
        }
        return score;
    }
    
    /**
     * No teacher can teach two lessons at the same time.
     */
    private int calculateTeacherConflicts(List<Lesson> lessons) {
        int score = 0;
        for (int i = 0; i < lessons.size(); i++) {
            Lesson lesson1 = lessons.get(i);
            if (lesson1.getTeacher() == null || lesson1.getTimeSlot() == null) {
                continue;
            }
            
            for (int j = i + 1; j < lessons.size(); j++) {
                Lesson lesson2 = lessons.get(j);
                if (lesson2.getTeacher() == null || lesson2.getTimeSlot() == null) {
                    continue;
                }
                
                if (lesson1.getTeacher().equals(lesson2.getTeacher()) && 
                    lesson1.getTimeSlot().equals(lesson2.getTimeSlot())) {
                    score -= 100; // Teacher conflict
                }
            }
        }
        return score;
    }
    
    /**
     * No student group can attend two lessons at the same time.
     */
    private int calculateStudentGroupConflicts(List<Lesson> lessons) {
        int score = 0;
        for (int i = 0; i < lessons.size(); i++) {
            Lesson lesson1 = lessons.get(i);
            if (lesson1.getStudentGroup() == null || lesson1.getTimeSlot() == null) {
                continue;
            }
            
            for (int j = i + 1; j < lessons.size(); j++) {
                Lesson lesson2 = lessons.get(j);
                if (lesson2.getStudentGroup() == null || lesson2.getTimeSlot() == null) {
                    continue;
                }
                
                if (lesson1.getStudentGroup().equals(lesson2.getStudentGroup()) && 
                    lesson1.getTimeSlot().equals(lesson2.getTimeSlot())) {
                    score -= 100; // Student group conflict
                }
            }
        }
        return score;
    }
    
    /**
     * Room capacity should be sufficient for the student group.
     */
    private int calculateRoomCapacityConflicts(List<Lesson> lessons) {
        int score = 0;
        for (Lesson lesson : lessons) {
            if (lesson.getRoom() != null && lesson.getStudentGroup() != null && 
                lesson.getStudentGroup().getSize() > lesson.getRoom().getCapacity()) {
                score -= (lesson.getStudentGroup().getSize() - lesson.getRoom().getCapacity());
            }
        }
        return score;
    }
    
    /**
     * Lab lessons should be in lab rooms.
     */
    private int calculateRoomTypeConflicts(List<Lesson> lessons) {
        int score = 0;
        for (Lesson lesson : lessons) {
            if (lesson.getRoom() != null && "lab".equals(lesson.getLessonType()) && !lesson.getRoom().isLab()) {
                score -= 50; // Lab lesson in non-lab room
            }
        }
        return score;
    }
    
    /**
     * Teachers prefer to teach at certain times.
     */
    private int calculateTeacherTimePreferences(List<Lesson> lessons) {
        int score = 0;
        for (Lesson lesson : lessons) {
            if (lesson.getTeacher() != null && lesson.getTimeSlot() != null) {
                if (!lesson.getTeacher().isAvailable(lesson.getTimeSlot())) {
                    score -= 10; // Teacher unavailable at this time
                }
            }
        }
        return score;
    }
    
    /**
     * There should be a minimum gap between lessons for the same student group.
     */
    private int calculateMinGapsBetweenLessons(List<Lesson> lessons) {
        int score = 0;
        
        // Group lessons by student group and day
        Map<StudentGroup, Map<Integer, Map<Integer, Lesson>>> lessonsByGroupAndDay = new HashMap<>();
        
        for (Lesson lesson : lessons) {
            if (lesson.getStudentGroup() == null || lesson.getTimeSlot() == null) {
                continue;
            }
            
            StudentGroup group = lesson.getStudentGroup();
            int day = lesson.getTimeSlot().getDay();
            int startTime = lesson.getTimeSlot().getStartTime();
            
            lessonsByGroupAndDay
                .computeIfAbsent(group, k -> new HashMap<>())
                .computeIfAbsent(day, k -> new HashMap<>())
                .put(startTime, lesson);
        }
        
        // Check for consecutive lessons without breaks
        for (Map<Integer, Map<Integer, Lesson>> lessonsByDay : lessonsByGroupAndDay.values()) {
            for (Map<Integer, Lesson> lessonsByTime : lessonsByDay.values()) {
                // Sort by start time
                Integer[] startTimes = lessonsByTime.keySet().toArray(new Integer[0]);
                java.util.Arrays.sort(startTimes);
                
                for (int i = 0; i < startTimes.length - 1; i++) {
                    Lesson lesson1 = lessonsByTime.get(startTimes[i]);
                    Lesson lesson2 = lessonsByTime.get(startTimes[i + 1]);
                    
                    int endTime1 = lesson1.getTimeSlot().getStartTime() + lesson1.getDuration();
                    int startTime2 = lesson2.getTimeSlot().getStartTime();
                    
                    if (startTime2 - endTime1 < 1) {
                        score -= 5; // No break between lessons
                    }
                }
            }
        }
        
        return score;
    }
    
    /**
     * Lessons of the same course should be in the same room.
     */
    private int calculateRoomStability(List<Lesson> lessons) {
        int score = 0;
        
        // Group lessons by course
        Map<Course, Map<Room, Integer>> roomsPerCourse = new HashMap<>();
        
        for (Lesson lesson : lessons) {
            if (lesson.getCourse() == null || lesson.getRoom() == null) {
                continue;
            }
            
            Course course = lesson.getCourse();
            Room room = lesson.getRoom();
            
            roomsPerCourse
                .computeIfAbsent(course, k -> new HashMap<>())
                .merge(room, 1, Integer::sum);
        }
        
        // Reward courses that use fewer rooms
        for (Map<Room, Integer> rooms : roomsPerCourse.values()) {
            if (rooms.size() > 1) {
                score -= (rooms.size() - 1) * 2; // Penalize using multiple rooms
            }
        }
        
        return score;
    }
} 