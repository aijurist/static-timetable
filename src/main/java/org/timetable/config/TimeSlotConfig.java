package org.timetable.config;

import java.time.LocalTime;

public class TimeSlotConfig {
    private final LocalTime startTime;
    private final LocalTime endTime;

    public TimeSlotConfig(LocalTime startTime, LocalTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }
} 