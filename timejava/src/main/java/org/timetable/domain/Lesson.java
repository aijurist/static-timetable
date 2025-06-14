package org.timetable.domain;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class Lesson {
    @PlanningId
    private String id;
    private Teacher teacher;
    private Course course;
    private StudentGroup studentGroup;
    private String sessionType; // lecture, lab, tutorial
    private String pattern; // A1, A2, A3, TA1, TA2
    private String labBatch;

    @PlanningVariable(valueRangeProviderRefs = "timeSlotRange")
    private TimeSlot timeSlot;

    @PlanningVariable(valueRangeProviderRefs = "roomRange")
    private Room room;

    public Lesson() {
    }

    public Lesson(String id, Teacher teacher, Course course, StudentGroup studentGroup, 
                  String sessionType, String pattern, String labBatch) {
        this.id = id;
        this.teacher = teacher;
        this.course = course;
        this.studentGroup = studentGroup;
        this.sessionType = sessionType;
        this.pattern = pattern;
        this.labBatch = labBatch;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    public String getSessionType() {
        return sessionType;
    }

    public void setSessionType(String sessionType) {
        this.sessionType = sessionType;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getLabBatch() {
        return labBatch;
    }

    public void setLabBatch(String labBatch) {
        this.labBatch = labBatch;
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

    @Override
    public String toString() {
        return course + " by " + teacher + " for " + studentGroup + " (" + sessionType + ")";
    }
} 