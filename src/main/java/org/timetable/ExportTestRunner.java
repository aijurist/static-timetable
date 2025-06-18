package org.timetable;

/**
 * Simple test to verify filename sanitization works for problematic names
 */
public class ExportTestRunner {
    
    /**
     * Test method copied from TimetableExporter for testing
     */
    private static String sanitizeFileName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "unnamed";
        }
        
        // Replace invalid characters with underscore
        // Invalid chars: < > : " | ? * \ / and control characters
        String sanitized = name.replaceAll("[<>:\"|?*\\\\/\\p{Cntrl}]", "_")
                              .replaceAll("\\s+", "_")  // Replace whitespace with underscore
                              .replaceAll("_+", "_")    // Replace multiple underscores with single
                              .replaceAll("^_|_$", ""); // Remove leading/trailing underscores
        
        // Handle empty result
        if (sanitized.isEmpty()) {
            sanitized = "unnamed";
        }
        
        // Limit length to prevent issues
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }
        
        return sanitized;
    }
    
    public static void main(String[] args) {
        System.out.println("=== Filename Sanitization Test ===");
        
        String[] testNames = {
            "Mr/Mrs._Placeholder_Name",
            "Dr. John Smith",
            "Prof: Jane Doe",
            "Teacher <with> invalid chars",
            "Normal Teacher Name",
            "   Leading and trailing spaces   ",
            "",
            null,
            "Very_Long_Teacher_Name_That_Exceeds_Normal_Limits_And_Should_Be_Truncated_To_Prevent_File_System_Issues_And_Compatibility_Problems"
        };
        
        for (String testName : testNames) {
            String sanitized = sanitizeFileName(testName);
            System.out.println("Original: '" + testName + "'");
            System.out.println("Sanitized: '" + sanitized + "'");
            System.out.println("Length: " + sanitized.length());
            System.out.println("---");
        }
        
        System.out.println("=== Test Completed ===");
    }
} 