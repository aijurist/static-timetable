package org.timetable;

import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.solver.SolverConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.timetable.domain.*;
import org.timetable.persistence.TimetableDataLoader;
import org.timetable.persistence.TimetableExporter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TimetableApp {
    private static final Logger logger = LoggerFactory.getLogger(TimetableApp.class);

    public static void main(String[] args) {
        // Build the Solver
        SolverFactory<TimetableProblem> solverFactory = SolverFactory.create(
                new SolverConfig().withSolutionClass(TimetableProblem.class)
                        .withEntityClasses(Lesson.class)
                        .withScoreDirectorFactory(
                                new SolverConfig.ScoreDirectorFactoryConfig()
                                        .withSimpleScoreCalculatorClass(
                                                org.timetable.solver.TimetableEasyScoreCalculator.class)));

        Solver<TimetableProblem> solver = solverFactory.buildSolver();

        // Load the problem
        TimetableDataLoader dataLoader = new TimetableDataLoader();
        TimetableProblem problem = dataLoader.loadProblem();

        // Solve the problem
        logger.info("Solving timetable problem...");
        TimetableProblem solution = solver.solve(problem);

        // Display the solution
        logger.info("Solution score: {}", solution.getScore());
        
        // Export the solution
        TimetableExporter exporter = new TimetableExporter();
        exporter.exportSolution(solution);
        
        logger.info("Timetable exported successfully!");
    }
} 