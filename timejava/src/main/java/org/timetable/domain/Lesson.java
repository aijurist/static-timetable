package org.timetable.domain;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

import java.util.Objects;

/**
 * Represents a lesson in the timetable system.
 */
@PlanningEntity
public class Lesson {
    @PlanningId
    private Long id;
    
    private Teacher teacher;
    private Course course;
    private StudentGroup studentGroup;
    private String lessonType; // "lecture", "lab", "tutorial"
    private int duration; // in hours
    
    @PlanningVariable(valueRangeProviderRefs = {"timeSlotRange"})
    private TimeSlot timeSlot;
    
    @PlanningVariable(valueRangeProviderRefs = {"roomRange"})
    private Room room;

    public Lesson() {
    }

    public Lesson(Long id, Teacher teacher, Course course, StudentGroup studentGroup, 
                 String lessonType, int duration) {
        this.id = id;
        this.teacher = teacher;
        this.course = course;
        this.studentGroup = studentGroup;
        this.lessonType = lessonType;
        this.duration = duration;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public StudentGroup getStudentGroup() {
        return studentGroup;
    }

    public void setStudentGroup(StudentGroup studentGroup) {
        this.studentGroup = studentGroup;
    }

    public String getLessonType() {
        return lessonType;
    }

    public void setLessonType(String lessonType) {
        this.lessonType = lessonType;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public TimeSlot getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(TimeSlot timeSlot) {
        this.timeSlot = timeSlot;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    /**
     * Check if this lesson conflicts with another lesson.
     */
    public boolean conflictsWith(Lesson other) {
        // Different time slots or rooms means no conflict
        if (this.timeSlot == null || other.timeSlot == null || this.room == null || other.room == null) {
            return false;
        }
        
        // Same time slot and same room means conflict
        if (this.timeSlot.equals(other.timeSlot) && this.room.equals(other.room)) {
            return true;
        }
        
        // Same time slot and same teacher means conflict
        if (this.timeSlot.equals(other.timeSlot) && this.teacher.equals(other.teacher)) {
            return true;
        }
        
        // Same time slot and same student group means conflict
        if (this.timeSlot.equals(other.timeSlot) && this.studentGroup.equals(other.studentGroup)) {
            return true;
        }
        
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lesson lesson = (Lesson) o;
        return Objects.equals(id, lesson.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return course + " - " + lessonType + " - " + studentGroup;
    }
} 