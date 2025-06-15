package org.timetable.domain;

import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.timetable.config.TimetableConfig;

public class Teacher {
    @PlanningId
    private String id;
    private String name;
    private String email;
    private int maxHours;

    public Teacher() {
    }

    public Teacher(String id, String name, String email, int maxHours) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.maxHours = maxHours;
    }

    public Teacher(String id, String name) {
        this.id = id;
        this.name = name;
        this.maxHours = TimetableConfig.MAX_TEACHER_HOURS; // Default max hours
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getMaxHours() {
        return maxHours;
    }

    public void setMaxHours(int maxHours) {
        this.maxHours = maxHours;
    }

    @Override
    public String toString() {
        return name;
    }
}