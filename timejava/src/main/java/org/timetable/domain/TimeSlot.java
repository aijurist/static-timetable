package org.timetable.domain;

import org.optaplanner.core.api.domain.lookup.PlanningId;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Represents a time slot in the timetable system.
 */
public class TimeSlot {
    @PlanningId
    private int id;
    private int day; // 0 = Monday, 1 = Tuesday, etc.
    private int startTime; // 8 = 8:00 AM, 13 = 1:00 PM, etc.
    private int endTime;
    private String startTimeStr; // "8:00", "13:00", etc.
    private String endTimeStr; // "8:50", "13:50", etc.
    private boolean isLabTimeSlot;

    public TimeSlot() {
    }

    public TimeSlot(int id, int day, int startTime, int endTime) {
        this.id = id;
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
        this.startTimeStr = formatTime(startTime);
        this.endTimeStr = formatTime(endTime);
        this.isLabTimeSlot = false;
    }

    public TimeSlot(int id, String dayStr, String startTimeStr, String endTimeStr) {
        this.id = id;
        this.day = parseDayOfWeek(dayStr);
        this.startTime = parseTime(startTimeStr);
        this.endTime = parseTime(endTimeStr);
        this.startTimeStr = startTimeStr;
        this.endTimeStr = endTimeStr;
        this.isLabTimeSlot = false;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public int getStartTime() {
        return startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
        this.startTimeStr = formatTime(startTime);
    }

    public int getEndTime() {
        return endTime;
    }

    public void setEndTime(int endTime) {
        this.endTime = endTime;
        this.endTimeStr = formatTime(endTime);
    }

    public String getStartTimeStr() {
        return startTimeStr;
    }

    public void setStartTimeStr(String startTimeStr) {
        this.startTimeStr = startTimeStr;
        this.startTime = parseTime(startTimeStr);
    }

    public String getEndTimeStr() {
        return endTimeStr;
    }

    public void setEndTimeStr(String endTimeStr) {
        this.endTimeStr = endTimeStr;
        this.endTime = parseTime(endTimeStr);
    }

    public boolean isLabTimeSlot() {
        return isLabTimeSlot;
    }

    public void setLabTimeSlot(boolean labTimeSlot) {
        isLabTimeSlot = labTimeSlot;
    }

    /**
     * Get a string representation of the time slot.
     */
    public String getTimeString() {
        return startTimeStr + "-" + endTimeStr;
    }

    /**
     * Parse a day of week string into an integer.
     */
    private int parseDayOfWeek(String day) {
        switch (day.toLowerCase()) {
            case "monday": return 0;
            case "tuesday": return 1;
            case "wednesday": return 2;
            case "thursday": return 3;
            case "friday": return 4;
            case "saturday": return 5;
            case "sunday": return 6;
            default: throw new IllegalArgumentException("Invalid day: " + day);
        }
    }

    /**
     * Parse a time string into an integer.
     */
    private int parseTime(String time) {
        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]);
    }

    /**
     * Format an integer time into a string.
     */
    private String formatTime(int time) {
        return time + ":00";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeSlot timeSlot = (TimeSlot) o;
        return id == timeSlot.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return getDayString() + " " + startTimeStr + "-" + endTimeStr;
    }

    /**
     * Get a string representation of the day.
     */
    public String getDayString() {
        switch (day) {
            case 0: return "Monday";
            case 1: return "Tuesday";
            case 2: return "Wednesday";
            case 3: return "Thursday";
            case 4: return "Friday";
            case 5: return "Saturday";
            case 6: return "Sunday";
            default: return "Unknown";
        }
    }
} 