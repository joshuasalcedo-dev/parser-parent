package io.joshuasalcedo.parser.java;

import io.joshuasalcedo.parser.java.analyzer.*;
import io.joshuasalcedo.parser.java.cli.ComprehensiveReportGenerator;
import io.joshuasalcedo.parser.java.cli.HtmlReportGenerator;
import io.joshuasalcedo.parser.java.model.ProjectRepresentation;
import io.joshuasalcedo.parser.java.result.*;
import picocli.CommandLine;
import picocli.CommandLine.*;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * Main CLI application for Java project analysis
 */
@Command(
    name = "java-analyzer",
    mixinStandardHelpOptions = true,
    version = "Java Analyzer 1.0",
    description = "Analyzes Java projects for dependencies, metrics, and code quality",
    subcommands = {
        DependencyCommand.class,
        MetricsCommand.class,
        GraphCommand.class,
        AllCommand.class
    }
)
public class JavaAnalyzerCLI implements Callable<Integer> {
    
    @Option(names = {"-v", "--verbose"}, description = "Enable verbose output")
    private boolean verbose;
    
    @Override
    public Integer call() {
        System.out.println("Java Analyzer - Code Analysis Tool");
        System.out.println("Use --help to see available commands");
        return 0;
    }
    
    public static void main(String[] args) {
        int exitCode = new CommandLine(new JavaAnalyzerCLI()).execute(args);
        System.exit(exitCode);
    }
    
    /**
     * Parse project using the integrated JavaProjectParser
     */
    static ProjectRepresentation parseProject(String projectPath) {
        System.out.println("Parsing project at: " + projectPath);
        
        try {
            // Use the JavaProjectParser from the parser package
            io.joshuasalcedo.parser.java.parser.JavaProjectParser parser = 
                new io.joshuasalcedo.parser.java.parser.JavaProjectParser();
            return parser.parseProject(projectPath);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing project: " + e.getMessage(), e);
        }
    }
}

/**
 * Command for dependency analysis
 */
@Command(
    name = "dependency",
    aliases = {"dep"},
    description = "Analyze project dependencies and coupling"
)
class DependencyCommand implements Callable<Integer> {
    
    @Parameters(index = "0", description = "Path to the Java project")
    private File projectPath;
    
    @Option(names = {"-o", "--output"}, description = "Output directory for results")
    private File outputDir;
    
    @Option(names = {"--show-circular"}, description = "Show circular dependencies", defaultValue = "true")
    private boolean showCircular;
    
    @Option(names = {"--show-unused"}, description = "Show unused classes", defaultValue = "true")
    private boolean showUnused;
    
    @Option(names = {"--export-graph"}, description = "Export dependency graph")
    private boolean exportGraph;
    
    @Override
    public Integer call() {
        try {
            System.out.println("Analyzing dependencies for: " + projectPath.getAbsolutePath());
            
            // Parse project
            ProjectRepresentation project = JavaAnalyzerCLI.parseProject(projectPath.getAbsolutePath());
            
            // Run dependency analysis
            DependencyAnalyzer analyzer = new DependencyAnalyzer(project, projectPath.getAbsolutePath());
            DependencyResult result = analyzer.analyze();
            
            // Print results
            analyzer.printResults(result);
            
            // Export if requested
            if (outputDir != null) {
                outputDir.mkdirs();
                String outputPath = new File(outputDir, "dependencies.json").getAbsolutePath();
                analyzer.exportResults(result, outputPath);
                System.out.println("\nDependency analysis exported to: " + outputPath);
                
                if (exportGraph) {
                    // Also run graph analysis
                    GraphAnalyzer graphAnalyzer = new GraphAnalyzer(project, result);
                    GraphResult graphResult = graphAnalyzer.analyze();
                    String graphPath = new File(outputDir, "dependency-graph").getAbsolutePath();
                    graphAnalyzer.exportResults(graphResult, graphPath);
                    System.out.println("Dependency graphs exported to: " + outputDir.getAbsolutePath());
                }
            }
            
            return 0;
        } catch (Exception e) {
            System.err.println("Error analyzing dependencies: " + e.getMessage());
            if (System.getProperty("verbose") != null) {
                e.printStackTrace();
            }
            return 1;
        }
    }
}

/**
 * Command for metrics analysis
 */
@Command(
    name = "metrics",
    description = "Analyze code metrics, complexity, and duplicates"
)
class MetricsCommand implements Callable<Integer> {
    
    @Parameters(index = "0", description = "Path to the Java project")
    private File projectPath;
    
    @Option(names = {"-o", "--output"}, description = "Output directory for results")
    private File outputDir;
    
    @Option(names = {"--show-duplicates"}, description = "Show code duplicates", defaultValue = "true")
    private boolean showDuplicates;
    
    @Option(names = {"--show-unused"}, description = "Show unused methods and classes", defaultValue = "true")
    private boolean showUnused;
    
    @Option(names = {"--complexity-threshold"}, description = "Complexity threshold for warnings", defaultValue = "10")
    private int complexityThreshold;
    
    @Option(names = {"-r", "--report"}, description = "Generate HTML report")
    private boolean generateReport;
    
    @Override
    public Integer call() {
        try {
            System.out.println("Analyzing metrics for: " + projectPath.getAbsolutePath());
            
            // Parse project
            ProjectRepresentation project = JavaAnalyzerCLI.parseProject(projectPath.getAbsolutePath());
            
            // Run metrics analysis
            MetricsAnalyzer analyzer = new MetricsAnalyzer(project);
            MetricsResult result = analyzer.analyze();
            
            // Print results
            analyzer.printResults(result);
            
            // Print recommendations
            System.out.println("\n=== Recommendations ===");
            result.projectMetrics().getRecommendations().forEach(rec -> 
                System.out.println("• " + rec));
            
            // Show health grade
            System.out.println("\nProject Health Grade: " + result.projectMetrics().getProjectHealthGrade());
            System.out.println("Overall Health Score: " + String.format("%.1f%%", result.getOverallHealthScore()));
            
            // Export if requested
            if (outputDir != null) {
                outputDir.mkdirs();
                String outputPath = new File(outputDir, "metrics.json").getAbsolutePath();
                analyzer.exportResults(result, outputPath);
                System.out.println("\nMetrics analysis exported to: " + outputPath);
                
                if (generateReport) {
                    HtmlReportGenerator generator = new HtmlReportGenerator();
                    File reportFile = new File(outputDir, "metrics-report.html");
                    generator.generateReport(result, project, reportFile);
                    System.out.println("HTML report generated: " + reportFile.getAbsolutePath());
                }
            }
            
            return 0;
        } catch (Exception e) {
            System.err.println("Error analyzing metrics: " + e.getMessage());
            if (System.getProperty("verbose") != null) {
                e.printStackTrace();
            }
            return 1;
        }
    }
}

/**
 * Command for graph visualization
 */
@Command(
    name = "graph",
    aliases = {"visualize", "viz"},
    description = "Generate graph visualizations of project structure"
)
class GraphCommand implements Callable<Integer> {
    
    @Parameters(index = "0", description = "Path to the Java project")
    private File projectPath;
    
    @Option(names = {"-o", "--output"}, description = "Output directory for graph files", required = true)
    private File outputDir;
    
    @Option(names = {"-f", "--formats"}, description = "Graph formats to generate (dot,mermaid,plantuml,d2,graphml,all)", 
            split = ",", defaultValue = "all")
    private String[] formats;
    
    @Option(names = {"--package-view"}, description = "Generate package-level view", defaultValue = "true")
    private boolean packageView;
    
    @Override
    public Integer call() {
        try {
            System.out.println("Generating graphs for: " + projectPath.getAbsolutePath());
            
            // Parse project
            ProjectRepresentation project = JavaAnalyzerCLI.parseProject(projectPath.getAbsolutePath());
            
            // Run dependency analysis first (required for graph generation)
            DependencyAnalyzer depAnalyzer = new DependencyAnalyzer(project, projectPath.getAbsolutePath());
            DependencyResult depResult = depAnalyzer.analyze();
            
            // Run graph analysis
            GraphAnalyzer analyzer = new GraphAnalyzer(project, depResult);
            GraphResult result = analyzer.analyze();
            
            // Print summary
            analyzer.printResults(result);
            
            // Create output directory
            outputDir.mkdirs();
            
            // Export graphs
            String basePath = new File(outputDir, "project-graph").getAbsolutePath();
            analyzer.exportResults(result, basePath);
            
            // Generate interactive HTML
            File htmlFile = new File(outputDir, "interactive-graph.html");
            try (java.io.FileWriter writer = new java.io.FileWriter(htmlFile)) {
                writer.write(result.generateInteractiveHTML());
            }
            System.out.println("Interactive HTML viewer: " + htmlFile.getAbsolutePath());
            
            return 0;
        } catch (Exception e) {
            System.err.println("Error generating graphs: " + e.getMessage());
            if (System.getProperty("verbose") != null) {
                e.printStackTrace();
            }
            return 1;
        }
    }
}

/**
 * Command to run all analyses
 */
@Command(
    name = "all",
    description = "Run all analyses (dependency, metrics, and graph)"
)
class AllCommand implements Callable<Integer> {
    
    @Parameters(index = "0", description = "Path to the Java project")
    private File projectPath;
    
    @Option(names = {"-o", "--output"}, description = "Output directory for all results", required = true)
    private File outputDir;
    
    @Option(names = {"--generate-report"}, description = "Generate comprehensive HTML report", defaultValue = "true")
    private boolean generateReport;
    
    @Override
    public Integer call() {
        try {
            System.out.println("Running comprehensive analysis for: " + projectPath.getAbsolutePath());
            long startTime = System.currentTimeMillis();
            
            // Create output directory
            outputDir.mkdirs();
            
            // Parse project once
            ProjectRepresentation project = JavaAnalyzerCLI.parseProject(projectPath.getAbsolutePath());
            
            System.out.println("\n[1/3] Running dependency analysis...");
            DependencyAnalyzer depAnalyzer = new DependencyAnalyzer(project, projectPath.getAbsolutePath());
            DependencyResult depResult = depAnalyzer.analyze();
            depAnalyzer.exportResults(depResult, new File(outputDir, "dependencies.json").getAbsolutePath());
            
            System.out.println("\n[2/3] Running metrics analysis...");
            MetricsAnalyzer metricsAnalyzer = new MetricsAnalyzer(project);
            MetricsResult metricsResult = metricsAnalyzer.analyze();
            metricsAnalyzer.exportResults(metricsResult, new File(outputDir, "metrics.json").getAbsolutePath());
            
            System.out.println("\n[3/3] Generating visualizations...");
            GraphAnalyzer graphAnalyzer = new GraphAnalyzer(project, depResult);
            GraphResult graphResult = graphAnalyzer.analyze();
            graphAnalyzer.exportResults(graphResult, new File(outputDir, "graphs").getAbsolutePath());
            
            // Generate comprehensive report
            System.out.println("\nDebug: generateReport flag = " + generateReport);
            if (generateReport) {
                System.out.println("\nGenerating comprehensive report...");
                try {
                    ComprehensiveReportGenerator reportGen = new ComprehensiveReportGenerator();
                    File reportFile = new File(outputDir, "analysis-report.html");
                    reportGen.generateReport(project, depResult, metricsResult, graphResult, reportFile, projectPath.toString());
                    System.out.println("✅ Comprehensive report with submodules: " + reportFile.getAbsolutePath());
                } catch (Exception e) {
                    System.err.println("Error generating comprehensive report: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("Report generation is disabled");
            }
            
            long endTime = System.currentTimeMillis();
            double duration = (endTime - startTime) / 1000.0;
            
            System.out.println("\n=== Analysis Complete ===");
            System.out.println("Total time: " + String.format("%.2f seconds", duration));
            System.out.println("Results saved to: " + outputDir.getAbsolutePath());
            
            // Print summary
            printSummary(project, depResult, metricsResult);
            
            return 0;
        } catch (Exception e) {
            System.err.println("Error during analysis: " + e.getMessage());
            if (System.getProperty("verbose") != null) {
                e.printStackTrace();
            }
            return 1;
        }
    }
    
    private void printSummary(ProjectRepresentation project, DependencyResult depResult, MetricsResult metricsResult) {
        System.out.println("\n=== Summary ===");
        System.out.println("Project: " + project.getName());
        System.out.println("Classes: " + project.getClasses().size());
        System.out.println("Health Grade: " + metricsResult.projectMetrics().getProjectHealthGrade());
        
        if (depResult.hasCircularDependencies()) {
            System.out.println("⚠️  Circular Dependencies: " + depResult.circularDependencies().size());
        }
        
        if (!metricsResult.codeDuplicates().isEmpty()) {
            System.out.println("⚠️  Code Duplicates: " + metricsResult.codeDuplicates().size());
        }
        
        if (!metricsResult.unusedClasses().isEmpty() || !metricsResult.unusedMethods().isEmpty()) {
            System.out.println("⚠️  Unused Code: " + 
                (metricsResult.unusedClasses().size() + metricsResult.unusedMethods().size()) + " items");
        }
    }
}