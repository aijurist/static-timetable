package org.timetable.domain;

import java.util.Objects;

/**
 * Represents a course in the timetable system.
 */
public class Course {
    private String code;
    private String name;
    private String department;
    private String courseType; // "theory", "lab", "tutorial"
    private int lectureHours;
    private int labHours;
    private int tutorialHours;
    private int projectHours;
    private Course prerequisite; // Course that must be completed before this one

    public Course() {
    }

    public Course(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public Course(String code, String name, String courseType) {
        this.code = code;
        this.name = name;
        this.courseType = courseType;
    }

    public Course(String code, String name, String department, String courseType, 
                 int lectureHours, int labHours, int tutorialHours, int projectHours) {
        this.code = code;
        this.name = name;
        this.department = department;
        this.courseType = courseType;
        this.lectureHours = lectureHours;
        this.labHours = labHours;
        this.tutorialHours = tutorialHours;
        this.projectHours = projectHours;
    }

    public String getId() {
        return code;
    }

    public String getCode() {
        return code;
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

    public String getCourseType() {
        return courseType;
    }

    public void setCourseType(String courseType) {
        this.courseType = courseType;
    }

    public int getLectureHours() {
        return lectureHours;
    }

    public void setLectureHours(int lectureHours) {
        this.lectureHours = lectureHours;
    }

    public int getLabHours() {
        return labHours;
    }

    public void setLabHours(int labHours) {
        this.labHours = labHours;
    }

    public int getTutorialHours() {
        return tutorialHours;
    }

    public void setTutorialHours(int tutorialHours) {
        this.tutorialHours = tutorialHours;
    }

    public int getProjectHours() {
        return projectHours;
    }

    public void setProjectHours(int projectHours) {
        this.projectHours = projectHours;
    }

    public Course getPrerequisite() {
        return prerequisite;
    }

    public void setPrerequisite(Course prerequisite) {
        this.prerequisite = prerequisite;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Course course = (Course) o;
        return Objects.equals(code, course.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return name + " (" + code + ")";
    }
} 