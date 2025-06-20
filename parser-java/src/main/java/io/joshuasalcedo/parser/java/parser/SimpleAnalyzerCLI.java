package io.joshuasalcedo.parser.java.parser;



import io.joshuasalcedo.parser.java.model.ProjectAnalysisResult;
import io.joshuasalcedo.parser.java.model.ProjectHealthAssessment;
import io.joshuasalcedo.parser.java.model.ProjectSummary;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Simple CLI for Java Project Analyzer
 * Single entry point for command-line usage
 */
public class SimpleAnalyzerCLI {
    
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }
        
        String projectPath = args[0];
        String command = args.length > 1 ? args[1] : "analyze";
        
        try (JavaProjectAnalyzer analyzer = new JavaProjectAnalyzer()) {
            
            switch (command.toLowerCase()) {
                case "analyze":
                    runFullAnalysis(analyzer, projectPath);
                    break;
                    
                case "quick":
                    runQuickSummary(analyzer, projectPath);
                    break;
                    
                case "health":
                    runHealthCheck(analyzer, projectPath);
                    break;
                    
                case "export":
                    if (args.length < 4) {
                        System.err.println("Usage: <project-path> export <format> <output-file>");
                        System.exit(1);
                    }
                    exportAnalysis(analyzer, projectPath, args[2], args[3]);
                    break;
                    
                default:
                    System.err.println("Unknown command: " + command);
                    printUsage();
                    System.exit(1);
            }
            
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void runFullAnalysis(JavaProjectAnalyzer analyzer, String projectPath) throws IOException {
        System.out.println("Analyzing project: " + projectPath);
        System.out.println("=".repeat(60));
        
        ProjectAnalysisResult result = analyzer.analyzeProject(projectPath);
        
        // Summary
        System.out.println("\n📊 PROJECT SUMMARY");
        System.out.println("-".repeat(40));
        System.out.printf("Project: %s%n", result.getProject().getName());
        System.out.printf("Health Grade: %s%n", result.getHealthGrade());
        System.out.printf("Classes: %d%n", result.getStatistics().totalClasses());
        System.out.printf("Methods: %d%n", result.getStatistics().totalMethods());
        System.out.printf("Lines of Code: %d%n", result.getStatistics().totalLinesOfCode());
        System.out.printf("Analysis Time: %d ms%n", result.getAnalysisTimeMs());
        
        // Issues
        if (result.hasCircularDependencies() || result.hasCodeDuplication()) {
            System.out.println("\n⚠️  ISSUES FOUND");
            System.out.println("-".repeat(40));
            
            if (result.hasCircularDependencies()) {
                System.out.printf("Circular Dependencies: %d%n", 
                    result.getDependencyResult().circularDependencies().size());
            }
            
            if (result.hasCodeDuplication()) {
                System.out.printf("Code Duplications: %d%n", 
                    result.getMetricsResult().codeDuplicates().size());
            }
        }
        
        // Recommendations
        if (result.getMetricsResult() != null) {
            List<String> recommendations = result.getMetricsResult().projectMetrics().getRecommendations();
            if (!recommendations.isEmpty()) {
                System.out.println("\n💡 RECOMMENDATIONS");
                System.out.println("-".repeat(40));
                recommendations.forEach(rec -> System.out.println("• " + rec));
            }
        }
    }
    
    private static void runQuickSummary(JavaProjectAnalyzer analyzer, String projectPath) throws IOException {
        ProjectSummary summary = analyzer.getProjectSummary(projectPath);
        
        System.out.println("Quick Summary: " + summary.getProjectName());
        System.out.println("=".repeat(60));
        System.out.printf("Classes: %d, Interfaces: %d, Enums: %d%n", 
            summary.getTotalClasses(), summary.getTotalInterfaces(), summary.getTotalEnums());
        System.out.printf("Methods: %d%n", summary.getTotalMethods());
        System.out.printf("Lines of Code: %d%n", summary.getTotalLinesOfCode());
        
        System.out.println("\nTop Packages:");
        summary.getPackageDistribution().entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .forEach(e -> System.out.printf("  %s: %d classes%n", e.getKey(), e.getValue()));
    }
    
    private static void runHealthCheck(JavaProjectAnalyzer analyzer, String projectPath) throws IOException {
        ProjectHealthAssessment health = analyzer.assessProjectHealth(projectPath);
        
        System.out.println("Project Health Check");
        System.out.println("=".repeat(60));
        System.out.printf("Health Score: %.1f/100%n", health.getHealthScore());
        System.out.printf("Health Grade: %s%n", health.getHealthGrade());
        System.out.printf("Status: %s%n", health.isHealthy() ? "✅ HEALTHY" : "⚠️ NEEDS ATTENTION");
        
        if (!health.getRisks().isEmpty()) {
            System.out.println("\n🚨 RISKS");
            System.out.println("-".repeat(40));
            health.getRisks().forEach(risk -> {
                System.out.printf("%s - %s: %s%n", risk.getLevel(), risk.getCategory(), risk.getDescription());
                System.out.printf("  → %s%n", risk.getMitigation());
            });
        }
        
        if (!health.getRecommendations().isEmpty()) {
            System.out.println("\n💡 RECOMMENDATIONS");
            System.out.println("-".repeat(40));
            health.getRecommendations().forEach(rec -> System.out.println("• " + rec));
        }
    }
    
    private static void exportAnalysis(JavaProjectAnalyzer analyzer, String projectPath, 
                                     String format, String outputFile) throws IOException {
        ProjectAnalysisResult result = analyzer.analyzeProject(projectPath);
        ExportFormat exportFormat = ExportFormat.valueOf(format.toUpperCase());
        
        analyzer.exportResults(result, outputFile, exportFormat);
        System.out.println("✅ Analysis exported to: " + outputFile);
    }
    
    private static void printUsage() {
        System.out.println("Java Project Analyzer - Simple CLI");
        System.out.println("=".repeat(60));
        System.out.println("Usage: java SimpleAnalyzerCLI <project-path> [command]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  analyze              - Run full analysis (default)");
        System.out.println("  quick                - Quick summary only");
        System.out.println("  health               - Health check with recommendations");
        System.out.println("  export <fmt> <file>  - Export analysis (JSON|HTML|MARKDOWN|CSV)");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java SimpleAnalyzerCLI /path/to/project");
        System.out.println("  java SimpleAnalyzerCLI /path/to/project health");
        System.out.println("  java SimpleAnalyzerCLI /path/to/project export HTML report.html");
    }
}