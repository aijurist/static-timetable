package org.timetable.domain;

import org.optaplanner.core.api.domain.lookup.PlanningId;

public class Room {
    @PlanningId
    private String id;
    private String name;
    private String block;
    private String description;
    private int capacity;
    private boolean isLab;
    private boolean isPriorityRoom; // For special rooms
    private String labType; // "core", "computer", or null for non-labs
    
    public Room() {
    }
    
    public Room(String id, String name, String block, String description, int capacity, boolean isLab) {
        this.id = id;
        this.name = name;
        this.block = block;
        this.description = description;
        this.capacity = capacity;
        this.isLab = isLab;
        this.isPriorityRoom = false; // Default to false, can be set explicitly if needed
        this.labType = null; // Default to null
    }

    public Room(String id, String name, String block, String description, int capacity, boolean isLab, String labType) {
        this.id = id;
        this.name = name;
        this.block = block;
        this.description = description;
        this.capacity = capacity;
        this.isLab = isLab;
        this.isPriorityRoom = false; // Default to false, can be set explicitly if needed
        this.labType = labType;
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

    public String getBlock() {
        return block;
    }

    public void setBlock(String block) {
        this.block = block;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public boolean isLab() {
        return isLab;
    }

    public void setLab(boolean lab) {
        isLab = lab;
    }

    public boolean isPriorityRoom() {
        return isPriorityRoom;
    }

    public void setPriorityRoom(boolean priorityRoom) {
        isPriorityRoom = priorityRoom;
    }

    public String getLabType() {
        return labType;
    }

    public void setLabType(String labType) {
        this.labType = labType;
    }

    @Override
    public String toString() {
        return name + (isLab ? " (Lab)" : "") + (isPriorityRoom ? " (Priority)" : "") + 
               (labType != null ? " (" + labType + ")" : "");
    }
} 