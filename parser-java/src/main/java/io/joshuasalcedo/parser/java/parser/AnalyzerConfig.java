package io.joshuasalcedo.parser.java.parser;

/**
 * Configuration for the analyzer
 */
public class AnalyzerConfig {
    private boolean analyzeDependencies = true;
    private boolean analyzeMetrics = true;
    private boolean generateGraphs = true;
    private boolean parallelAnalysis = false;
    private int threadPoolSize = 4;
    
    public static AnalyzerConfig defaultConfig() {
        return new AnalyzerConfig();
    }
    
    public static AnalyzerConfig minimalConfig() {
        AnalyzerConfig config = new AnalyzerConfig();
        config.analyzeDependencies = false;
        config.analyzeMetrics = true;
        config.generateGraphs = false;
        return config;
    }
    
    public static AnalyzerConfig fullConfig() {
        AnalyzerConfig config = new AnalyzerConfig();
        config.parallelAnalysis = true;
        return config;
    }
    
    // Builder pattern methods
    public AnalyzerConfig withDependencies(boolean analyze) {
        this.analyzeDependencies = analyze;
        return this;
    }
    
    public AnalyzerConfig withMetrics(boolean analyze) {
        this.analyzeMetrics = analyze;
        return this;
    }
    
    public AnalyzerConfig withGraphs(boolean generate) {
        this.generateGraphs = generate;
        return this;
    }
    
    public AnalyzerConfig withParallelAnalysis(boolean parallel) {
        this.parallelAnalysis = parallel;
        return this;
    }
    
    public AnalyzerConfig withThreadPoolSize(int size) {
        this.threadPoolSize = size;
        return this;
    }
    
    // Getters
    public boolean isAnalyzeDependencies() { return analyzeDependencies; }
    public boolean isAnalyzeMetrics() { return analyzeMetrics; }
    public boolean isGenerateGraphs() { return generateGraphs; }
    public boolean isParallelAnalysis() { return parallelAnalysis; }
    public int getThreadPoolSize() { return threadPoolSize; }

    public AnalyzerConfig withAnalyzeDependencies(boolean analyzeDependencies) {
        this.withDependencies(analyzeDependencies);
        return this;
    }

    public AnalyzerConfig withAnalyzeMetrics(boolean analyzeMetrics) {
        withMetrics(analyzeMetrics);
        return this;
    }

    public AnalyzerConfig withGenerateGraphs(boolean generateGraphs) {
        withGraphs(generateGraphs);
        return  this;
    }
}
