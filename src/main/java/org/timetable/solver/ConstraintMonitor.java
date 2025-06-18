package org.timetable.solver;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Monitors constraint violations and scores to provide insights into timetable optimization progress.
 * Tracks statistics over time and provides detailed reporting on constraint satisfaction.
 */
public class ConstraintMonitor {
    private static final Logger LOGGER = Logger.getLogger(ConstraintMonitor.class.getName());
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // Singleton instance
    private static final ConstraintMonitor INSTANCE = new ConstraintMonitor();
    
    // Constraint tracking data structures
    private final Map<String, AtomicInteger> hardViolationCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> softViolationCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> hardScoreContributions = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> softScoreContributions = new ConcurrentHashMap<>();
    
    // Tracking over time
    private final List<ConstraintSnapshot> snapshots = Collections.synchronizedList(new ArrayList<>());
    private LocalDateTime lastLogTime = LocalDateTime.now();
    private AtomicInteger evaluationCount = new AtomicInteger(0);
    
    private ConstraintMonitor() {}
    
    public static ConstraintMonitor getInstance() {
        return INSTANCE;
    }
    
    /**
     * Records a hard constraint violation
     */
    public void recordHardViolation(String constraintName, int penaltyAmount) {
        hardViolationCounts.computeIfAbsent(constraintName, k -> new AtomicInteger(0)).incrementAndGet();
        hardScoreContributions.computeIfAbsent(constraintName, k -> new AtomicLong(0)).addAndGet(penaltyAmount);
        checkAndLog();
    }
    
    /**
     * Records a soft constraint violation
     */
    public void recordSoftViolation(String constraintName, int penaltyAmount) {
        softViolationCounts.computeIfAbsent(constraintName, k -> new AtomicInteger(0)).incrementAndGet();
        softScoreContributions.computeIfAbsent(constraintName, k -> new AtomicLong(0)).addAndGet(penaltyAmount);
        checkAndLog();
    }
    
    /**
     * Records when a constraint is satisfied (positive contribution)
     */
    public void recordConstraintSatisfied(String constraintName, boolean isHard, int rewardAmount) {
        if (isHard) {
            hardScoreContributions.computeIfAbsent(constraintName, k -> new AtomicLong(0)).addAndGet(-rewardAmount);
        } else {
            softScoreContributions.computeIfAbsent(constraintName, k -> new AtomicLong(0)).addAndGet(-rewardAmount);
        }
        checkAndLog();
    }
    
    /**
     * Called periodically to reset counters and take snapshot
     */
    public void newEvaluation() {
        int count = evaluationCount.incrementAndGet();
        
        // Take snapshot every 100 evaluations or if significant time has passed
        if (count % 100 == 0 || LocalDateTime.now().minusMinutes(1).isAfter(lastLogTime)) {
            takeSnapshot();
            if (count % 500 == 0) { // Detailed log every 500 evaluations
                logDetailedStatus();
            }
        }
    }
    
    /**
     * Gets current constraint violation summary
     */
    public Map<String, ConstraintStats> getCurrentStats() {
        Map<String, ConstraintStats> stats = new HashMap<>();
        
        Set<String> allConstraints = new HashSet<>();
        allConstraints.addAll(hardViolationCounts.keySet());
        allConstraints.addAll(softViolationCounts.keySet());
        allConstraints.addAll(hardScoreContributions.keySet());
        allConstraints.addAll(softScoreContributions.keySet());
        
        for (String constraint : allConstraints) {
            int hardViolations = hardViolationCounts.getOrDefault(constraint, new AtomicInteger(0)).get();
            int softViolations = softViolationCounts.getOrDefault(constraint, new AtomicInteger(0)).get();
            long hardScore = hardScoreContributions.getOrDefault(constraint, new AtomicLong(0)).get();
            long softScore = softScoreContributions.getOrDefault(constraint, new AtomicLong(0)).get();
            
            stats.put(constraint, new ConstraintStats(constraint, hardViolations, softViolations, hardScore, softScore));
        }
        
        return stats;
    }
    
    /**
     * Gets trend analysis over time
     */
    public List<ConstraintSnapshot> getSnapshots() {
        return new ArrayList<>(snapshots);
    }
    
    /**
     * Resets all monitoring data
     */
    public void reset() {
        hardViolationCounts.clear();
        softViolationCounts.clear();
        hardScoreContributions.clear();
        softScoreContributions.clear();
        snapshots.clear();
        evaluationCount.set(0);
        lastLogTime = LocalDateTime.now();
        LOGGER.info("Constraint monitor reset");
    }
    
    private void checkAndLog() {
        int count = evaluationCount.get();
        if (count > 0 && count % 1000 == 0) {
            logQuickStatus();
        }
    }
    
    private void takeSnapshot() {
        Map<String, ConstraintStats> currentStats = getCurrentStats();
        ConstraintSnapshot snapshot = new ConstraintSnapshot(LocalDateTime.now(), currentStats);
        snapshots.add(snapshot);
        
        // Keep only last 100 snapshots to prevent memory issues
        if (snapshots.size() > 100) {
            snapshots.remove(0);
        }
        
        lastLogTime = LocalDateTime.now();
    }
    
    private void logQuickStatus() {
        Map<String, ConstraintStats> stats = getCurrentStats();
        
        int totalHardViolations = stats.values().stream().mapToInt(ConstraintStats::getHardViolations).sum();
        int totalSoftViolations = stats.values().stream().mapToInt(ConstraintStats::getSoftViolations).sum();
        long totalHardScore = stats.values().stream().mapToLong(ConstraintStats::getHardScore).sum();
        long totalSoftScore = stats.values().stream().mapToLong(ConstraintStats::getSoftScore).sum();
        
        LOGGER.info(String.format("Evaluation %d | Hard: %d violations (score: %d) | Soft: %d violations (score: %d)",
                evaluationCount.get(), totalHardViolations, totalHardScore, totalSoftViolations, totalSoftScore));
    }
    
    private void logDetailedStatus() {
        LOGGER.info("=== DETAILED CONSTRAINT ANALYSIS ===");
        LOGGER.info("Timestamp: " + LocalDateTime.now().format(TIMESTAMP_FORMAT));
        LOGGER.info("Total Evaluations: " + evaluationCount.get());
        
        Map<String, ConstraintStats> stats = getCurrentStats();
        
        // Sort by total impact (hard violations first, then by score impact)
        List<ConstraintStats> sortedStats = new ArrayList<>(stats.values());
        sortedStats.sort((a, b) -> {
            if (a.getHardViolations() != b.getHardViolations()) {
                return Integer.compare(b.getHardViolations(), a.getHardViolations());
            }
            return Long.compare(Math.abs(b.getTotalScore()), Math.abs(a.getTotalScore()));
        });
        
        LOGGER.info("\n--- TOP CONSTRAINT VIOLATIONS ---");
        sortedStats.stream()
                .filter(s -> s.getHardViolations() > 0 || s.getSoftViolations() > 0)
                .limit(10)
                .forEach(stat -> {
                    LOGGER.info(String.format("%-40s | Hard: %4d (%6d) | Soft: %4d (%6d)",
                            truncate(stat.getConstraintName(), 40),
                            stat.getHardViolations(), stat.getHardScore(),
                            stat.getSoftViolations(), stat.getSoftScore()));
                });
        
        // Summary statistics
        int totalHardViolations = sortedStats.stream().mapToInt(ConstraintStats::getHardViolations).sum();
        int totalSoftViolations = sortedStats.stream().mapToInt(ConstraintStats::getSoftViolations).sum();
        long totalHardScore = sortedStats.stream().mapToLong(ConstraintStats::getHardScore).sum();
        long totalSoftScore = sortedStats.stream().mapToLong(ConstraintStats::getSoftScore).sum();
        
        LOGGER.info("\n--- SUMMARY ---");
        LOGGER.info(String.format("Total Hard Violations: %d (Score: %d)", totalHardViolations, totalHardScore));
        LOGGER.info(String.format("Total Soft Violations: %d (Score: %d)", totalSoftViolations, totalSoftScore));
        LOGGER.info(String.format("Overall Score: %s", HardSoftScore.of((int)totalHardScore, (int)totalSoftScore)));
        LOGGER.info("=== END CONSTRAINT ANALYSIS ===");
    }
    
    private String truncate(String str, int maxLength) {
        return str.length() <= maxLength ? str : str.substring(0, maxLength - 3) + "...";
    }
    
    // Data classes for tracking
    public static class ConstraintStats {
        private final String constraintName;
        private final int hardViolations;
        private final int softViolations;
        private final long hardScore;
        private final long softScore;
        
        public ConstraintStats(String constraintName, int hardViolations, int softViolations, long hardScore, long softScore) {
            this.constraintName = constraintName;
            this.hardViolations = hardViolations;
            this.softViolations = softViolations;
            this.hardScore = hardScore;
            this.softScore = softScore;
        }
        
        public String getConstraintName() { return constraintName; }
        public int getHardViolations() { return hardViolations; }
        public int getSoftViolations() { return softViolations; }
        public long getHardScore() { return hardScore; }
        public long getSoftScore() { return softScore; }
        public long getTotalScore() { return hardScore + softScore; }
        public int getTotalViolations() { return hardViolations + softViolations; }
    }
    
    public static class ConstraintSnapshot {
        private final LocalDateTime timestamp;
        private final Map<String, ConstraintStats> stats;
        
        public ConstraintSnapshot(LocalDateTime timestamp, Map<String, ConstraintStats> stats) {
            this.timestamp = timestamp;
            this.stats = new HashMap<>(stats);
        }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public Map<String, ConstraintStats> getStats() { return stats; }
    }
} 