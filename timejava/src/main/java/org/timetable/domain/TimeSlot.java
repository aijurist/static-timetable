package org.timetable.domain;

import org.optaplanner.core.api.domain.lookup.PlanningId;

import java.time.DayOfWeek;
import java.time.LocalTime;

public class TimeSlot {
    @PlanningId
    private String id;
    private DayOfWeek day;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean isLab;

    public TimeSlot() {
    }

    public TimeSlot(String id, DayOfWeek day, LocalTime startTime, LocalTime endTime, boolean isLab) {
        this.id = id;
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isLab = isLab;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public DayOfWeek getDay() {
        return day;
    }

    public void setDay(DayOfWeek day) {
        this.day = day;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public boolean isLab() {
        return isLab;
    }

    public void setLab(boolean lab) {
        isLab = lab;
    }

    @Override
    public String toString() {
        return day + " " + startTime + "-" + endTime + (isLab ? " (Lab)" : "");
    }
} 