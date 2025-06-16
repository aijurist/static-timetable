package org.timetable.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class SolverProperties {
    private static final Logger logger = LoggerFactory.getLogger(SolverProperties.class);
    private static final Properties properties = new Properties();
    
    static {
        loadProperties();
    }
    
    private static void loadProperties() {
        try (InputStream input = SolverProperties.class.getClassLoader()
                .getResourceAsStream("solver.properties")) {
            if (input != null) {
                properties.load(input);
                logger.info("Loaded solver configuration from solver.properties");
            } else {
                logger.info("No solver.properties found, using default values");
            }
        } catch (IOException e) {
            logger.warn("Failed to load solver.properties, using defaults", e);
        }
    }
    
    public static int getSolveMinutes() {
        return getIntProperty("solver.minutes", 30);
    }
    
    public static int getThreadCount() {
        int defaultThreads = Math.max(2, Math.min(8, 
                (int) (Runtime.getRuntime().availableProcessors() * 0.75)));
        return getIntProperty("solver.threads", defaultThreads);
    }
    
    public static int getConstructionHeuristicSeconds() {
        return getIntProperty("solver.construction.seconds", 300);
    }
    
    public static int getLateAcceptanceSize() {
        return getIntProperty("solver.late.acceptance.size", 400);
    }
    
    public static int getEntityTabuSize() {
        return getIntProperty("solver.entity.tabu.size", 7);
    }
    
    public static int getAcceptedCountLimit() {
        return getIntProperty("solver.accepted.count.limit", 4);
    }
    
    public static boolean isProgressLoggingEnabled() {
        return getBooleanProperty("solver.progress.logging", true);
    }
    
    public static int getProgressLoggingIntervalSeconds() {
        return getIntProperty("solver.progress.interval.seconds", 30);
    }
    
    private static int getIntProperty(String key, int defaultValue) {
        // Check system property first
        String systemValue = System.getProperty(key);
        if (systemValue != null) {
            try {
                return Integer.parseInt(systemValue);
            } catch (NumberFormatException e) {
                logger.warn("Invalid system property value for {}: {}", key, systemValue);
            }
        }
        
        // Check environment variable
        String envKey = key.replace('.', '_').toUpperCase();
        String envValue = System.getenv(envKey);
        if (envValue != null) {
            try {
                return Integer.parseInt(envValue);
            } catch (NumberFormatException e) {
                logger.warn("Invalid environment variable value for {}: {}", envKey, envValue);
            }
        }
        
        // Check properties file
        String propValue = properties.getProperty(key);
        if (propValue != null) {
            try {
                return Integer.parseInt(propValue);
            } catch (NumberFormatException e) {
                logger.warn("Invalid property value for {}: {}", key, propValue);
            }
        }
        
        return defaultValue;
    }
    
    private static boolean getBooleanProperty(String key, boolean defaultValue) {
        // Check system property first
        String systemValue = System.getProperty(key);
        if (systemValue != null) {
            return Boolean.parseBoolean(systemValue);
        }
        
        // Check environment variable
        String envKey = key.replace('.', '_').toUpperCase();
        String envValue = System.getenv(envKey);
        if (envValue != null) {
            return Boolean.parseBoolean(envValue);
        }
        
        // Check properties file
        String propValue = properties.getProperty(key);
        if (propValue != null) {
            return Boolean.parseBoolean(propValue);
        }
        
        return defaultValue;
    }
    
    public static void logCurrentConfiguration() {
        logger.info("--- Solver Configuration ---");
        logger.info("Solve minutes: {}", getSolveMinutes());
        logger.info("Thread count: {}", getThreadCount());
        logger.info("Construction heuristic seconds: {}", getConstructionHeuristicSeconds());
        logger.info("Late acceptance size: {}", getLateAcceptanceSize());
        logger.info("Entity tabu size: {}", getEntityTabuSize());
        logger.info("Accepted count limit: {}", getAcceptedCountLimit());
        logger.info("Progress logging enabled: {}", isProgressLoggingEnabled());
        if (isProgressLoggingEnabled()) {
            logger.info("Progress logging interval: {} seconds", getProgressLoggingIntervalSeconds());
        }
    }
} 