package io.joshuasalcedo.parser.java.model;

import java.util.*;

/**
 * Main result model containing all analysis results
 */
public class ProjectAnalysisResult {
    private final ProjectRepresentation project;
    private final ProjectStatistics statistics;
    private final ProjectDependencyResult projectDependencyResult;
    private final MetricsResult metricsResult;
    private final GraphResult graphResult;
    private final long analysisTimeMs;
    
    public ProjectAnalysisResult(ProjectRepresentation project,
                                 ProjectStatistics statistics,
                                 ProjectDependencyResult projectDependencyResult,
                                 MetricsResult metricsResult,
                                 GraphResult graphResult,
                                 long analysisTimeMs) {
        this.project = project;
        this.statistics = statistics;
        this.projectDependencyResult = projectDependencyResult;
        this.metricsResult = metricsResult;
        this.graphResult = graphResult;
        this.analysisTimeMs = analysisTimeMs;
    }
    
    // Getters
    public ProjectRepresentation getProject() { return project; }
    public ProjectStatistics getStatistics() { return statistics; }
    public ProjectDependencyResult getDependencyResult() { return projectDependencyResult; }
    public MetricsResult getMetricsResult() { return metricsResult; }
    public GraphResult getGraphResult() { return graphResult; }
    public long getAnalysisTimeMs() { return analysisTimeMs; }
    
    // Convenience methods
    public boolean hasCircularDependencies() {
        return projectDependencyResult != null && projectDependencyResult.hasCircularDependencies();
    }
    
    public boolean hasCodeDuplication() {
        return metricsResult != null && !metricsResult.codeDuplicates().isEmpty();
    }
    
    public String getHealthGrade() {
        return metricsResult != null ? metricsResult.projectMetrics().getProjectHealthGrade() : "N/A";
    }
    
    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("projectName", project.getName());
        summary.put("totalClasses", statistics.totalClasses());
        summary.put("totalMethods", statistics.totalMethods());
        summary.put("totalLinesOfCode", statistics.totalLinesOfCode());
        summary.put("healthGrade", getHealthGrade());
        summary.put("hasCircularDependencies", hasCircularDependencies());
        summary.put("hasCodeDuplication", hasCodeDuplication());
        summary.put("analysisTimeMs", analysisTimeMs);
        return summary;
    }
}

