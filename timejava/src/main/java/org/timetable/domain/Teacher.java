package org.timetable.domain;

import org.optaplanner.core.api.domain.lookup.PlanningId;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a teacher in the timetable system.
 */
public class Teacher {
    @PlanningId
    private String id;
    private String name;
    private String department;
    private int maxHoursPerWeek;
    private Set<TimeSlot> unavailableTimeSlots;

    public Teacher() {
        this.unavailableTimeSlots = new HashSet<>();
    }

    public Teacher(String id, String name) {
        this.id = id;
        this.name = name;
        this.unavailableTimeSlots = new HashSet<>();
    }

    public Teacher(String id, String name, String department, int maxHoursPerWeek) {
        this.id = id;
        this.name = name;
        this.department = department;
        this.maxHoursPerWeek = maxHoursPerWeek;
        this.unavailableTimeSlots = new HashSet<>();
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

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public int getMaxHoursPerWeek() {
        return maxHoursPerWeek;
    }

    public void setMaxHoursPerWeek(int maxHoursPerWeek) {
        this.maxHoursPerWeek = maxHoursPerWeek;
    }

    public Set<TimeSlot> getUnavailableTimeSlots() {
        return unavailableTimeSlots;
    }

    public void setUnavailableTimeSlots(Set<TimeSlot> unavailableTimeSlots) {
        this.unavailableTimeSlots = unavailableTimeSlots;
    }

    /**
     * Add a time slot to the list of unavailable time slots.
     */
    public void addUnavailableTimeSlot(TimeSlot timeSlot) {
        this.unavailableTimeSlots.add(timeSlot);
    }

    /**
     * Check if the teacher is available at the given time slot.
     */
    public boolean isAvailable(TimeSlot timeSlot) {
        return !unavailableTimeSlots.contains(timeSlot);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Teacher teacher = (Teacher) o;
        return Objects.equals(id, teacher.id);
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