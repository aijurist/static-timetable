package org.timetable.solver;

import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for analyzing constraint monitoring data and generating reports
 */
public class ConstraintAnalysisUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConstraintAnalysisUtil.class);

    /**
     * Generates a comprehensive constraint analysis report
     */
    public static void generateDetailedReport() {
        ConstraintMonitor monitor = ConstraintMonitor.getInstance();
        Map<String, ConstraintMonitor.ConstraintStats> stats = monitor.getCurrentStats();
        
        LOGGER.info("=== COMPREHENSIVE CONSTRAINT ANALYSIS ===");
        
        if (stats.isEmpty()) {
            LOGGER.info("No constraint data available");
            return;
        }
        
        // Overview statistics
        int totalHardViolations = stats.values().stream().mapToInt(ConstraintMonitor.ConstraintStats::getHardViolations).sum();
        int totalSoftViolations = stats.values().stream().mapToInt(ConstraintMonitor.ConstraintStats::getSoftViolations).sum();
        long totalHardScore = stats.values().stream().mapToLong(ConstraintMonitor.ConstraintStats::getHardScore).sum();
        long totalSoftScore = stats.values().stream().mapToLong(ConstraintMonitor.ConstraintStats::getSoftScore).sum();
        
        LOGGER.info("OVERVIEW:");
        LOGGER.info("  Total Hard Violations: {} (Score: {})", totalHardViolations, totalHardScore);
        LOGGER.info("  Total Soft Violations: {} (Score: {})", totalSoftViolations, totalSoftScore);
        LOGGER.info("  Active Constraints: {}", stats.size());
        
        // Hard constraint violations (critical issues)
        List<ConstraintMonitor.ConstraintStats> hardViolations = stats.values().stream()
                .filter(s -> s.getHardViolations() > 0)
                .sorted((a, b) -> Integer.compare(b.getHardViolations(), a.getHardViolations()))
                .collect(Collectors.toList());
        
        if (!hardViolations.isEmpty()) {
            LOGGER.info("\nHARD CONSTRAINT VIOLATIONS (Critical):");
            hardViolations.forEach(stat -> 
                LOGGER.info("  {} | Violations: {} | Score: {}", 
                    stat.getConstraintName(), stat.getHardViolations(), stat.getHardScore())
            );
        } else {
            LOGGER.info("\n✓ No hard constraint violations - Solution is FEASIBLE!");
        }
        
        // Soft constraint violations (optimization opportunities)
        List<ConstraintMonitor.ConstraintStats> softViolations = stats.values().stream()
                .filter(s -> s.getSoftViolations() > 0)
                .sorted((a, b) -> Long.compare(b.getSoftScore(), a.getSoftScore()))
                .limit(10)
                .collect(Collectors.toList());
        
        if (!softViolations.isEmpty()) {
            LOGGER.info("\nTOP SOFT CONSTRAINT VIOLATIONS (Optimization):");
            softViolations.forEach(stat -> 
                LOGGER.info("  {} | Violations: {} | Score: {}", 
                    stat.getConstraintName(), stat.getSoftViolations(), stat.getSoftScore())
            );
        }
        
        // Lab type analysis
        analyzeLaboratoryConstraints(stats);
        
        LOGGER.info("=== END CONSTRAINT ANALYSIS ===");
    }
    
    /**
     * Analyzes laboratory-specific constraints
     */
    private static void analyzeLaboratoryConstraints(Map<String, ConstraintMonitor.ConstraintStats> stats) {
        List<ConstraintMonitor.ConstraintStats> labConstraints = stats.values().stream()
                .filter(s -> s.getConstraintName().toLowerCase().contains("lab"))
                .collect(Collectors.toList());
        
        if (!labConstraints.isEmpty()) {
            LOGGER.info("\nLABORATORY CONSTRAINT ANALYSIS:");
            labConstraints.forEach(stat -> {
                if (stat.getTotalViolations() > 0) {
                    LOGGER.info("  {} | Hard: {} | Soft: {}", 
                        stat.getConstraintName(), stat.getHardViolations(), stat.getSoftViolations());
                }
            });
            
            // Priority analysis for lab_type constraints
            stats.values().stream()
                    .filter(s -> s.getConstraintName().contains("lab type"))
                    .forEach(stat -> {
                        if (stat.getHardViolations() > 0) {
                            LOGGER.info("  🎯 PRIORITY LAB ISSUE: {} violations of '{}'", 
                                stat.getHardViolations(), stat.getConstraintName());
                        }
                    });
        }
    }
    
    /**
     * Shows constraint satisfaction trends over time
     */
    public static void showTrends() {
        ConstraintMonitor monitor = ConstraintMonitor.getInstance();
        List<ConstraintMonitor.ConstraintSnapshot> snapshots = monitor.getSnapshots();
        
        if (snapshots.size() < 2) {
            LOGGER.info("Not enough data for trend analysis");
            return;
        }
        
        LOGGER.info("=== CONSTRAINT SATISFACTION TRENDS ===");
        
        // Compare first and last snapshots
        ConstraintMonitor.ConstraintSnapshot first = snapshots.get(0);
        ConstraintMonitor.ConstraintSnapshot last = snapshots.get(snapshots.size() - 1);
        
        Map<String, ConstraintMonitor.ConstraintStats> firstStats = first.getStats();
        Map<String, ConstraintMonitor.ConstraintStats> lastStats = last.getStats();
        
        Set<String> allConstraints = new HashSet<>();
        allConstraints.addAll(firstStats.keySet());
        allConstraints.addAll(lastStats.keySet());
        
        LOGGER.info("Trend analysis from {} to {}:", 
            first.getTimestamp().toString(), last.getTimestamp().toString());
        
        for (String constraint : allConstraints) {
            ConstraintMonitor.ConstraintStats firstStat = firstStats.get(constraint);
            ConstraintMonitor.ConstraintStats lastStat = lastStats.get(constraint);
            
            int firstViolations = firstStat != null ? firstStat.getTotalViolations() : 0;
            int lastViolations = lastStat != null ? lastStat.getTotalViolations() : 0;
            
            if (firstViolations != lastViolations) {
                String trend = lastViolations < firstViolations ? "↓ IMPROVED" : "↑ WORSENED";
                LOGGER.info("  {} | {} → {} {}", 
                    constraint, firstViolations, lastViolations, trend);
            }
        }
        
        LOGGER.info("=== END TREND ANALYSIS ===");
    }
    
    /**
     * Quick status check for monitoring system
     */
    public static void quickStatus() {
        ConstraintMonitor monitor = ConstraintMonitor.getInstance();
        Map<String, ConstraintMonitor.ConstraintStats> stats = monitor.getCurrentStats();
        
        int hardViolations = stats.values().stream().mapToInt(ConstraintMonitor.ConstraintStats::getHardViolations).sum();
        int softViolations = stats.values().stream().mapToInt(ConstraintMonitor.ConstraintStats::getSoftViolations).sum();
        
        LOGGER.info("📊 Quick Status: {} hard violations, {} soft violations, {} active constraints",
                hardViolations, softViolations, stats.size());
    }
} 