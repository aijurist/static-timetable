package org.timetable.persistence;

public class RawDataRecord {
    private final String courseId;
    private final String courseCode;
    private final String courseName;
    private final String courseDept;
    private final int semester;
    private final String teacherId;
    private final String firstName;
    private final String lastName;
    private final String teacherEmail;
    private final int lectureHours;
    private final int tutorialHours;
    private final int practicalHours;
    private final int credits;

    public RawDataRecord(String courseId, String courseCode, String courseName, String courseDept,
            int semester, String teacherId, String firstName, String lastName, String teacherEmail,
            int lectureHours, int tutorialHours, int practicalHours, int credits) {
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.courseDept = courseDept;
        this.semester = semester;
        this.teacherId = teacherId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.teacherEmail = teacherEmail;
        this.lectureHours = lectureHours;
        this.tutorialHours = tutorialHours;
        this.practicalHours = practicalHours;
        this.credits = credits;
    }

    @Override
    public String toString() {
        return String.format("RawDataRecord{courseId='%s', courseCode='%s', courseName='%s', " +
                "courseDept='%s', semester=%d, teacherId='%s', firstName='%s', lastName='%s', " +
                "teacherEmail='%s', lectureHours=%d, tutorialHours=%d, practicalHours=%d, credits=%d}",
                courseId, courseCode, courseName, courseDept, semester, teacherId, firstName, lastName,
                teacherEmail, lectureHours, tutorialHours, practicalHours, credits);
    }
} 