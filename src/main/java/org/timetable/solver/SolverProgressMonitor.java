package org.timetable.solver;

import org.optaplanner.core.api.solver.event.BestSolutionChangedEvent;
import org.optaplanner.core.api.solver.event.SolverEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.timetable.domain.TimetableProblem;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public class SolverProgressMonitor implements SolverEventListener<TimetableProblem> {
    private static final Logger logger = LoggerFactory.getLogger(SolverProgressMonitor.class);
    
    private final LocalDateTime startTime;
    private final AtomicInteger improvementCount = new AtomicInteger(0);
    private volatile String lastBestScore = "N/A";
    private volatile LocalDateTime lastImprovementTime;
    
    public SolverProgressMonitor() {
        this.startTime = LocalDateTime.now();
        this.lastImprovementTime = startTime;
    }
    
    @Override
    public void bestSolutionChanged(BestSolutionChangedEvent<TimetableProblem> event) {
        TimetableProblem newBestSolution = event.getNewBestSolution();
        String newScore = newBestSolution.getScore() != null ? newBestSolution.getScore().toString() : "N/A";
        
        if (!newScore.equals(lastBestScore)) {
            lastBestScore = newScore;
            lastImprovementTime = LocalDateTime.now();
            int improvements = improvementCount.incrementAndGet();
            
            Duration totalTime = Duration.between(startTime, lastImprovementTime);
            long assignedLessons = newBestSolution.getLessons().stream()
                    .filter(lesson -> lesson.getTimeSlot() != null && lesson.getRoom() != null)
                    .count();
            
            logger.info("Improvement #{}: Score={}, Assigned={}/{}, Time={}",
                    improvements,
                    newScore,
                    assignedLessons,
                    newBestSolution.getLessons().size(),
                    formatDuration(totalTime));
            
            // Log feasibility status
            if (newBestSolution.getScore() != null) {
                if (newBestSolution.getScore().isFeasible()) {
                    logger.info("✓ Solution is FEASIBLE (all hard constraints satisfied)");
                } else {
                    logger.warn("✗ Solution is NOT feasible ({} hard constraint violations)",
                            -newBestSolution.getScore().getHardScore());
                }
            }
        }
    }
    
    public void logFinalStats() {
        Duration totalTime = Duration.between(startTime, LocalDateTime.now());
        Duration timeSinceLastImprovement = Duration.between(lastImprovementTime, LocalDateTime.now());
        
        logger.info("--- Solver Statistics ---");
        logger.info("Total solving time: {}", formatDuration(totalTime));
        logger.info("Time since last improvement: {}", formatDuration(timeSinceLastImprovement));
        logger.info("Total improvements found: {}", improvementCount.get());
        logger.info("Final best score: {}", lastBestScore);
        
        if (improvementCount.get() > 0) {
            double improvementsPerMinute = improvementCount.get() / Math.max(1.0, totalTime.toMinutes());
            logger.info("Average improvements per minute: {:.2f}", improvementsPerMinute);
        }
    }
    
    private String formatDuration(Duration duration) {
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