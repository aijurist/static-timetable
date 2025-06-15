package org.timetable.validation;

import java.time.DayOfWeek;
import java.util.Map;

/**
 * Analytics data for a student group
 */
public class StudentGroupAnalytics {
    
    private int totalLessons;
    private int differentCourses;
    private Map<DayOfWeek, Integer> dailyLoad;
    
    public int getTotalLessons() { return totalLessons; }
    public void setTotalLessons(int totalLessons) { this.totalLessons = totalLessons; }
    
    public int getDifferentCourses() { return differentCourses; }
    public void setDifferentCourses(int differentCourses) { this.differentCourses = differentCourses; }
    
    public Map<DayOfWeek, Integer> getDailyLoad() { return dailyLoad; }
    public void setDailyLoad(Map<DayOfWeek, Integer> dailyLoad) { this.dailyLoad = dailyLoad; }
    
    @Override
    public String toString() {
        return String.format("Lessons: %d, Courses: %d", totalLessons, differentCourses);
    }
} 