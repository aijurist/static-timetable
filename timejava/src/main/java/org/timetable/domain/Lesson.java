package org.timetable.domain;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;
import org.timetable.config.TimetableConfig;

import java.time.LocalTime; // Import LocalTime

@PlanningEntity
public class Lesson {
    @PlanningId
    private String id;
    private Teacher teacher;
    private Course course;
    private StudentGroup studentGroup;
    private String sessionType; // "lecture", "lab", "tutorial"
    private String labBatch; // "B1", "B2", or null for full class sessions

    @PlanningVariable(valueRangeProviderRefs = "timeSlotRange")
    private TimeSlot timeSlot;

    @PlanningVariable(valueRangeProviderRefs = "roomRange")
    private Room room;

    public Lesson() {
    }

    public Lesson(String id, Teacher teacher, Course course, StudentGroup studentGroup, String sessionType, String labBatch) {
        this.id = id;
        this.teacher = teacher;
        this.course = course;
        this.studentGroup = studentGroup;
        this.sessionType = sessionType;
        this.labBatch = labBatch;
    }
    
    // NEW: Helper method to get the start time of the assigned timeslot.
    public LocalTime getStartTime() {
        return timeSlot != null ? timeSlot.getStartTime() : null;
    }
    
    // NEW: Helper method to get the end time of the assigned timeslot.
    public LocalTime getEndTime() {
        return timeSlot != null ? timeSlot.getEndTime() : null;
    }

    public int getRequiredCapacity() {
        if (isSplitBatch()) {
            return TimetableConfig.LAB_BATCH_SIZE;
        }
        return this.studentGroup.getSize();
    }

    public boolean requiresLabRoom() {
        return "lab".equals(this.sessionType);
    }

    public boolean requiresTheoryRoom() {
        return "lecture".equals(this.sessionType) || "tutorial".equals(this.sessionType);
    }

    public boolean isSplitBatch() {
        return this.labBatch != null;
    }

    public int getEffectiveHours() {
        if (timeSlot == null) {
            return 0;
        }
        if (timeSlot.isLab()) {
            return 2;
        }
        return 1;
    }

    // --- Getters and Setters ---

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

    public String getBatch() {
        return getLabBatch();
    }

    @Override
    public String toString() {
        String batchInfo = labBatch != null ? " (" + labBatch + ")" : "";
        return course + " for " + studentGroup + " (" + sessionType + batchInfo + ")";
    }
}