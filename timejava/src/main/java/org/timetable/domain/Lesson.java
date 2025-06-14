package org.timetable.domain;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

import java.time.temporal.ChronoUnit;

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

    // No-arg constructor required for OptaPlanner
    public Lesson() {
    }

    public Lesson(Course course, Teacher teacher, StudentGroup studentGroup, String sessionType) {
        this.id = String.format("%s_%s_%s_%s", course.getId(), teacher.getId(), studentGroup.getId(), sessionType);
        this.course = course;
        this.teacher = teacher;
        this.studentGroup = studentGroup;
        this.sessionType = sessionType;
    }

    public Lesson(String id, Teacher teacher, Course course, StudentGroup studentGroup, String sessionType, String labBatch) {
        this.id = id;
        this.teacher = teacher;
        this.course = course;
        this.studentGroup = studentGroup;
        this.sessionType = sessionType;
        this.labBatch = labBatch;
    }
    
    /**
     * @return The required capacity for this lesson. Labs with batches need smaller rooms.
     */
    public int getRequiredCapacity() {
        if ("lab".equals(this.sessionType) && this.labBatch != null) {
            return 35; // Standard lab batch size
        }
        return this.studentGroup.getSize(); // Full class size
    }

    /**
     * @return The duration of the lesson in hours, based on its assigned timeslot.
     */
    public long getDurationInHours() {
        if (timeSlot == null) {
            return 0;
        }
        return ChronoUnit.HOURS.between(timeSlot.getStartTime(), timeSlot.getEndTime());
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

    @Override
    public String toString() {
        String batchInfo = labBatch != null ? " (" + labBatch + ")" : "";
        return course + " for " + studentGroup + " (" + sessionType + batchInfo + ")";
    }
}