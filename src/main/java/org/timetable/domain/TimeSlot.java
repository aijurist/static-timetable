package org.timetable.domain;

import org.optaplanner.core.api.domain.lookup.PlanningId;

import java.time.DayOfWeek;
import java.time.LocalTime;

public class TimeSlot {
    @PlanningId
    private String id;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private int duration;
    private boolean isLab;

    public TimeSlot() {
    }

    public TimeSlot(String id, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, boolean isLab) {
        this.id = id;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.duration = (int) java.time.Duration.between(startTime, endTime).toHours();
        this.isLab = isLab;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
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

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public boolean isLab() {
        return isLab;
    }

    public void setLab(boolean lab) {
        isLab = lab;
    }

    @Override
    public String toString() {
        return dayOfWeek + " " + startTime + "-" + endTime + (isLab ? " (Lab)" : "");
    }
} 