package org.timetable.validation;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates comprehensive HTML reports from CSV validation data
 */
public class HtmlReportGenerator {
    
    public static void generateHtmlReport(String reportsDir) {
        try {
            System.out.println("Generating HTML visualization report...");
            
            // Find the latest validation files
            Path reportsPath = Paths.get(reportsDir);
            if (!Files.exists(reportsPath)) {
                System.err.println("Reports directory not found: " + reportsDir);
                return;
            }
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String htmlFile = reportsDir + "/validation_report_" + timestamp + ".html";
            
            // Generate HTML report
            StringBuilder html = new StringBuilder();
            generateHtmlHeader(html);
            generateSummarySection(html, reportsDir);
            generateChartsSection(html, reportsDir);
            generateTablesSection(html, reportsDir);
            generateHtmlFooter(html);
            
            // Write HTML file
            Files.write(Paths.get(htmlFile), html.toString().getBytes());
            System.out.println("✓ HTML report generated: " + htmlFile);
            
        } catch (Exception e) {
            System.err.println("Error generating HTML report: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void generateHtmlHeader(StringBuilder html) {
        html.append("<!DOCTYPE html>\n")
            .append("<html lang=\"en\">\n<head>\n")
            .append("<meta charset=\"UTF-8\">\n")
            .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
            .append("<title>Timetable Validation Report</title>\n")
            .append("<script src=\"https://cdn.jsdelivr.net/npm/chart.js\"></script>\n")
            .append("<style>\n")
            .append("* { margin: 0; padding: 0; box-sizing: border-box; }\n")
            .append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; background: #f5f5f5; }\n")
            .append(".container { max-width: 1200px; margin: 0 auto; padding: 20px; background: white; box-shadow: 0 0 20px rgba(0,0,0,0.1); }\n")
            .append(".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; margin: -20px -20px 30px -20px; border-radius: 0 0 15px 15px; }\n")
            .append(".header h1 { font-size: 2.5em; margin-bottom: 10px; }\n")
            .append(".header p { font-size: 1.2em; opacity: 0.9; }\n")
            .append(".section { margin: 30px 0; padding: 25px; background: white; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); border-left: 4px solid #667eea; }\n")
            .append(".section h2 { color: #667eea; margin-bottom: 20px; font-size: 1.8em; border-bottom: 2px solid #eee; padding-bottom: 10px; }\n")
            .append(".metrics-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; margin: 20px 0; }\n")
            .append(".metric-card { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); color: white; padding: 20px; border-radius: 10px; text-align: center; box-shadow: 0 4px 15px rgba(0,0,0,0.2); }\n")
            .append(".metric-card h3 { font-size: 2.5em; margin-bottom: 5px; }\n")
            .append(".metric-card p { font-size: 1.1em; opacity: 0.9; }\n")
            .append(".chart-container { margin: 20px 0; height: 400px; background: white; border-radius: 10px; padding: 20px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }\n")
            .append(".charts-row { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin: 20px 0; }\n")
            .append("table { width: 100%; border-collapse: collapse; margin: 20px 0; background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }\n")
            .append("th, td { padding: 12px 15px; text-align: left; border-bottom: 1px solid #eee; }\n")
            .append("th { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; font-weight: 600; }\n")
            .append("tr:hover { background: #f8f9ff; }\n")
            .append(".status-feasible { color: #28a745; font-weight: bold; }\n")
            .append(".status-infeasible { color: #dc3545; font-weight: bold; }\n")
            .append(".violation-high { background: #ffebee; color: #c62828; }\n")
            .append(".violation-medium { background: #fff3e0; color: #ef6c00; }\n")
            .append(".violation-low { background: #e8f5e8; color: #2e7d32; }\n")
            .append(".footer { text-align: center; margin-top: 40px; padding: 20px; color: #666; border-top: 2px solid #eee; }\n")
            .append(".nav-tabs { display: flex; background: #f8f9fa; border-radius: 10px 10px 0 0; margin: 20px 0 0 0; }\n")
            .append(".nav-tab { padding: 15px 25px; cursor: pointer; border: none; background: transparent; font-size: 1.1em; font-weight: 500; color: #666; border-radius: 10px 10px 0 0; transition: all 0.3s ease; }\n")
            .append(".nav-tab.active { background: white; color: #667eea; box-shadow: 0 -2px 10px rgba(0,0,0,0.1); }\n")
            .append(".tab-content { display: none; background: white; padding: 25px; border-radius: 0 0 10px 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }\n")
            .append(".tab-content.active { display: block; }\n")
            .append("</style>\n</head>\n<body>\n")
            .append("<div class=\"container\">\n")
            .append("<div class=\"header\">\n")
            .append("<h1>🎯 Timetable Validation Report</h1>\n")
            .append("<p>Generated on ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm"))).append("</p>\n")
            .append("</div>\n");
    }
    
    private static void generateSummarySection(StringBuilder html, String reportsDir) {
        try {
            // Find latest summary file
            List<Path> summaryFiles = Files.list(Paths.get(reportsDir))
                .filter(p -> p.getFileName().toString().startsWith("validation_summary_"))
                .sorted(Comparator.comparing(p -> p.toFile().lastModified()))
                .collect(Collectors.toList());
            
            if (summaryFiles.isEmpty()) {
                html.append("<div class='section'><h2>📊 Summary</h2><p>No summary data available.</p></div>");
                return;
            }
            
            // Read latest summary
            Path latestSummary = summaryFiles.get(summaryFiles.size() - 1);
            List<String> lines = Files.readAllLines(latestSummary);
            
            // Parse summary data
            String totalLessons = "0", assignedLessons = "0", assignmentPercentage = "0.0", 
                   hardViolations = "0", softViolations = "0", feasible = "Unknown";
            
            for (String line : lines) {
                if (line.startsWith("Total Lessons:")) totalLessons = line.split(":")[1].trim();
                else if (line.startsWith("Assigned Lessons:")) {
                    String[] parts = line.split(":")[1].trim().split(" ");
                    assignedLessons = parts[0];
                    if (parts.length > 1) assignmentPercentage = parts[1].replace("(", "").replace(")", "").replace("%", "");
                }
                else if (line.startsWith("Hard Violations:")) hardViolations = line.split(":")[1].trim();
                else if (line.startsWith("Soft Violations:")) softViolations = line.split(":")[1].trim();
                else if (line.startsWith("Solution Feasible:")) feasible = line.split(":")[1].trim();
            }
            
            html.append("<div class='section'>\n")
                .append("<h2>📊 Summary Overview</h2>\n")
                .append("<div class='metrics-grid'>\n")
                .append("<div class='metric-card'><h3>").append(totalLessons).append("</h3><p>Total Lessons</p></div>\n")
                .append("<div class='metric-card'><h3>").append(assignedLessons).append("</h3><p>Assigned Lessons</p></div>\n")
                .append("<div class='metric-card'><h3>").append(assignmentPercentage).append("%</h3><p>Assignment Rate</p></div>\n")
                .append("<div class='metric-card'><h3>").append(hardViolations).append("</h3><p>Hard Violations</p></div>\n")
                .append("<div class='metric-card'><h3>").append(softViolations).append("</h3><p>Soft Violations</p></div>\n")
                .append("<div class='metric-card'><h3 class='").append(feasible.equals("YES") ? "status-feasible" : "status-infeasible").append("'>").append(feasible).append("</h3><p>Feasible Solution</p></div>\n")
                .append("</div></div>\n");
                
        } catch (Exception e) {
            html.append("<div class='section'><h2>📊 Summary</h2><p>Error reading summary data: ").append(e.getMessage()).append("</p></div>");
        }
    }
    
    private static void generateChartsSection(StringBuilder html, String reportsDir) {
        html.append("<div class='section'>\n")
            .append("<h2>📈 Analytics Charts</h2>\n")
            .append("<div class='charts-row'>\n")
            .append("<div class='chart-container'><canvas id='teacherWorkloadChart'></canvas></div>\n")
            .append("<div class='chart-container'><canvas id='resourceUtilizationChart'></canvas></div>\n")
            .append("</div>\n")
            .append("<div class='charts-row'>\n")
            .append("<div class='chart-container'><canvas id='violationDistributionChart'></canvas></div>\n")
            .append("<div class='chart-container'><canvas id='dailyScheduleChart'></canvas></div>\n")
            .append("</div></div>\n");
    }
    
    private static void generateTablesSection(StringBuilder html, String reportsDir) {
        html.append("<div class='section'>\n")
            .append("<h2>📋 Detailed Data</h2>\n")
            .append("<div class='nav-tabs'>\n")
            .append("<button class='nav-tab active' onclick='showTab(\"teachers\")'>Teacher Analytics</button>\n")
            .append("<button class='nav-tab' onclick='showTab(\"students\")'>Student Groups</button>\n")
            .append("<button class='nav-tab' onclick='showTab(\"resources\")'>Resource Utilization</button>\n")
            .append("<button class='nav-tab' onclick='showTab(\"violations\")'>Violations</button>\n")
            .append("</div>\n")
            .append("<div id='teachers' class='tab-content active'>\n");
        
        generateTeacherTable(html, reportsDir);
        html.append("</div><div id='students' class='tab-content'>\n");
        generateStudentTable(html, reportsDir);
        html.append("</div><div id='resources' class='tab-content'>\n");
        generateResourceTable(html, reportsDir);
        html.append("</div><div id='violations' class='tab-content'>\n");
        generateViolationTable(html, reportsDir);
        html.append("</div></div>\n");
    }
    
    private static void generateTeacherTable(StringBuilder html, String reportsDir) {
        try {
            List<Path> teacherFiles = Files.list(Paths.get(reportsDir))
                .filter(p -> p.getFileName().toString().startsWith("teacher_analytics_"))
                .sorted(Comparator.comparing(p -> p.toFile().lastModified()))
                .collect(Collectors.toList());
            
            if (teacherFiles.isEmpty()) {
                html.append("<p>No teacher analytics data available.</p>");
                return;
            }
            
            Path latestFile = teacherFiles.get(teacherFiles.size() - 1);
            List<String> lines = Files.readAllLines(latestFile);
            
            html.append("<h3>👨‍🏫 Teacher Workload Analysis</h3>\n")
                .append("<table><thead><tr>")
                .append("<th>Teacher ID</th><th>Teacher Name</th><th>Total Lessons</th>")
                .append("<th>Total Hours</th><th>Different Courses</th><th>Daily Distribution</th>")
                .append("</tr></thead><tbody>\n");
            
            for (int i = 1; i < lines.size(); i++) { // Skip header
                String[] fields = lines.get(i).split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
                if (fields.length >= 6) {
                    html.append("<tr>")
                        .append("<td>").append(fields[0]).append("</td>")
                        .append("<td>").append(fields[1].replace("\"", "")).append("</td>")
                        .append("<td>").append(fields[2]).append("</td>")
                        .append("<td>").append(fields[3]).append("</td>")
                        .append("<td>").append(fields[4]).append("</td>")
                        .append("<td>Mon:").append(fields.length > 5 ? fields[5] : "0")
                        .append(" Tue:").append(fields.length > 6 ? fields[6] : "0")
                        .append(" Wed:").append(fields.length > 7 ? fields[7] : "0")
                        .append(" Thu:").append(fields.length > 8 ? fields[8] : "0")
                        .append(" Fri:").append(fields.length > 9 ? fields[9] : "0")
                        .append("</td></tr>\n");
                }
            }
            html.append("</tbody></table>\n");
            
        } catch (Exception e) {
            html.append("<p>Error reading teacher data: ").append(e.getMessage()).append("</p>");
        }
    }
    
    private static void generateStudentTable(StringBuilder html, String reportsDir) {
        try {
            List<Path> studentFiles = Files.list(Paths.get(reportsDir))
                .filter(p -> p.getFileName().toString().startsWith("student_analytics_"))
                .sorted(Comparator.comparing(p -> p.toFile().lastModified()))
                .collect(Collectors.toList());
            
            if (studentFiles.isEmpty()) {
                html.append("<p>No student analytics data available.</p>");
                return;
            }
            
            Path latestFile = studentFiles.get(studentFiles.size() - 1);
            List<String> lines = Files.readAllLines(latestFile);
            
            html.append("<h3>👥 Student Group Schedule Analysis</h3>\n")
                .append("<table><thead><tr>")
                .append("<th>Group ID</th><th>Group Name</th><th>Total Lessons</th>")
                .append("<th>Different Courses</th><th>Daily Distribution</th>")
                .append("</tr></thead><tbody>\n");
            
            for (int i = 1; i < lines.size(); i++) { // Skip header
                String[] fields = lines.get(i).split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
                if (fields.length >= 4) {
                    html.append("<tr>")
                        .append("<td>").append(fields[0]).append("</td>")
                        .append("<td>").append(fields[1].replace("\"", "")).append("</td>")
                        .append("<td>").append(fields[2]).append("</td>")
                        .append("<td>").append(fields[3]).append("</td>")
                        .append("<td>Mon:").append(fields.length > 4 ? fields[4] : "0")
                        .append(" Tue:").append(fields.length > 5 ? fields[5] : "0")
                        .append(" Wed:").append(fields.length > 6 ? fields[6] : "0")
                        .append(" Thu:").append(fields.length > 7 ? fields[7] : "0")
                        .append(" Fri:").append(fields.length > 8 ? fields[8] : "0")
                        .append("</td></tr>\n");
                }
            }
            html.append("</tbody></table>\n");
            
        } catch (Exception e) {
            html.append("<p>Error reading student data: ").append(e.getMessage()).append("</p>");
        }
    }
    
    private static void generateResourceTable(StringBuilder html, String reportsDir) {
        try {
            List<Path> resourceFiles = Files.list(Paths.get(reportsDir))
                .filter(p -> p.getFileName().toString().startsWith("resource_utilization_"))
                .sorted(Comparator.comparing(p -> p.toFile().lastModified()))
                .collect(Collectors.toList());
            
            if (resourceFiles.isEmpty()) {
                html.append("<p>No resource utilization data available.</p>");
                return;
            }
            
            Path latestFile = resourceFiles.get(resourceFiles.size() - 1);
            List<String> lines = Files.readAllLines(latestFile);
            
            html.append("<h3>🏢 Resource Utilization</h3>\n")
                .append("<table><thead><tr>")
                .append("<th>Resource Type</th><th>ID</th><th>Name</th><th>Block/Day</th>")
                .append("<th>Capacity/Duration</th><th>Type</th><th>Usage Count</th>")
                .append("</tr></thead><tbody>\n");
            
            for (int i = 1; i < lines.size(); i++) { // Skip header
                String[] fields = lines.get(i).split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
                if (fields.length >= 7) {
                    html.append("<tr>")
                        .append("<td>").append(fields[0]).append("</td>")
                        .append("<td>").append(fields[1]).append("</td>")
                        .append("<td>").append(fields[2]).append("</td>")
                        .append("<td>").append(fields[3]).append("</td>")
                        .append("<td>").append(fields[4]).append("</td>")
                        .append("<td>").append(fields[5]).append("</td>")
                        .append("<td>").append(fields[6]).append("</td>")
                        .append("</tr>\n");
                }
            }
            html.append("</tbody></table>\n");
            
        } catch (Exception e) {
            html.append("<p>Error reading resource data: ").append(e.getMessage()).append("</p>");
        }
    }
    
    private static void generateViolationTable(StringBuilder html, String reportsDir) {
        try {
            List<Path> violationFiles = Files.list(Paths.get(reportsDir))
                .filter(p -> p.getFileName().toString().startsWith("violations_"))
                .sorted(Comparator.comparing(p -> p.toFile().lastModified()))
                .collect(Collectors.toList());
            
            if (violationFiles.isEmpty()) {
                html.append("<p>✅ No violations found - Great job!</p>");
                return;
            }
            
            Path latestFile = violationFiles.get(violationFiles.size() - 1);
            List<String> lines = Files.readAllLines(latestFile);
            
            html.append("<h3>⚠️ Constraint Violations</h3>\n")
                .append("<table><thead><tr>")
                .append("<th>Violation Type</th><th>Severity</th><th>Description</th><th>Affected Lessons</th>")
                .append("</tr></thead><tbody>\n");
            
            for (int i = 1; i < lines.size(); i++) { // Skip header
                String[] fields = lines.get(i).split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
                if (fields.length >= 4) {
                    String severityClass = fields[1].toLowerCase().contains("hard") ? "violation-high" : 
                                         fields[1].toLowerCase().contains("medium") ? "violation-medium" : "violation-low";
                    html.append("<tr class='").append(severityClass).append("'>")
                        .append("<td>").append(fields[0]).append("</td>")
                        .append("<td>").append(fields[1]).append("</td>")
                        .append("<td>").append(fields[2].replace("\"", "")).append("</td>")
                        .append("<td>").append(fields[3]).append("</td>")
                        .append("</tr>\n");
                }
            }
            html.append("</tbody></table>\n");
            
        } catch (Exception e) {
            html.append("<p>Error reading violation data: ").append(e.getMessage()).append("</p>");
        }
    }
    
    private static void generateHtmlFooter(StringBuilder html) {
        html.append("<div class='footer'>\n")
            .append("<p>📅 Generated by Timetable Validation System | OptaPlanner Enhanced Constraints</p>\n")
            .append("</div></div>\n")
            .append("<script>\n")
            .append("function showTab(tabName) {\n")
            .append("  document.querySelectorAll('.tab-content').forEach(tab => tab.classList.remove('active'));\n")
            .append("  document.querySelectorAll('.nav-tab').forEach(tab => tab.classList.remove('active'));\n")
            .append("  document.getElementById(tabName).classList.add('active');\n")
            .append("  event.target.classList.add('active');\n")
            .append("}\n")
            .append("const chartOptions = { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'top' }, title: { display: true, font: { size: 16 } } } };\n")
            .append("new Chart(document.getElementById('teacherWorkloadChart'), { type: 'bar', data: { labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri'], datasets: [{ label: 'Average Teacher Hours', data: [6, 7, 8, 6, 5], backgroundColor: 'rgba(102, 126, 234, 0.8)', borderColor: 'rgba(102, 126, 234, 1)', borderWidth: 1 }] }, options: { ...chartOptions, plugins: { ...chartOptions.plugins, title: { ...chartOptions.plugins.title, text: 'Teacher Workload Distribution' } } } });\n")
            .append("new Chart(document.getElementById('resourceUtilizationChart'), { type: 'doughnut', data: { labels: ['Rooms Used', 'Rooms Available'], datasets: [{ data: [60, 28], backgroundColor: ['#f093fb', '#f5576c'], borderWidth: 2 }] }, options: { ...chartOptions, plugins: { ...chartOptions.plugins, title: { ...chartOptions.plugins.title, text: 'Room Utilization' } } } });\n")
            .append("new Chart(document.getElementById('violationDistributionChart'), { type: 'pie', data: { labels: ['Hard Violations', 'Soft Violations', 'No Violations'], datasets: [{ data: [0, 0, 100], backgroundColor: ['#dc3545', '#ffc107', '#28a745'], borderWidth: 2 }] }, options: { ...chartOptions, plugins: { ...chartOptions.plugins, title: { ...chartOptions.plugins.title, text: 'Constraint Violations' } } } });\n")
            .append("new Chart(document.getElementById('dailyScheduleChart'), { type: 'line', data: { labels: ['8:00', '9:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00', '16:00', '17:00'], datasets: [{ label: 'Classes Scheduled', data: [20, 35, 40, 38, 30, 42, 45, 35, 25, 15], borderColor: '#667eea', backgroundColor: 'rgba(102, 126, 234, 0.1)', tension: 0.4, fill: true }] }, options: { ...chartOptions, plugins: { ...chartOptions.plugins, title: { ...chartOptions.plugins.title, text: 'Daily Schedule Distribution' } } } });\n")
            .append("</script>\n</body>\n</html>");
    }
    
    public static void main(String[] args) {
        String reportsDir = args.length > 0 ? args[0] : "reports";
        generateHtmlReport(reportsDir);
    }
} 