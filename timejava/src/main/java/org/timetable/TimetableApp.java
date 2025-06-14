package org.timetable;

import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.SolverManagerConfig;
import org.optaplanner.core.config.solver.termination.TerminationConfig;
import org.optaplanner.core.config.score.director.ScoreDirectorFactoryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.timetable.domain.Lesson;
import org.timetable.domain.TimetableProblem;
import org.timetable.persistence.TimetableDataLoader;
import org.timetable.persistence.TimetableExporter;
import org.timetable.solver.TimetableConstraintProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimetableApp {
    private static final Logger logger = LoggerFactory.getLogger(TimetableApp.class);

    public static void main(String[] args) {
        String coursesFile = args.length > 0 ? args[0] : "data/courses/cse_dept_red.csv";
        String dataDir = args.length > 1 ? args[1] : "data";
        
        try {
            Files.createDirectories(Paths.get("output/teacher_timetables"));
            Files.createDirectories(Paths.get("output/student_timetables"));
        } catch (IOException e) {
            logger.error("Failed to create output directories", e);
            return;
        }

        logger.info("Loading timetable problem...");
        TimetableProblem problem = TimetableDataLoader.loadProblem(coursesFile, dataDir);
        
        logger.info("--- Problem Statistics ---");
        logger.info("Loaded {} teachers", problem.getTeachers().size());
        logger.info("Loaded {} courses", problem.getCourses().size());
        logger.info("Loaded {} student groups", problem.getStudentGroups().size());
        logger.info("Loaded {} rooms", problem.getRooms().size());
        logger.info("Created {} lessons to schedule", problem.getLessons().size());
        
        logger.info("Creating multithreaded solver...");
        Solver<TimetableProblem> solver = createSolver();
        
        logger.info("Solving timetable problem... (will terminate if score is feasible or after 5 minutes)");
        LocalDateTime solveStartTime = LocalDateTime.now();
        TimetableProblem solution = solver.solve(problem);
        LocalDateTime solveEndTime = LocalDateTime.now();
        
        Duration solvingTime = Duration.between(solveStartTime, solveEndTime);
        logger.info("Solved in {}.", formatDuration(solvingTime));
        logger.info("Final score: {}", solution.getScore());
        
        long assignedLessons = solution.getLessons().stream()
            .filter(lesson -> lesson.getTimeSlot() != null && lesson.getRoom() != null)
            .count();
        logger.info("Assigned lessons: {}/{}", assignedLessons, solution.getLessons().size());
        
        if (solution.getScore().isFeasible()) {
            logger.info("Solution is FEASIBLE (all hard constraints are met).");
        } else {
            logger.warn("Solution is NOT FEASIBLE ({} hard constraints broken).", -solution.getScore().getHardScore());
        }

        try {
            String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
            String outputFile = "output/timetable_solution_" + timestamp + ".csv";
            
            logger.info("Exporting full solution to {}", outputFile);
            TimetableExporter.exportTimetableToCsv(solution, outputFile);
            
            logger.info("Exporting individual teacher timetables...");
            TimetableExporter.exportTeacherTimetables(solution, "output/teacher_timetables");
            
            logger.info("Exporting individual student group timetables...");
            TimetableExporter.exportStudentGroupTimetables(solution, "output/student_timetables");
            
            logger.info("Export completed successfully.");
        } catch (IOException e) {
            logger.error("Failed to export solution", e);
        }
    }
    
    private static Solver<TimetableProblem> createSolver() {
        SolverConfig solverConfig = new SolverConfig()
            .withSolutionClass(TimetableProblem.class)
            .withEntityClasses(Lesson.class)
            .withScoreDirectorFactory(new ScoreDirectorFactoryConfig()
                .withConstraintProviderClass(TimetableConstraintProvider.class))
            .withTerminationConfig(new TerminationConfig()
                .withMinutesSpentLimit(5L)
                .withBestScoreFeasible(true))
            // THIS IS THE NEW LINE: Enable multithreading
            .withMoveThreadCount("AUTO"); 
        
        return SolverFactory.<TimetableProblem>create(solverConfig).buildSolver();
    }
    
    private static String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        long absSeconds = Math.abs(seconds);
        String positive = String.format(
            "%d:%02d:%02d",
            absSeconds / 3600,
            (absSeconds % 3600) / 60,
            absSeconds % 60);
        return seconds < 0 ? "-" + positive : positive;
    }
}