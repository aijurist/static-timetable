package org.timetable.domain;

import org.optaplanner.core.api.domain.lookup.PlanningId;

public class Course {
    @PlanningId
    private String id;
    private String code;
    private String name;
    private String department;
    private int lectureHours;
    private int tutorialHours;
    private int practicalHours;
    private int credits;

    public Course() {
    }

    public Course(String id, String code, String name, String department, int lectureHours, int tutorialHours, int practicalHours, int credits) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.department = department;
        this.lectureHours = lectureHours;
        this.tutorialHours = tutorialHours;
        this.practicalHours = practicalHours;
        this.credits = credits;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public int getLectureHours() {
        return lectureHours;
    }

    public void setLectureHours(int lectureHours) {
        this.lectureHours = lectureHours;
    }

    public int getTutorialHours() {
        return tutorialHours;
    }

    public void setTutorialHours(int tutorialHours) {
        this.tutorialHours = tutorialHours;
    }

    public int getPracticalHours() {
        return practicalHours;
    }

    public void setPracticalHours(int practicalHours) {
        this.practicalHours = practicalHours;
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    @Override
    public String toString() {
        return code + " - " + name;
    }
} 