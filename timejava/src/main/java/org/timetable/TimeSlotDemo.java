package org.timetable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.timetable.domain.TimeSlot;
import org.timetable.util.TimeSlotGenerator;
import org.timetable.util.TimetableConstants;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Demo to showcase the time slot generation functionality.
 */
public class TimeSlotDemo {
    private static final Logger logger = LoggerFactory.getLogger(TimeSlotDemo.class);

    public static void main(String[] args) {
        logger.info("Starting Time Slot Demo");
        
        // Generate and display theory time slots
        List<TimeSlot> theorySlots = TimeSlotGenerator.generateTheoryTimeSlots();
        logger.info("Theory Time Slots ({} slots):", theorySlots.size());
        for (int i = 0; i < Math.min(10, theorySlots.size()); i++) {
            logger.info("  {}", theorySlots.get(i));
        }
        if (theorySlots.size() > 10) {
            logger.info("  ... and {} more", theorySlots.size() - 10);
        }
        
        // Generate and display lab time slots
        List<TimeSlot> labSlots = TimeSlotGenerator.generateLabTimeSlots();
        logger.info("\nLab Time Slots ({} slots):", labSlots.size());
        for (int i = 0; i < Math.min(10, labSlots.size()); i++) {
            logger.info("  {}", labSlots.get(i));
        }
        if (labSlots.size() > 10) {
            logger.info("  ... and {} more", labSlots.size() - 10);
        }
        
        // Display lunch break slots for Monday
        List<TimeSlot> lunchSlots = TimeSlotGenerator.getLunchBreakSlots(0);
        logger.info("\nLunch Break Slots for Monday:");
        for (TimeSlot slot : lunchSlots) {
            logger.info("  {}", slot);
        }
        
        // Display shift information
        logger.info("\nShift Information:");
        for (Map.Entry<String, LocalTime[]> entry : TimetableConstants.SHIFTS.entrySet()) {
            logger.info("  {}: {} to {}", 
                    entry.getKey(), 
                    entry.getValue()[0], 
                    entry.getValue()[1]);
        }
        
        // Display department data
        logger.info("\nDepartment Data (sections per year):");
        for (Map.Entry<String, Map<String, Integer>> dept : TimetableConstants.DEPARTMENT_DATA.entrySet()) {
            StringBuilder sb = new StringBuilder();
            sb.append("  ").append(dept.getKey()).append(": ");
            
            for (Map.Entry<String, Integer> year : dept.getValue().entrySet()) {
                sb.append("Year ").append(year.getKey())
                  .append(" (").append(year.getValue()).append(" sections), ");
            }
            
            // Remove trailing comma and space
            if (sb.length() > 2) {
                sb.setLength(sb.length() - 2);
            }
            
            logger.info(sb.toString());
        }
        
        logger.info("\nTime Slot Demo completed successfully!");
    }
} 