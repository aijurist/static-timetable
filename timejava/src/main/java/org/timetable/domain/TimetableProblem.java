package org.timetable.domain;

import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;

import java.util.List;

@PlanningSolution
public class TimetableProblem {

    private String name;
    
    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "timeSlotRange")
    private List<TimeSlot> timeSlots;

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "roomRange")
    private List<Room> rooms;

    @ProblemFactCollectionProperty
    private List<Teacher> teachers;

    @ProblemFactCollectionProperty
    private List<Course> courses;

    @ProblemFactCollectionProperty
    private List<StudentGroup> studentGroups;

    @PlanningEntityCollectionProperty
    private List<Lesson> lessons;

    @PlanningScore
    private HardSoftScore score;

    // No-arg constructor required for OptaPlanner
    public TimetableProblem() {
    }

    public TimetableProblem(String name, List<TimeSlot> timeSlots, List<Room> rooms, 
                           List<Teacher> teachers, List<Course> courses, 
                           List<StudentGroup> studentGroups, List<Lesson> lessons) {
        this.name = name;
        this.timeSlots = timeSlots;
        this.rooms = rooms;
        this.teachers = teachers;
        this.courses = courses;
        this.studentGroups = studentGroups;
        this.lessons = lessons;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<TimeSlot> getTimeSlots() {
        return timeSlots;
    }

    public void setTimeSlots(List<TimeSlot> timeSlots) {
        this.timeSlots = timeSlots;
    }

    public List<Room> getRooms() {
        return rooms;
    }

    public void setRooms(List<Room> rooms) {
        this.rooms = rooms;
    }

    public List<Teacher> getTeachers() {
        return teachers;
    }

    public void setTeachers(List<Teacher> teachers) {
        this.teachers = teachers;
    }

    public List<Course> getCourses() {
        return courses;
    }

    public void setCourses(List<Course> courses) {
        this.courses = courses;
    }

    public List<StudentGroup> getStudentGroups() {
        return studentGroups;
    }

    public void setStudentGroups(List<StudentGroup> studentGroups) {
        this.studentGroups = studentGroups;
    }

    public List<Lesson> getLessons() {
        return lessons;
    }

    public void setLessons(List<Lesson> lessons) {
        this.lessons = lessons;
    }

    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }
} 