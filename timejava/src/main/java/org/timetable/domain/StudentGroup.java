package org.timetable.domain;

import org.optaplanner.core.api.domain.lookup.PlanningId;

public class StudentGroup {
    @PlanningId
    private String id;
    private String name;
    private int size;

    public StudentGroup() {
    }

    public StudentGroup(String id, String name, int size) {
        this.id = id;
        this.name = name;
        this.size = size;
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

    @Override
    public String toString() {
        return name;
    }
} 