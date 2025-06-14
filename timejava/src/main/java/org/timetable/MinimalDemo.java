package org.timetable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinimalDemo {
    private static final Logger logger = LoggerFactory.getLogger(MinimalDemo.class);

    public static void main(String[] args) {
        logger.info("Starting Timetable Demo");
        logger.info("This is a minimal demo to test if the project compiles and runs");
        logger.info("OptaPlanner version: {}", getOptaPlannerVersion());
        logger.info("Java version: {}", System.getProperty("java.version"));
        logger.info("OS: {}", System.getProperty("os.name"));
        logger.info("Demo completed successfully!");
    }
    
    private static String getOptaPlannerVersion() {
        try {
            // Try to load a class from OptaPlanner to verify it's available
            Class.forName("org.optaplanner.core.api.solver.SolverFactory");
            return "Available (exact version unknown)";
        } catch (ClassNotFoundException e) {
            return "Not available";
        }
    }
} 