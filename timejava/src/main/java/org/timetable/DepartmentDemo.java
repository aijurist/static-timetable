package org.timetable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.timetable.domain.Department;
import org.timetable.domain.StudentGroup;
import org.timetable.util.DepartmentMapper;

/**
 * Demo to showcase the department mapping functionality.
 */
public class DepartmentDemo {
    private static final Logger logger = LoggerFactory.getLogger(DepartmentDemo.class);

    public static void main(String[] args) {
        logger.info("Starting Department Mapping Demo");
        
        // Test department mapping by full name
        String[] departmentNames = {
            "Computer Science & Design",
            "Computer Science & Engineering",
            "Computer Science & Engineering (Cyber Security)",
            "Computer Science & Business Systems",
            "Artificial Intelligence & Machine Learning"
        };
        
        logger.info("Department mappings by full name:");
        for (String fullName : departmentNames) {
            String abbreviation = DepartmentMapper.getAbbreviation(fullName);
            Department dept = DepartmentMapper.getDepartmentByFullName(fullName);
            logger.info("{} -> {} ({})", fullName, abbreviation, dept);
        }
        
        logger.info("\nDepartment mappings by abbreviation:");
        String[] abbreviations = {"CSD", "CSE", "CSE-CS", "CSBS", "AIML"};
        for (String abbr : abbreviations) {
            String fullName = DepartmentMapper.getFullName(abbr);
            Department dept = DepartmentMapper.getDepartmentByAbbreviation(abbr);
            logger.info("{} -> {} ({})", abbr, fullName, dept);
        }
        
        // Create student groups with departments
        logger.info("\nCreating student groups with departments:");
        StudentGroup cseGroup = new StudentGroup("CSE-2023", "CSE Batch 2023", 60, Department.CSE);
        StudentGroup aimlGroup = new StudentGroup("AIML-2023", "AIML Batch 2023", 40, Department.AIML);
        
        cseGroup.setYear(2);
        cseGroup.setSection("A");
        
        aimlGroup.setYear(1);
        aimlGroup.setSection("B");
        
        logger.info("Student Group 1: {} (Department: {})", cseGroup, cseGroup.getDepartment());
        logger.info("Student Group 2: {} (Department: {})", aimlGroup, aimlGroup.getDepartment());
        
        logger.info("\nDepartment Demo completed successfully!");
    }
} 