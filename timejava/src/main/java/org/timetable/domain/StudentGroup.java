package org.timetable.domain;

import org.optaplanner.core.api.domain.lookup.PlanningId;
import java.util.Objects;

/**
 * Represents a student group in the timetable system.
 */
public class StudentGroup {
    @PlanningId
    private String id;
    private String name;
    private int size;
    private int year;
    private String section;
    private Department department;

    public StudentGroup() {
    }

    public StudentGroup(String id, String name, int size) {
        this.id = id;
        this.name = name;
        this.size = size;
    }

    public StudentGroup(String id, String name, int size, Department department) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.department = department;
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

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StudentGroup that = (StudentGroup) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name;
    }
} 