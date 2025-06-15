package org.timetable.config;

import java.time.LocalTime;
import java.util.List;

public class TimetableConfig {
    
    public static final List<String> DAYS = List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday");

    // Theory time slots
    public static final List<LocalTime[]> THEORY_TIME_SLOTS = List.of(
            new LocalTime[]{LocalTime.of(8, 0), LocalTime.of(8, 50)},
            new LocalTime[]{LocalTime.of(9, 0), LocalTime.of(9, 50)},
            new LocalTime[]{LocalTime.of(10, 0), LocalTime.of(10, 50)},
            new LocalTime[]{LocalTime.of(11, 0), LocalTime.of(11, 50)},
            new LocalTime[]{LocalTime.of(12, 0), LocalTime.of(12, 50)},
            new LocalTime[]{LocalTime.of(13, 0), LocalTime.of(13, 50)},
            new LocalTime[]{LocalTime.of(14, 0), LocalTime.of(14, 50)},
            new LocalTime[]{LocalTime.of(15, 0), LocalTime.of(15, 50)},
            new LocalTime[]{LocalTime.of(16, 0), LocalTime.of(16, 50)},
            new LocalTime[]{LocalTime.of(17, 0), LocalTime.of(17, 50)},
            new LocalTime[]{LocalTime.of(18, 0), LocalTime.of(18, 50)}
    );

    // Lab time slots
    public static final List<LocalTime[]> LAB_TIME_SLOTS = List.of(
            new LocalTime[]{LocalTime.of(8, 0), LocalTime.of(9, 40)},
            new LocalTime[]{LocalTime.of(9, 50), LocalTime.of(11, 30)},
            new LocalTime[]{LocalTime.of(11, 50), LocalTime.of(13, 30)},
            new LocalTime[]{LocalTime.of(13, 50), LocalTime.of(15, 30)},
            new LocalTime[]{LocalTime.of(15, 50), LocalTime.of(17, 30)},
            new LocalTime[]{LocalTime.of(17, 30), LocalTime.of(19, 10)} 
    );

    // Constants
    public static final int MAX_TEACHER_HOURS = 21;
    public static final int CLASS_STRENGTH = 70;
    public static final int LAB_BATCH_SIZE = 35;
    public static final int LAB_DURATION_IN_MINUTES = 100; 

}