package org.timetable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.timetable.domain.Department;
import org.timetable.domain.StudentGroup;
import org.timetable.domain.TimeSlot;
import org.timetable.util.DepartmentMapper;
import org.timetable.util.TimeSlotGenerator;
import org.timetable.util.TimetableConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive demo that brings together all components.
 */
public class ComprehensiveDemo {
    private static final Logger logger = LoggerFactory.getLogger(ComprehensiveDemo.class);

    public static void main(String[] args) {
        logger.info("Starting Comprehensive Timetable Demo");
        
        // 1. Generate time slots
        List<TimeSlot> allTimeSlots = TimeSlotGenerator.generateAllTimeSlots();
        logger.info("Generated {} time slots ({} theory, {} lab)",
                allTimeSlots.size(),
                TimeSlotGenerator.generateTheoryTimeSlots().size(),
                TimeSlotGenerator.generateLabTimeSlots().size());
        
        // 2. Create student groups for all departments
        List<StudentGroup> allStudentGroups = generateStudentGroups();
        logger.info("Generated {} student groups", allStudentGroups.size());
        
        // 3. Display some sample student groups
        logger.info("\nSample Student Groups:");
        for (int i = 0; i < Math.min(10, allStudentGroups.size()); i++) {
            StudentGroup group = allStudentGroups.get(i);
            logger.info("  {} (Department: {}, Year: {}, Section: {}, Students: {})",
                    group,
                    group.getDepartment(),
                    group.getYear(),
                    group.getSection(),
                    group.getStudentCount());
        }
        
        // 4. Display department mapping
        logger.info("\nDepartment Mapping:");
        for (Department dept : Department.values()) {
            logger.info("  {} -> {}", dept.name(), dept.getFullName());
        }
        
        // 5. Display shift patterns
        logger.info("\nShift Patterns:");
        for (int i = 0; i < TimetableConstants.SHIFT_PATTERNS.size(); i++) {
            Map<String, Integer> pattern = TimetableConstants.SHIFT_PATTERNS.get(i);
            logger.info("  Pattern {}: {}", i+1, pattern);
        }
        
        logger.info("\nComprehensive Demo completed successfully!");
    }
    
    /**
     * Generate student groups for all departments based on the department data.
     */
    private static List<StudentGroup> generateStudentGroups() {
        List<StudentGroup> groups = new ArrayList<>();
        int groupId = 1;
        
        // For each department in the department data
        for (Map.Entry<String, Map<String, Integer>> deptEntry : TimetableConstants.DEPARTMENT_DATA.entrySet()) {
            String deptCode = deptEntry.getKey();
            Department dept = Department.getByAbbreviation(deptCode);
            
            // If department is not in our enum, skip it
            if (dept == null) continue;
            
            // For each year in the department
            for (Map.Entry<String, Integer> yearEntry : deptEntry.getValue().entrySet()) {
                int year = Integer.parseInt(yearEntry.getKey());
                int numSections = yearEntry.getValue();
                
                // For each section
                for (int section = 1; section <= numSections; section++) {
                    String sectionLetter = (char)('A' + section - 1) + "";
                    String groupName = deptCode + "-" + year + sectionLetter;
                    
                    StudentGroup group = new StudentGroup(
                            "SG" + groupId++,
                            groupName,
                            TimetableConstants.CLASS_STRENGTH,
                            dept
                    );
                    group.setYear(year);
                    group.setSection(sectionLetter);
                    
                    groups.add(group);
                }
            }
        }
        
        return groups;
    }
} 