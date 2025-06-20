package io.joshuasalcedo.parser.java.parser;

import io.joshuasalcedo.parser.java.analyzer.*;
import io.joshuasalcedo.parser.java.model.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main library class for analyzing Java projects.
 * Provides a simple API to parse and analyze Java codebases.
 */
public class JavaProjectAnalyzer implements AutoCloseable {
    
    private final AnalyzerConfig config;
    private final ExecutorService executor;
    
    /**
     * Create analyzer with default configuration
     */
    public JavaProjectAnalyzer() {
        this(AnalyzerConfig.defaultConfig());
    }
    
    /**
     * Create analyzer with custom configuration
     */
    public JavaProjectAnalyzer(AnalyzerConfig config) {
        this.config = config;
        this.executor = config.isParallelAnalysis() ? 
            Executors.newFixedThreadPool(config.getThreadPoolSize()) : null;
    }
    
    /**
     * Analyze a Java project and return comprehensive results
     * @param projectPath Path to the project root directory
     * @return Complete analysis results
     */
    public ProjectAnalysisResult analyzeProject(String projectPath) throws IOException {
        return analyzeProject(new File(projectPath));
    }
    
    /**
     * Analyze a Java project and return comprehensive results
     * @param projectPath Path to the project root directory
     * @return Complete analysis results
     */
    public ProjectAnalysisResult analyzeProject(Path projectPath) throws IOException {
        return analyzeProject(projectPath.toFile());
    }
    
    /**
     * Analyze a Java project and return comprehensive results
     * @param projectDir Project root directory
     * @return Complete analysis results
     */
    public ProjectAnalysisResult analyzeProject(File projectDir) throws IOException {
        validateProjectDirectory(projectDir);
        
        long startTime = System.currentTimeMillis();
        
        // Parse the project
        JavaProjectParser parser = new JavaProjectParser();
        ProjectRepresentation project = parser.parseProject(projectDir.getAbsolutePath());
        
        if (project.getClasses().isEmpty()) {
            throw new IllegalArgumentException("No Java classes found in project: " + projectDir);
        }
        
        // Run analyses based on configuration
        DependencyResult dependencyResult = null;
        MetricsResult metricsResult = null;
        GraphResult graphResult = null;
        
        if (config.isParallelAnalysis() && executor != null) {
            // Run analyses in parallel using CompletableFuture
            CompletableFuture<DependencyResult> dependencyFuture = null;
            CompletableFuture<MetricsResult> metricsFuture = null;
            
            if (config.isAnalyzeDependencies()) {
                dependencyFuture = CompletableFuture.supplyAsync(() -> {
                    DependencyAnalyzer analyzer = new DependencyAnalyzer(project, projectDir.getAbsolutePath());
                    return analyzer.analyze();
                }, executor);
            }
            
            if (config.isAnalyzeMetrics()) {
                metricsFuture = CompletableFuture.supplyAsync(() -> {
                    MetricsAnalyzer analyzer = new MetricsAnalyzer(project);
                    return analyzer.analyze();
                }, executor);
            }
            
            // Wait for all analyses to complete and get results
            try {
                if (dependencyFuture != null) {
                    dependencyResult = dependencyFuture.get();
                }
                if (metricsFuture != null) {
                    metricsResult = metricsFuture.get();
                }
            } catch (Exception e) {
                throw new RuntimeException("Error during parallel analysis", e);
            }
            
        } else {
            // Run analyses sequentially
            if (config.isAnalyzeDependencies()) {
                DependencyAnalyzer depAnalyzer = new DependencyAnalyzer(project, projectDir.getAbsolutePath());
                dependencyResult = depAnalyzer.analyze();
            } else {
                dependencyResult = null;
            }

            if (config.isAnalyzeMetrics()) {
                MetricsAnalyzer metricsAnalyzer = new MetricsAnalyzer(project);
                metricsResult = metricsAnalyzer.analyze();
            } else {
                metricsResult = null;
            }
        }
        
        // Generate graphs if requested
        if (config.isGenerateGraphs() && dependencyResult != null) {
            GraphAnalyzer graphAnalyzer = new GraphAnalyzer(project, dependencyResult);
            graphResult = graphAnalyzer.analyze();
        }
        
        // Calculate statistics
        ProjectStatistics statistics = parser.calculateStatistics(project);
        
        long analysisTime = System.currentTimeMillis() - startTime;
        
        return new ProjectAnalysisResult(
            project,
            statistics,
            dependencyResult,
            metricsResult,
            graphResult,
            analysisTime
        );
    }
    
    /**
     * Get a quick summary of the project without deep analysis
     */
    public ProjectSummary getProjectSummary(String projectPath) throws IOException {
        File projectDir = new File(projectPath);
        validateProjectDirectory(projectDir);
        
        JavaProjectParser parser = new JavaProjectParser();
        ProjectRepresentation project = parser.parseProject(projectDir.getAbsolutePath());
        ProjectStatistics statistics = parser.calculateStatistics(project);
        
        return new ProjectSummary(
            project.getName(),
            projectDir.getAbsolutePath(),
            statistics.totalClasses(),
            statistics.totalInterfaces(),
            statistics.totalEnums(),
            statistics.totalMethods(),
            statistics.totalLinesOfCode(),
            statistics.packageDistribution()
        );
    }
    
    /**
     * Analyze only dependencies
     */
    public DependencyResult analyzeDependencies(String projectPath) throws IOException {
        File projectDir = new File(projectPath);
        validateProjectDirectory(projectDir);
        
        JavaProjectParser parser = new JavaProjectParser();
        ProjectRepresentation project = parser.parseProject(projectDir.getAbsolutePath());
        
        DependencyAnalyzer analyzer = new DependencyAnalyzer(project, projectDir.getAbsolutePath());
        return analyzer.analyze();
    }
    
    /**
     * Analyze only code metrics
     */
    public MetricsResult analyzeMetrics(String projectPath) throws IOException {
        File projectDir = new File(projectPath);
        validateProjectDirectory(projectDir);
        
        JavaProjectParser parser = new JavaProjectParser();
        ProjectRepresentation project = parser.parseProject(projectDir.getAbsolutePath());
        
        MetricsAnalyzer analyzer = new MetricsAnalyzer(project);
        return analyzer.analyze();
    }
    
    /**
     * Find specific patterns in the code
     */
    public PatternSearchResult findPatterns(String projectPath, List<CodePattern> patterns) throws IOException {
        File projectDir = new File(projectPath);
        validateProjectDirectory(projectDir);
        
        JavaProjectParser parser = new JavaProjectParser();
        ProjectRepresentation project = parser.parseProject(projectDir.getAbsolutePath());
        
        Map<CodePattern, List<PatternMatch>> matches = new HashMap<>();
        
        for (CodePattern pattern : patterns) {
            List<PatternMatch> patternMatches = new ArrayList<>();
            
            for (ClassRepresentation cls : project.getClasses()) {
                // Match class-level patterns
                if (pattern.matches(cls)) {
                    patternMatches.add(new PatternMatch(
                        pattern,
                        cls.getFullyQualifiedName(),
                        cls.getFilePath(),
                        "class",
                        cls.getName()
                    ));
                }
                
                // Match method-level patterns
                for (MethodRepresentation method : cls.getMethods()) {
                    if (pattern.matchesMethod(method)) {
                        patternMatches.add(new PatternMatch(
                            pattern,
                            cls.getFullyQualifiedName() + "." + method.getName(),
                            cls.getFilePath(),
                            "method",
                            method.getName()
                        ));
                    }
                }
            }
            
            matches.put(pattern, patternMatches);
        }
        
        return new PatternSearchResult(matches);
    }
    
    /**
     * Export analysis results in various formats
     */
    public void exportResults(ProjectAnalysisResult results, String outputPath, ExportFormat format) throws IOException {
        ResultExporter exporter = new ResultExporter();
        
        switch (format) {
            case JSON:
                exporter.exportToJson(results, outputPath);
                break;
            case HTML:
                exporter.exportToHtml(results, outputPath);
                break;
            case MARKDOWN:
                exporter.exportToMarkdown(results, outputPath);
                break;
            case CSV:
                exporter.exportToCsv(results, outputPath);
                break;
            default:
                throw new IllegalArgumentException("Unsupported export format: " + format);
        }
    }
    
    /**
     * Get health assessment of the project
     */
    public ProjectHealthAssessment assessProjectHealth(String projectPath) throws IOException {
        ProjectAnalysisResult analysis = analyzeProject(projectPath);
        
        return new ProjectHealthAssessment(
            analysis.getMetricsResult() != null ? analysis.getMetricsResult().getOverallHealthScore() : 0.0,
            analysis.getMetricsResult() != null ? analysis.getMetricsResult().projectMetrics().getProjectHealthGrade() : "N/A",
            generateHealthRecommendations(analysis),
            identifyRisks(analysis)
        );
    }
    
    private List<String> generateHealthRecommendations(ProjectAnalysisResult analysis) {
        List<String> recommendations = new ArrayList<>();
        
        if (analysis.getMetricsResult() != null) {
            recommendations.addAll(analysis.getMetricsResult().projectMetrics().getRecommendations());
        }
        
        if (analysis.getDependencyResult() != null && analysis.getDependencyResult().hasCircularDependencies()) {
            recommendations.add("Resolve " + analysis.getDependencyResult().circularDependencies().size() + 
                              " circular dependencies to improve maintainability");
        }
        
        return recommendations;
    }
    
    private List<ProjectRisk> identifyRisks(ProjectAnalysisResult analysis) {
        List<ProjectRisk> risks = new ArrayList<>();
        
        if (analysis.getDependencyResult() != null) {
            if (analysis.getDependencyResult().hasCircularDependencies()) {
                risks.add(new ProjectRisk(
                    RiskLevel.HIGH,
                    "Circular Dependencies",
                    "Found " + analysis.getDependencyResult().circularDependencies().size() + " circular dependencies",
                    "Refactor to break circular dependencies"
                ));
            }
            
            if (analysis.getDependencyResult().getMaxEfferentCoupling() > 20) {
                risks.add(new ProjectRisk(
                    RiskLevel.MEDIUM,
                    "High Coupling",
                    "Some classes have very high coupling (>20 dependencies)",
                    "Consider breaking down highly coupled classes"
                ));
            }
        }
        
        if (analysis.getMetricsResult() != null) {
            if (analysis.getMetricsResult().projectMetrics().duplicationRatio() > 0.2) {
                risks.add(new ProjectRisk(
                    RiskLevel.MEDIUM,
                    "Code Duplication",
                    String.format("%.1f%% of code is duplicated", 
                        analysis.getMetricsResult().projectMetrics().duplicationRatio() * 100),
                    "Extract duplicated code into reusable components"
                ));
            }
        }
        
        return risks;
    }
    
    private void validateProjectDirectory(File projectDir) {
        if (!projectDir.exists()) {
            throw new IllegalArgumentException("Project directory does not exist: " + projectDir);
        }
        if (!projectDir.isDirectory()) {
            throw new IllegalArgumentException("Path is not a directory: " + projectDir);
        }
    }
    
    /**
     * Close resources
     */
    public void close() {
        if (executor != null) {
            executor.shutdown();
        }
    }
}

