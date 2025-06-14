package org.timetable.domain;

import org.optaplanner.core.api.domain.lookup.PlanningId;

import java.util.Objects;

/**
 * Represents a room in the timetable system.
 */
public class Room {
    @PlanningId
    private String id;
    private String name;
    private String building;
    private String floor;
    private int capacity;
    private boolean isLab;

    public Room() {
    }

    public Room(String id, String name, int capacity) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.isLab = false;
    }

    public Room(String id, String name, String building, String floor, int capacity, boolean isLab) {
        this.id = id;
        this.name = name;
        this.building = building;
        this.floor = floor;
        this.capacity = capacity;
        this.isLab = isLab;
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

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
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

    /**
     * Check if the room is available at the given time slot.
     * This can be extended to handle room-specific availability.
     */
    public boolean isAvailable(TimeSlot timeSlot) {
        // For now, all rooms are available at all times
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Room room = (Room) o;
        return Objects.equals(id, room.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name + (isLab ? " (Lab)" : "");
    }
} 