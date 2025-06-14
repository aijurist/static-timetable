package org.timetable.domain;

import org.optaplanner.core.api.domain.lookup.PlanningId;

import java.util.ArrayList;
import java.util.List;

public class Teacher {
    @PlanningId
    private String id;
    private String name;
    private String email;
    private int maxHours;
    private int teachingLoad;
    private List<Course> assignedCourses;

    public Teacher() {
        this.assignedCourses = new ArrayList<>();
    }

    public Teacher(String id, String name, String email, int maxHours) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.maxHours = maxHours;
        this.teachingLoad = 0;
        this.assignedCourses = new ArrayList<>();
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

    public int getTeachingLoad() {
        return teachingLoad;
    }

    public void setTeachingLoad(int teachingLoad) {
        this.teachingLoad = teachingLoad;
    }

    public List<Course> getAssignedCourses() {
        return assignedCourses;
    }

    public void setAssignedCourses(List<Course> assignedCourses) {
        this.assignedCourses = assignedCourses;
    }

    public void addCourse(Course course) {
        if (!assignedCourses.contains(course)) {
            assignedCourses.add(course);
            teachingLoad++;
        }
    }

    @Override
    public String toString() {
        return name;
    }
} 