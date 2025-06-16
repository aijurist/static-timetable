package org.timetable.validation;

import java.time.DayOfWeek;
import java.util.Map;

/**
 * Analytics data for a teacher
 */
public class TeacherAnalytics {
    
    private int totalLessons;
    private int totalHours;
    private int differentCourses;
    private Map<DayOfWeek, Integer> dailyLoad;
    
    public int getTotalLessons() { return totalLessons; }
    public void setTotalLessons(int totalLessons) { this.totalLessons = totalLessons; }
    
    public int getTotalHours() { return totalHours; }
    public void setTotalHours(int totalHours) { this.totalHours = totalHours; }
    
    public int getDifferentCourses() { return differentCourses; }
    public void setDifferentCourses(int differentCourses) { this.differentCourses = differentCourses; }
    
    public Map<DayOfWeek, Integer> getDailyLoad() { return dailyLoad; }
    public void setDailyLoad(Map<DayOfWeek, Integer> dailyLoad) { this.dailyLoad = dailyLoad; }
    
    @Override
    public String toString() {
        return String.format("Lessons: %d, Hours: %d, Courses: %d", 
            totalLessons, totalHours, differentCourses);
    }
} 