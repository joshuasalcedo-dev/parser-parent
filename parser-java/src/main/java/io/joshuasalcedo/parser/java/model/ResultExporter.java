package io.joshuasalcedo.parser.java.model;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Exports analysis results to various formats
 */
public class ResultExporter {
    
    private final Gson gson;
    
    public ResultExporter() {
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    }
    
    /**
     * Export results to JSON format
     */
    public void exportToJson(ProjectAnalysisResult results, String outputPath) throws IOException {
        Map<String, Object> exportData = new HashMap<>();
        
        exportData.put("project", Map.of(
            "name", results.getProject().getName(),
            "path", results.getProject().getRootPath(),
            "classCount", results.getProject().getClasses().size()
        ));
        
        exportData.put("statistics", results.getStatistics());
        exportData.put("summary", results.getSummary());
        exportData.put("analysisTimeMs", results.getAnalysisTimeMs());
        
        if (results.getDependencyResult() != null) {
            exportData.put("dependencies", Map.of(
                "circularDependencies", results.getDependencyResult().circularDependencies(),
                "unusedClasses", results.getDependencyResult().unusedClasses(),
                "maxCoupling", results.getDependencyResult().getMaxEfferentCoupling(),
                "averageInstability", results.getDependencyResult().getAverageInstability()
            ));
        }
        
        if (results.getMetricsResult() != null) {
            exportData.put("metrics", Map.of(
                "healthGrade", results.getMetricsResult().projectMetrics().getProjectHealthGrade(),
                "healthScore", results.getMetricsResult().getOverallHealthScore(),
                "duplicationRatio", results.getMetricsResult().projectMetrics().duplicationRatio(),
                "averageComplexity", results.getMetricsResult().projectMetrics().averageComplexity(),
                "technicalDebt", results.getMetricsResult().projectMetrics().technicalDebtScore()
            ));
        }
        
        try (FileWriter writer = new FileWriter(outputPath)) {
            gson.toJson(exportData, writer);
        }
    }
    
    /**
     * Export results to HTML format
     */
    public void exportToHtml(ProjectAnalysisResult results, String outputPath) throws IOException {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>\n<html>\n<head>\n");
        html.append("<title>").append(results.getProject().getName()).append(" - Analysis Report</title>\n");
        html.append(getHtmlStyles());
        html.append("</head>\n<body>\n");
        
        // Header
        html.append("<div class=\"header\">\n");
        html.append("<h1>Java Code Analysis Report</h1>\n");
        html.append("<h2>").append(results.getProject().getName()).append("</h2>\n");
        html.append("<p class=\"timestamp\">Generated: ").append(getCurrentTimestamp()).append("</p>\n");
        html.append("</div>\n");
        
        // Summary Section
        html.append("<div class=\"section\">\n");
        html.append("<h2>Executive Summary</h2>\n");
        html.append("<div class=\"summary-grid\">\n");
        
        Map<String, Object> summary = results.getSummary();
        html.append(createSummaryCard("Health Grade", summary.get("healthGrade").toString(), getGradeColor(summary.get("healthGrade").toString())));
        html.append(createSummaryCard("Classes", summary.get("totalClasses").toString(), "#3498db"));
        html.append(createSummaryCard("Methods", summary.get("totalMethods").toString(), "#3498db"));
        html.append(createSummaryCard("Lines of Code", summary.get("totalLinesOfCode").toString(), "#3498db"));
        
        html.append("</div>\n</div>\n");
        
        // Issues Section
        if (results.hasCircularDependencies() || results.hasCodeDuplication()) {
            html.append("<div class=\"section issues\">\n");
            html.append("<h2>Issues Found</h2>\n");
            
            if (results.hasCircularDependencies()) {
                html.append("<div class=\"issue-item critical\">\n");
                html.append("<h3>🔄 Circular Dependencies</h3>\n");
                html.append("<p>").append(results.getDependencyResult().circularDependencies().size())
                    .append(" circular dependency chains detected</p>\n");
                html.append("</div>\n");
            }
            
            if (results.hasCodeDuplication()) {
                html.append("<div class=\"issue-item warning\">\n");
                html.append("<h3>📋 Code Duplication</h3>\n");
                html.append("<p>").append(results.getMetricsResult().codeDuplicates().size())
                    .append(" code duplications found</p>\n");
                html.append("</div>\n");
            }
            
            html.append("</div>\n");
        }
        
        // Metrics Details
        if (results.getMetricsResult() != null) {
            html.append("<div class=\"section\">\n");
            html.append("<h2>Code Metrics</h2>\n");
            html.append("<table>\n<thead>\n<tr>\n");
            html.append("<th>Metric</th><th>Value</th><th>Status</th>\n");
            html.append("</tr>\n</thead>\n<tbody>\n");
            
            ProjectMetrics pm = results.getMetricsResult().projectMetrics();
            html.append(createMetricRow("Average Complexity", String.format("%.2f", pm.averageComplexity()), 
                pm.averageComplexity() < 10 ? "good" : "warning"));
            html.append(createMetricRow("Code Duplication", String.format("%.1f%%", pm.duplicationRatio() * 100), 
                pm.duplicationRatio() < 0.1 ? "good" : "warning"));
            html.append(createMetricRow("Code Reuse", String.format("%.1f%%", pm.codeReuse() * 100), 
                pm.codeReuse() > 0.3 ? "good" : "warning"));
            html.append(createMetricRow("Unused Code", pm.unusedMethods() + pm.unusedClasses() + " items", 
                (pm.unusedMethods() + pm.unusedClasses()) < 10 ? "good" : "warning"));
            
            html.append("</tbody>\n</table>\n</div>\n");
        }
        
        // Recommendations
        if (results.getMetricsResult() != null) {
            List<String> recommendations = results.getMetricsResult().projectMetrics().getRecommendations();
            if (!recommendations.isEmpty()) {
                html.append("<div class=\"section\">\n");
                html.append("<h2>Recommendations</h2>\n");
                html.append("<ul>\n");
                recommendations.forEach(rec -> html.append("<li>").append(rec).append("</li>\n"));
                html.append("</ul>\n</div>\n");
            }
        }
        
        html.append("</body>\n</html>");
        
        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(html.toString());
        }
    }
    
    /**
     * Export results to Markdown format
     */
    public void exportToMarkdown(ProjectAnalysisResult results, String outputPath) throws IOException {
        StringBuilder md = new StringBuilder();
        
        md.append("# Java Code Analysis Report\n\n");
        md.append("## ").append(results.getProject().getName()).append("\n\n");
        md.append("Generated: ").append(getCurrentTimestamp()).append("\n\n");
        
        // Summary
        md.append("## Executive Summary\n\n");
        Map<String, Object> summary = results.getSummary();
        md.append("| Metric | Value |\n");
        md.append("|--------|-------|\n");
        md.append("| Health Grade | **").append(summary.get("healthGrade")).append("** |\n");
        md.append("| Total Classes | ").append(summary.get("totalClasses")).append(" |\n");
        md.append("| Total Methods | ").append(summary.get("totalMethods")).append(" |\n");
        md.append("| Lines of Code | ").append(summary.get("totalLinesOfCode")).append(" |\n");
        md.append("| Analysis Time | ").append(summary.get("analysisTimeMs")).append(" ms |\n\n");
        
        // Issues
        if (results.hasCircularDependencies() || results.hasCodeDuplication()) {
            md.append("## Issues Found\n\n");
            
            if (results.hasCircularDependencies()) {
                md.append("### ⚠️ Circular Dependencies\n\n");
                md.append("Found **").append(results.getDependencyResult().circularDependencies().size())
                  .append("** circular dependency chains.\n\n");
            }
            
            if (results.hasCodeDuplication()) {
                md.append("### ⚠️ Code Duplication\n\n");
                md.append("Found **").append(results.getMetricsResult().codeDuplicates().size())
                  .append("** instances of duplicated code.\n\n");
            }
        }
        
        // Detailed Metrics
        if (results.getMetricsResult() != null) {
            md.append("## Detailed Metrics\n\n");
            ProjectMetrics pm = results.getMetricsResult().projectMetrics();
            
            md.append("- **Average Complexity**: ").append(String.format("%.2f", pm.averageComplexity())).append("\n");
            md.append("- **Average Maintainability**: ").append(String.format("%.2f", pm.averageMaintainability())).append("\n");
            md.append("- **Code Duplication**: ").append(String.format("%.1f%%", pm.duplicationRatio() * 100)).append("\n");
            md.append("- **Code Reuse**: ").append(String.format("%.1f%%", pm.codeReuse() * 100)).append("\n");
            md.append("- **Technical Debt Score**: ").append(String.format("%.0f", pm.technicalDebtScore())).append("\n\n");
        }
        
        // Recommendations
        if (results.getMetricsResult() != null) {
            List<String> recommendations = results.getMetricsResult().projectMetrics().getRecommendations();
            if (!recommendations.isEmpty()) {
                md.append("## Recommendations\n\n");
                recommendations.forEach(rec -> md.append("- ").append(rec).append("\n"));
            }
        }
        
        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(md.toString());
        }
    }
    
    /**
     * Export results to CSV format (metrics only)
     */
    public void exportToCsv(ProjectAnalysisResult results, String outputPath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            // Header
            writer.println("Class,Methods,Fields,Complexity,Maintainability,LOC,Coupling,Cohesion");
            
            if (results.getMetricsResult() != null) {
                for (ClassMetrics cm : results.getMetricsResult().classMetrics()) {
                    writer.printf("%s,%d,%d,%.2f,%.2f,%d,%d,%.2f%n",
                        cm.className(),
                        cm.methodCount(),
                        cm.fieldCount(),
                        cm.complexity(),
                        cm.maintainabilityIndex(),
                        cm.linesOfCode(),
                        cm.efferentCoupling() + cm.afferentCoupling(),
                        cm.cohesion()
                    );
                }
            }
        }
    }
    
    private String getHtmlStyles() {
        return """
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif;
                    margin: 0;
                    padding: 0;
                    background-color: #f5f7fa;
                    color: #333;
                }
                .header {
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    color: white;
                    padding: 40px;
                    text-align: center;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                }
                .header h1 {
                    margin: 0;
                    font-size: 2.5em;
                }
                .header h2 {
                    margin: 10px 0 0 0;
                    font-weight: normal;
                    opacity: 0.9;
                }
                .timestamp {
                    margin-top: 10px;
                    opacity: 0.8;
                }
                .section {
                    max-width: 1200px;
                    margin: 30px auto;
                    background: white;
                    border-radius: 10px;
                    padding: 30px;
                    box-shadow: 0 2px 10px rgba(0,0,0,0.08);
                }
                .summary-grid {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                    gap: 20px;
                    margin-top: 20px;
                }
                .summary-card {
                    background: #f8f9fa;
                    border-radius: 8px;
                    padding: 20px;
                    text-align: center;
                    border-left: 4px solid;
                }
                .summary-card h3 {
                    margin: 0;
                    color: #666;
                    font-size: 0.9em;
                    text-transform: uppercase;
                }
                .summary-card .value {
                    font-size: 2em;
                    font-weight: bold;
                    margin: 10px 0;
                }
                .issues {
                    background: #fff9f0;
                }
                .issue-item {
                    padding: 20px;
                    margin: 15px 0;
                    border-radius: 8px;
                    border-left: 4px solid;
                }
                .issue-item.critical {
                    background: #ffebee;
                    border-color: #f44336;
                }
                .issue-item.warning {
                    background: #fff3e0;
                    border-color: #ff9800;
                }
                table {
                    width: 100%;
                    border-collapse: collapse;
                    margin-top: 20px;
                }
                th, td {
                    text-align: left;
                    padding: 12px;
                    border-bottom: 1px solid #e0e0e0;
                }
                th {
                    background-color: #f5f5f5;
                    font-weight: 600;
                }
                .good { color: #4caf50; }
                .warning { color: #ff9800; }
                .critical { color: #f44336; }
                ul {
                    line-height: 1.8;
                }
            </style>
            """;
    }
    
    private String createSummaryCard(String label, String value, String color) {
        return String.format(
            "<div class=\"summary-card\" style=\"border-color: %s;\">\n" +
            "<h3>%s</h3>\n" +
            "<div class=\"value\" style=\"color: %s;\">%s</div>\n" +
            "</div>\n",
            color, label, color, value
        );
    }
    
    private String createMetricRow(String metric, String value, String status) {
        return String.format(
            "<tr>\n<td>%s</td>\n<td>%s</td>\n<td class=\"%s\">%s</td>\n</tr>\n",
            metric, value, status, status.toUpperCase()
        );
    }
    
    private String getGradeColor(String grade) {
        switch (grade) {
            case "A": return "#4caf50";
            case "B": return "#8bc34a";
            case "C": return "#ff9800";
            case "D": return "#ff5722";
            case "F": return "#f44336";
            default: return "#999";
        }
    }
    
    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}