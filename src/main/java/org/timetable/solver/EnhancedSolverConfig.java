package org.timetable.solver;

import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.score.director.ScoreDirectorFactoryConfig;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.termination.TerminationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.timetable.domain.Lesson;
import org.timetable.domain.TimetableProblem;
import org.timetable.config.SolverProperties;
import org.optaplanner.core.api.score.ScoreManager;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;

public class EnhancedSolverConfig {
    private static final Logger logger = LoggerFactory.getLogger(EnhancedSolverConfig.class);
    
    public static Solver<TimetableProblem> createEnhancedSolver() {
        int threadCount = SolverProperties.getThreadCount();
        logger.info("Creating enhanced solver with {} threads", threadCount);
        
        SolverConfig solverConfig = new SolverConfig()
                .withSolutionClass(TimetableProblem.class)
                .withEntityClasses(Lesson.class)
                .withScoreDirectorFactory(createScoreDirectorConfig())
                .withTerminationConfig(createTerminationConfig());
        
        // Enable multithreading if supported
        try {
            solverConfig.setMoveThreadCount(String.valueOf(threadCount));
            logger.info("Multithreading enabled with {} threads", threadCount);
        } catch (Exception e) {
            logger.warn("Multithreading not supported in this OptaPlanner version, using single thread");
        }
        
        org.optaplanner.core.api.solver.SolverFactory<TimetableProblem> factory = SolverFactory.create(solverConfig);
        Solver<TimetableProblem> solver = factory.buildSolver();

        // Attach progress logger
        attachProgressLogger(factory, solver);

        return solver;
    }
    
    private static ScoreDirectorFactoryConfig createScoreDirectorConfig() {
        return new ScoreDirectorFactoryConfig()
                .withConstraintProviderClass(OptimizedTimetableConstraintProvider.class);
    }
    
    private static TerminationConfig createTerminationConfig() {
        long solveMinutes = SolverProperties.getSolveMinutes();
        return new TerminationConfig()
                .withMinutesSpentLimit(solveMinutes)
                .withBestScoreFeasible(true)
                .withUnimprovedMinutesSpentLimit(Math.max(5L, solveMinutes / 4)); // Stop if no improvement for 25% of total time
    }
    

    
    public static Solver<TimetableProblem> createQuickSolver() {
        logger.info("Creating quick solver for testing (5 minutes max)");
        
        SolverConfig solverConfig = new SolverConfig()
                .withSolutionClass(TimetableProblem.class)
                .withEntityClasses(Lesson.class)
                .withScoreDirectorFactory(createScoreDirectorConfig())
                .withTerminationConfig(new TerminationConfig()
                        .withMinutesSpentLimit(5L)
                        .withBestScoreFeasible(true));
        
        // Try to enable multithreading for quick solver too
        try {
            int threadCount = Math.min(4, SolverProperties.getThreadCount()); // Limit threads for quick mode
            solverConfig.setMoveThreadCount(String.valueOf(threadCount));
            logger.info("Quick solver using {} threads", threadCount);
        } catch (Exception e) {
            logger.info("Quick solver using single thread");
        }
        
        org.optaplanner.core.api.solver.SolverFactory<TimetableProblem> factory = SolverFactory.create(solverConfig);
        Solver<TimetableProblem> solver = factory.buildSolver();

        // Attach progress logger
        attachProgressLogger(factory, solver);

        return solver;
    }

    private static void attachProgressLogger(org.optaplanner.core.api.solver.SolverFactory<TimetableProblem> factory, Solver<TimetableProblem> solver) {
        try {
            var scoreManager = org.optaplanner.core.api.score.ScoreManager.create(factory);
            solver.addEventListener(event -> {
                var best = event.getNewBestSolution();
                var score = (org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore) event.getNewBestScore();
                var explanation = scoreManager.explainScore(best);
                logger.info("▷ Best score updated: {} (hard={}, soft={})", score, score.getHardScore(), score.getSoftScore());

                explanation.getConstraintMatchTotalMap().forEach((name, cmt) -> {
                    int hard = ((org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore) cmt.getScore()).getHardScore();
                    int soft = ((org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore) cmt.getScore()).getSoftScore();
                    if (hard != 0 || soft != 0) {
                        logger.info("   • {} = {}", name, cmt.getScore());
                    }
                });
            });
        } catch (Exception e) {
            logger.warn("Could not attach progress logger: {}", e.getMessage());
        }
    }
} 