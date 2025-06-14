package org.timetable.domain;

import org.optaplanner.core.api.domain.lookup.PlanningId;

public class StudentGroup {
    @PlanningId
    private String id;
    private String name;
    private int size;
    private String department; // CSD, CSE, etc.
    private int year; // 2, 3, or 4

    public StudentGroup() {
    }

    public StudentGroup(String id, String name, int size) {
        this.id = id;
        this.name = name;
        this.size = size;
        // Extract department and year from name
        String[] parts = name.split(" ");
        this.year = Integer.parseInt(parts[1]);
        this.department = parts[2];
    }

    public StudentGroup(String id, String name, int size, String department, int year) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.department = department;
        this.year = year;
    }

    // --- Getters and Setters ---
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

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    @Override
    public String toString() {
        return name;
    }
}