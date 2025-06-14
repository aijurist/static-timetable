package org.timetable;

import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.termination.TerminationConfig;
import org.optaplanner.core.config.score.director.ScoreDirectorFactoryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.timetable.domain.TimetableProblem;
import org.timetable.persistence.TimetableDataLoader;
import org.timetable.persistence.TimetableExporter;
import org.timetable.solver.TimetableConstraintProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimetableApp {
    private static final Logger logger = LoggerFactory.getLogger(TimetableApp.class);

    public static void main(String[] args) {
        // Get file paths from arguments or use defaults
        String coursesFile = args.length > 0 ? args[0] : "data/courses/cse_dept_red.csv";
        String dataDir = args.length > 1 ? args[1] : "data";
        
        // Create output directories if they don't exist
        try {
            Files.createDirectories(Paths.get("output"));
            Files.createDirectories(Paths.get("output/teacher_timetables"));
            Files.createDirectories(Paths.get("output/student_timetables"));
        } catch (IOException e) {
            logger.error("Failed to create output directories", e);
            return;
        }

        // Load problem
        logger.info("Loading timetable problem...");
        TimetableProblem problem = TimetableDataLoader.loadProblem(coursesFile, dataDir);
        
        // Print some statistics
        logger.info("Loaded {} teachers", problem.getTeachers().size());
        logger.info("Loaded {} courses", problem.getCourses().size());
        logger.info("Loaded {} rooms", problem.getRooms().size());
        logger.info("Created {} lessons to schedule", problem.getLessons().size());
        
        // Create solver
        logger.info("Creating solver...");
        Solver<TimetableProblem> solver = createSolver();
        
        // Solve problem
        logger.info("Solving timetable problem...");
        LocalDateTime solveStartTime = LocalDateTime.now();
        TimetableProblem solution = solver.solve(problem);
        LocalDateTime solveEndTime = LocalDateTime.now();
        
        // Log solution statistics
        Duration solvingTime = Duration.between(solveStartTime, solveEndTime);
        logger.info("Solved timetable problem in {}.", formatDuration(solvingTime));
        logger.info("Score: {}", solution.getScore());
        
        // Count assigned and unassigned lessons
        long assignedLessons = solution.getLessons().stream()
            .filter(lesson -> lesson.getTimeSlot() != null && lesson.getRoom() != null)
            .count();
        logger.info("Assigned lessons: {}/{}", assignedLessons, solution.getLessons().size());
        
        // Export solution
        try {
            String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
            String outputFile = "output/timetable_solution_" + timestamp + ".csv";
            
            logger.info("Exporting solution to {}", outputFile);
            TimetableExporter.exportTimetableToCsv(solution, outputFile);
            
            logger.info("Exporting teacher timetables...");
            TimetableExporter.exportTeacherTimetables(solution, "output/teacher_timetables");
            
            logger.info("Exporting student group timetables...");
            TimetableExporter.exportStudentGroupTimetables(solution, "output/student_timetables");
            
            logger.info("Export completed successfully.");
        } catch (IOException e) {
            logger.error("Failed to export solution", e);
        }
    }
    
    private static Solver<TimetableProblem> createSolver() {
        SolverConfig solverConfig = new SolverConfig()
            .withEntityClasses(org.timetable.domain.Lesson.class)
            .withSolutionClass(TimetableProblem.class)
            .withScoreDirectorFactory(new ScoreDirectorFactoryConfig()
                .withEasyScoreCalculatorClass(TimetableConstraintProvider.class))
            .withTerminationConfig(new TerminationConfig()
                .withMinutesSpentLimit(5L)); // 5 minutes time limit
        
        return SolverFactory.<TimetableProblem>create(solverConfig).buildSolver();
    }
    
    private static String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        return String.format("%d hours, %d minutes, %d seconds",
            hours, minutes % 60, seconds % 60);
    }
} 