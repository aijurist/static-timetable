package org.timetable.util;

/**
 * Enum representing all departments in the institution.
 */
public enum Department {
    // Computer Science and IT departments
    CSE("Computer Science and Engineering"),
    CSE_CS("Computer Science and Engineering - Cyber Security"),
    CSBS("Computer Science and Business Systems"),
    CSD("Computer Science and Design"),
    IT("Information Technology"),
    AIML("Artificial Intelligence and Machine Learning"),
    AIDS("Artificial Intelligence and Data Science"),
    
    // Electronics and Communication departments
    ECE("Electronics and Communication Engineering"),
    EEE("Electrical and Electronics Engineering"),
    
    // Mechanical and related departments
    MECH("Mechanical Engineering"),
    AERO("Aeronautical Engineering"),
    AUTO("Automobile Engineering"),
    MCT("Mechatronics Engineering"),
    
    // Biotechnology and related departments
    BT("Biotechnology"),
    BME("Biomedical Engineering"),
    
    // Other engineering departments
    R_A("Robotics and Automation"),
    FT("Food Technology"),
    CIVIL("Civil Engineering"),
    CHEM("Chemical Engineering");
    
    private final String fullName;
    
    Department(String fullName) {
        this.fullName = fullName;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public String getCode() {
        return this.name().replace("_", "-");
    }
    
    /**
     * Get department from code string (handles both underscore and hyphen formats)
     */
    public static Department fromCode(String code) {
        // Handle special cases
        if ("CSE-CS".equals(code)) return CSE_CS;
        if ("R&A".equals(code)) return R_A;
        if ("R-A".equals(code)) return R_A;  // Handle the converted format
        
        // Try direct match first
        try {
            return Department.valueOf(code);
        } catch (IllegalArgumentException e) {
            // Try with underscores replaced by hyphens
            try {
                return Department.valueOf(code.replace("-", "_"));
            } catch (IllegalArgumentException e2) {
                throw new IllegalArgumentException("Unknown department code: " + code);
            }
        }
    }
} 