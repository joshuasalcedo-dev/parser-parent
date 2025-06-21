package io.joshuasalcedo.parser.spring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Java Parser Spring Boot Starter.
 */
@ConfigurationProperties(prefix = "parser")
public class ParserProperties {
    
    /**
     * Enable or disable automatic project analysis on startup
     */
    private boolean autoAnalyzeOnStartup = true;
    
    /**
     * Project root path to analyze. If not specified, uses the application's working directory
     */
    private String projectPath;
    
    /**
     * Enable parallel analysis for better performance
     */
    private boolean parallelAnalysis = true;
    
    /**
     * Thread pool size for parallel analysis
     */
    private int threadPoolSize = Runtime.getRuntime().availableProcessors();
    
    /**
     * Enable dependency analysis
     */
    private boolean analyzeDependencies = true;
    
    /**
     * Enable code metrics analysis
     */
    private boolean analyzeMetrics = true;
    
    /**
     * Enable graph generation
     */
    private boolean generateGraphs = true;
    
    /**
     * API endpoint path prefix
     */
    private String apiPath = "/api/parser";
    
    
    public boolean isAutoAnalyzeOnStartup() {
        return autoAnalyzeOnStartup;
    }
    
    public void setAutoAnalyzeOnStartup(boolean autoAnalyzeOnStartup) {
        this.autoAnalyzeOnStartup = autoAnalyzeOnStartup;
    }
    
    public String getProjectPath() {
        return projectPath;
    }
    
    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }
    
    public boolean isParallelAnalysis() {
        return parallelAnalysis;
    }
    
    public void setParallelAnalysis(boolean parallelAnalysis) {
        this.parallelAnalysis = parallelAnalysis;
    }
    
    public int getThreadPoolSize() {
        return threadPoolSize;
    }
    
    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }
    
    public boolean isAnalyzeDependencies() {
        return analyzeDependencies;
    }
    
    public void setAnalyzeDependencies(boolean analyzeDependencies) {
        this.analyzeDependencies = analyzeDependencies;
    }
    
    public boolean isAnalyzeMetrics() {
        return analyzeMetrics;
    }
    
    public void setAnalyzeMetrics(boolean analyzeMetrics) {
        this.analyzeMetrics = analyzeMetrics;
    }
    
    public boolean isGenerateGraphs() {
        return generateGraphs;
    }
    
    public void setGenerateGraphs(boolean generateGraphs) {
        this.generateGraphs = generateGraphs;
    }
    
    public String getApiPath() {
        return apiPath;
    }
    
    public void setApiPath(String apiPath) {
        this.apiPath = apiPath;
    }
    
}