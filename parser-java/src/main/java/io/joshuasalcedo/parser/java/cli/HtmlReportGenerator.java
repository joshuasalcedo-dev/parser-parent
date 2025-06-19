package io.joshuasalcedo.parser.java.cli;

import io.joshuasalcedo.parser.java.model.*;
import io.joshuasalcedo.parser.java.result.*;
import io.joshuasalcedo.parser.java.parser.JavaProjectParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates HTML reports for metrics analysis using FreeMarker templates
 */
public class HtmlReportGenerator {
    
    private final Configuration freemarkerConfig;
    
    public HtmlReportGenerator() {
        this.freemarkerConfig = new Configuration(Configuration.VERSION_2_3_34);
        freemarkerConfig.setClassForTemplateLoading(this.getClass(), "/templates");
        freemarkerConfig.setDefaultEncoding("UTF-8");
    }
    
    public void generateReport(MetricsResult result, ProjectRepresentation project, File outputFile) throws IOException {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>\n<html>\n<head>\n");
        html.append("<title>").append(project.getName()).append(" - Metrics Report</title>\n");
        html.append(getStyles());
        html.append(getScripts());
        html.append("</head>\n<body>\n");
        
        // Header
        html.append("<div class=\"header\">\n");
        html.append("<h1>Code Metrics Report</h1>\n");
        html.append("<div class=\"project-info\">\n");
        html.append("<span class=\"project-name\">").append(project.getName()).append("</span>\n");
        html.append("<span class=\"timestamp\">Generated: ").append(getCurrentTimestamp()).append("</span>\n");
        html.append("</div>\n</div>\n");
        
        // Summary cards
        html.append("<div class=\"summary-cards\">\n");
        html.append(createSummaryCard("Health Grade", result.projectMetrics().getProjectHealthGrade(), "grade"));
        html.append(createSummaryCard("Classes", String.valueOf(result.projectMetrics().totalClasses()), "info"));
        html.append(createSummaryCard("Methods", String.valueOf(result.projectMetrics().totalMethods()), "info"));
        html.append(createSummaryCard("Lines of Code", String.valueOf(result.projectMetrics().totalLinesOfCode()), "info"));
        html.append("</div>\n");
        
        // Metrics charts
        html.append("<div class=\"metrics-section\">\n");
        html.append("<h2>Code Quality Metrics</h2>\n");
        html.append("<div class=\"charts-container\">\n");
        html.append("<canvas id=\"complexityChart\"></canvas>\n");
        html.append("<canvas id=\"maintainabilityChart\"></canvas>\n");
        html.append("<canvas id=\"duplicationChart\"></canvas>\n");
        html.append("</div>\n</div>\n");
        
        // Complex classes table
        html.append("<div class=\"table-section\">\n");
        html.append("<h2>Most Complex Classes</h2>\n");
        html.append(createComplexClassesTable(result.getMostComplexClasses(10)));
        html.append("</div>\n");
        
        // Code duplicates
        if (!result.codeDuplicates().isEmpty()) {
            html.append("<div class=\"table-section\">\n");
            html.append("<h2>Code Duplicates</h2>\n");
            html.append(createDuplicatesTable(result.getMostImpactfulDuplicates(10)));
            html.append("</div>\n");
        }
        
        // Recommendations
        html.append("<div class=\"recommendations-section\">\n");
        html.append("<h2>Recommendations</h2>\n");
        html.append("<ul>\n");
        result.projectMetrics().getRecommendations().forEach(rec -> 
            html.append("<li>").append(rec).append("</li>\n"));
        html.append("</ul>\n</div>\n");
        
        // Chart data script
        html.append("<script>\n");
        html.append(generateChartData(result));
        html.append("</script>\n");
        
        html.append("</body>\n</html>");
        
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(html.toString());
        }
    }
    
    private String getStyles() {
        return """
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif;
                    margin: 0;
                    padding: 0;
                    background-color: #f5f5f5;
                }
                .header {
                    background-color: #2c3e50;
                    color: white;
                    padding: 20px;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                }
                .header h1 {
                    margin: 0;
                    font-size: 24px;
                }
                .project-info {
                    margin-top: 10px;
                    font-size: 14px;
                    opacity: 0.8;
                }
                .project-name {
                    margin-right: 20px;
                }
                .summary-cards {
                    display: flex;
                    gap: 20px;
                    padding: 20px;
                    flex-wrap: wrap;
                }
                .summary-card {
                    background: white;
                    border-radius: 8px;
                    padding: 20px;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    min-width: 200px;
                    text-align: center;
                }
                .summary-card h3 {
                    margin: 0;
                    color: #666;
                    font-size: 14px;
                    font-weight: normal;
                }
                .summary-card .value {
                    font-size: 36px;
                    font-weight: bold;
                    margin: 10px 0;
                }
                .summary-card.grade .value {
                    color: #27ae60;
                }
                .metrics-section, .table-section, .recommendations-section {
                    margin: 20px;
                    background: white;
                    border-radius: 8px;
                    padding: 20px;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                }
                .charts-container {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
                    gap: 20px;
                    margin-top: 20px;
                }
                canvas {
                    max-height: 300px;
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
                tr:hover {
                    background-color: #f9f9f9;
                }
                .recommendations-section ul {
                    line-height: 1.8;
                }
                .complexity-high { color: #e74c3c; }
                .complexity-medium { color: #f39c12; }
                .complexity-low { color: #27ae60; }
            </style>
            """;
    }
    
    private String getScripts() {
        return """
            <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
            """;
    }
    
    private String createSummaryCard(String title, String value, String type) {
        return String.format(
            "<div class=\"summary-card %s\">\n<h3>%s</h3>\n<div class=\"value\">%s</div>\n</div>\n",
            type, title, value
        );
    }
    
    private String createComplexClassesTable(List<ClassMetrics> classes) {
        StringBuilder table = new StringBuilder();
        table.append("<table>\n<thead>\n<tr>\n");
        table.append("<th>Class</th>\n");
        table.append("<th>Complexity</th>\n");
        table.append("<th>Methods</th>\n");
        table.append("<th>Maintainability</th>\n");
        table.append("<th>Level</th>\n");
        table.append("</tr>\n</thead>\n<tbody>\n");
        
        for (ClassMetrics cls : classes) {
            table.append("<tr>\n");
            table.append("<td>").append(getSimpleName(cls.className())).append("</td>\n");
            table.append("<td>").append(String.format("%.1f", cls.complexity())).append("</td>\n");
            table.append("<td>").append(cls.methodCount()).append("</td>\n");
            table.append("<td>").append(String.format("%.1f", cls.maintainabilityIndex())).append("</td>\n");
            table.append("<td class=\"").append(getComplexityClass(cls.complexity())).append("\">")
                 .append(cls.getComplexityLevel()).append("</td>\n");
            table.append("</tr>\n");
        }
        
        table.append("</tbody>\n</table>\n");
        return table.toString();
    }
    
    private String createDuplicatesTable(List<CodeDuplication> duplicates) {
        StringBuilder table = new StringBuilder();
        table.append("<table>\n<thead>\n<tr>\n");
        table.append("<th>Type</th>\n");
        table.append("<th>Methods</th>\n");
        table.append("<th>Lines</th>\n");
        table.append("<th>Impact</th>\n");
        table.append("</tr>\n</thead>\n<tbody>\n");
        
        for (CodeDuplication dup : duplicates) {
            table.append("<tr>\n");
            table.append("<td>").append(dup.getDuplicationType()).append("</td>\n");
            table.append("<td>").append(dup.methods().size()).append(" methods</td>\n");
            table.append("<td>").append(dup.lineCount()).append("</td>\n");
            table.append("<td>").append(String.format("%.1f", dup.impact())).append("</td>\n");
            table.append("</tr>\n");
        }
        
        table.append("</tbody>\n</table>\n");
        return table.toString();
    }
    
    private String generateChartData(MetricsResult result) {
        // Prepare data for charts
        List<ClassMetrics> topComplex = result.getMostComplexClasses(10);
        
        String complexityLabels = topComplex.stream()
            .map(c -> "'" + getSimpleName(c.className()) + "'")
            .collect(Collectors.joining(","));
        
        String complexityData = topComplex.stream()
            .map(c -> String.valueOf((int) c.complexity()))
            .collect(Collectors.joining(","));
        
        String maintainabilityData = topComplex.stream()
            .map(c -> String.valueOf((int) c.maintainabilityIndex()))
            .collect(Collectors.joining(","));
        
        return String.format("""
            // Complexity Chart
            new Chart(document.getElementById('complexityChart'), {
                type: 'bar',
                data: {
                    labels: [%s],
                    datasets: [{
                        label: 'Complexity',
                        data: [%s],
                        backgroundColor: 'rgba(231, 76, 60, 0.6)'
                    }]
                },
                options: {
                    responsive: true,
                    plugins: {
                        title: {
                            display: true,
                            text: 'Class Complexity'
                        }
                    }
                }
            });
            
            // Maintainability Chart
            new Chart(document.getElementById('maintainabilityChart'), {
                type: 'bar',
                data: {
                    labels: [%s],
                    datasets: [{
                        label: 'Maintainability Index',
                        data: [%s],
                        backgroundColor: 'rgba(39, 174, 96, 0.6)'
                    }]
                },
                options: {
                    responsive: true,
                    plugins: {
                        title: {
                            display: true,
                            text: 'Maintainability Index'
                        }
                    }
                }
            });
            
            // Duplication Chart
            new Chart(document.getElementById('duplicationChart'), {
                type: 'doughnut',
                data: {
                    labels: ['Unique Code', 'Duplicated Code'],
                    datasets: [{
                        data: [%d, %d],
                        backgroundColor: ['rgba(52, 152, 219, 0.6)', 'rgba(231, 76, 60, 0.6)']
                    }]
                },
                options: {
                    responsive: true,
                    plugins: {
                        title: {
                            display: true,
                            text: 'Code Duplication'
                        }
                    }
                }
            });
            """,
            complexityLabels, complexityData,
            complexityLabels, maintainabilityData,
            (int)((1 - result.projectMetrics().duplicationRatio()) * 100),
            (int)(result.projectMetrics().duplicationRatio() * 100)
        );
    }
    
    private String getSimpleName(String fullyQualifiedName) {
        int lastDot = fullyQualifiedName.lastIndexOf('.');
        return lastDot > 0 ? fullyQualifiedName.substring(lastDot + 1) : fullyQualifiedName;
    }
    
    private String getComplexityClass(double complexity) {
        if (complexity < 10) return "complexity-low";
        if (complexity < 20) return "complexity-medium";
        return "complexity-high";
    }
    
    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    public void generateComprehensiveReport(ProjectRepresentation project, ProjectStatistics statistics, 
                                          String outputPath, String projectPath) throws IOException {
        try {
            Template template = freemarkerConfig.getTemplate("report.ftl");
            
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("projectName", project.getName());
            dataModel.put("timestamp", getCurrentTimestamp());
            dataModel.put("statistics", statistics);
            dataModel.put("classComplexities", statistics.classComplexities());
            dataModel.put("packageDistribution", statistics.packageDistribution());
            
            // Scan for submodules
            List<SubmoduleAnalysis> submodules = scanSubmodules(new File(projectPath));
            dataModel.put("submodules", submodules);
            
            try (FileWriter writer = new FileWriter(outputPath)) {
                template.process(dataModel, writer);
            }
            
            System.out.println("✅ Comprehensive HTML report generated: " + outputPath);
            
        } catch (TemplateException e) {
            throw new IOException("Error processing FreeMarker template", e);
        }
    }
    
    private List<SubmoduleAnalysis> scanSubmodules(File projectRoot) {
        List<SubmoduleAnalysis> submodules = new ArrayList<>();
        
        try {
            Files.walk(projectRoot.toPath())
                .filter(path -> path.getFileName().toString().equals("pom.xml"))
                .filter(path -> !path.equals(projectRoot.toPath().resolve("pom.xml")))
                .forEach(pomPath -> {
                    File moduleDir = pomPath.getParent().toFile();
                    if (isJavaModule(moduleDir)) {
                        try {
                            SubmoduleAnalysis analysis = analyzeSubmodule(moduleDir);
                            if (analysis != null) {
                                submodules.add(analysis);
                            }
                        } catch (Exception e) {
                            System.err.println("Warning: Failed to analyze submodule " + moduleDir.getName() + ": " + e.getMessage());
                        }
                    }
                });
        } catch (IOException e) {
            System.err.println("Warning: Error scanning for submodules: " + e.getMessage());
        }
        
        return submodules;
    }
    
    private boolean isJavaModule(File directory) {
        File srcDir = new File(directory, "src/main/java");
        return srcDir.exists() && srcDir.isDirectory();
    }
    
    private SubmoduleAnalysis analyzeSubmodule(File moduleDir) {
        try {
            JavaProjectParser parser = new JavaProjectParser();
            ProjectRepresentation project = parser.parseProject(moduleDir.getAbsolutePath());
            
            if (project == null || project.getClasses().isEmpty()) {
                return null;
            }
            
            // Calculate statistics for this submodule
            ProjectStatistics stats = calculateModuleStatistics(project);
            
            // Get top 5 most complex classes
            List<ClassComplexity> topComplex = stats.classComplexities().stream()
                .sorted((a, b) -> Integer.compare(b.dependencyCount(), a.dependencyCount()))
                .limit(5)
                .collect(Collectors.toList());
            
            return new SubmoduleAnalysis(
                moduleDir.getName(),
                moduleDir.getAbsolutePath(),
                stats,
                topComplex
            );
            
        } catch (Exception e) {
            System.err.println("Error analyzing submodule " + moduleDir.getName() + ": " + e.getMessage());
            return null;
        }
    }
    
    private ProjectStatistics calculateModuleStatistics(ProjectRepresentation project) {
        int totalClasses = 0, totalInterfaces = 0, totalEnums = 0, totalAbstractClasses = 0;
        int totalMethods = 0, totalFields = 0, totalConstructors = 0;
        int totalPublicMethods = 0, totalPrivateMethods = 0, totalProtectedMethods = 0, totalStaticMethods = 0;
        int totalAnnotations = 0, totalLinesOfCode = 0;
        
        Map<String, Integer> annotationUsage = new HashMap<>();
        Map<String, Integer> packageDistribution = new HashMap<>();
        List<ClassComplexity> classComplexities = new ArrayList<>();
        
        for (ClassRepresentation cls : project.getClasses()) {
            // Count class types
            if (cls.isInterface()) totalInterfaces++;
            else if (cls.isEnum()) totalEnums++;
            else if (cls.isAbstract()) totalAbstractClasses++;
            else totalClasses++;
            
            // Count methods and fields
            totalMethods += cls.getMethods().size();
            totalFields += cls.getFields().size();
            totalConstructors += cls.getConstructors().size();
            
            // Package distribution
            String packageName = cls.getPackageName().isEmpty() ? "default" : cls.getPackageName();
            packageDistribution.merge(packageName, 1, Integer::sum);
            
            // Class complexity (using method count as a simple complexity metric)
            classComplexities.add(new ClassComplexity(
                cls.getName(),
                cls.getMethods().size(),
                cls.getFields().size(),
                cls.getMethods().size() + cls.getFields().size() // Simple dependency count
            ));
            
            // Estimate lines of code (rough estimate based on methods and fields)
            totalLinesOfCode += cls.getMethods().size() * 10 + cls.getFields().size() * 2;
        }
        
        return new ProjectStatistics(
            totalClasses, totalInterfaces, totalEnums, totalAbstractClasses,
            totalMethods, totalFields, totalConstructors,
            totalPublicMethods, totalPrivateMethods, totalProtectedMethods, totalStaticMethods,
            totalAnnotations, totalLinesOfCode,
            annotationUsage, packageDistribution, classComplexities
        );
    }
}

