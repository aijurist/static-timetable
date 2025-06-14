package org.timetable.domain;

/**
 * Represents academic departments in the timetable system.
 */
public enum Department {
    CSD("Computer Science & Design"),
    CSE("Computer Science & Engineering"),
    CSE_CS("Computer Science & Engineering (Cyber Security)"),
    CSBS("Computer Science & Business Systems"),
    AIML("Artificial Intelligence & Machine Learning");

    private final String fullName;

    Department(String fullName) {
        this.fullName = fullName;
    }

    public String getFullName() {
        return fullName;
    }

    /**
     * Get a Department by its abbreviation.
     * 
     * @param abbreviation The department abbreviation (e.g., "CSE", "AIML")
     * @return The matching Department or null if not found
     */
    public static Department getByAbbreviation(String abbreviation) {
        if (abbreviation == null) return null;
        
        try {
            // Handle the special case for CSE-CS
            if (abbreviation.equals("CSE-CS")) {
                return CSE_CS;
            }
            return Department.valueOf(abbreviation);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Get a Department by its full name.
     * 
     * @param fullName The department's full name
     * @return The matching Department or null if not found
     */
    public static Department getByFullName(String fullName) {
        if (fullName == null) return null;
        
        for (Department dept : values()) {
            if (dept.fullName.equals(fullName)) {
                return dept;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return fullName;
    }
} 