package org.timetable;

import org.timetable.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class RoomAvailabilityAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(RoomAvailabilityAnalyzer.class);

    public static void generateRoomAvailabilityCSV(TimetableProblem solution) {
        List<Room> rooms = solution.getRooms();
        List<TimeSlot> timeSlots = solution.getTimeSlots();
        List<Lesson> lessons = solution.getLessons();

        // Separate rooms into classrooms and labs
        List<Room> classrooms = rooms.stream()
                .filter(room -> !room.isLab())
                .collect(Collectors.toList());
        List<Room> labs = rooms.stream()
                .filter(room -> room.isLab())
                .collect(Collectors.toList());

        // Generate CSV for classrooms
        generateRoomTypeAvailabilityCSV(classrooms, timeSlots, lessons, "output/classroom_availability.csv");
        // Generate CSV for labs
        generateRoomTypeAvailabilityCSV(labs, timeSlots, lessons, "output/lab_availability.csv");
        
        logger.info("Generated room availability CSV files: classroom_availability.csv and lab_availability.csv");
    }

    private static void generateRoomTypeAvailabilityCSV(List<Room> rooms, List<TimeSlot> timeSlots, 
            List<Lesson> lessons, String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            // Write header
            writer.write("Room");
            for (TimeSlot slot : timeSlots) {
                writer.write("," + slot.getDayOfWeek() + "_" + slot.getStartTime());
            }
            writer.write("\n");

            // Create a map of room-timeSlot assignments
            Map<Room, Set<TimeSlot>> roomAssignments = new HashMap<>();
            for (Lesson lesson : lessons) {
                if (lesson.getRoom() != null && lesson.getTimeSlot() != null) {
                    roomAssignments.computeIfAbsent(lesson.getRoom(), k -> new HashSet<>())
                            .add(lesson.getTimeSlot());
                }
            }

            // Write data for each room
            for (Room room : rooms) {
                writer.write(room.getName());
                for (TimeSlot slot : timeSlots) {
                    boolean isOccupied = roomAssignments.containsKey(room) && 
                            roomAssignments.get(room).contains(slot);
                    writer.write("," + (isOccupied ? "1" : "0"));
                }
                writer.write("\n");
            }
        } catch (IOException e) {
            logger.error("Failed to write room availability CSV: " + filename, e);
        }
    }
} 