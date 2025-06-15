package org.timetable.validation;

import org.timetable.domain.Lesson;

import java.util.List;

/**
 * Represents a constraint violation in the timetable
 */
public class ConstraintViolation {
    
    private final String type;
    private final String severity; // "HARD" or "SOFT"
    private final String description;
    private final List<Lesson> affectedLessons;
    
    public ConstraintViolation(String type, String severity, String description, List<Lesson> affectedLessons) {
        this.type = type;
        this.severity = severity;
        this.description = description;
        this.affectedLessons = affectedLessons;
    }
    
    public String getType() { return type; }
    public String getSeverity() { return severity; }
    public String getDescription() { return description; }
    public List<Lesson> getAffectedLessons() { return affectedLessons; }
    
    @Override
    public String toString() {
        return String.format("[%s] %s: %s (affects %d lessons)", 
            severity, type, description, affectedLessons.size());
    }
} 