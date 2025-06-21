package io.joshuasalcedo.parser.java.analyzer;

import io.joshuasalcedo.parser.common.Analyzer;
import io.joshuasalcedo.parser.common.model.*;
import io.joshuasalcedo.parser.java.model.*;
import io.joshuasalcedo.parser.java.parser.RiskLevel;
import io.joshuasalcedo.parser.common.statistic.StatisticAnalyzer;
import tech.tablesaw.api.Table;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Composite class containing all analyzer implementations
 * to avoid creating multiple separate files
 */
public class CompositeAnalyzers {

    /**
     * Analyzes a ProjectRepresentation to generate ProjectStatistics
     */
    public static class StatisticsAnalyzer implements Analyzer<ProjectStatistics, ProjectRepresentation> {
        @Override
        public ProjectStatistics analyze(ProjectRepresentation project) {
            int totalClasses = 0;
            int totalInterfaces = 0;
            int totalEnums = 0;
            int totalMethods = 0;
            int totalFields = 0;
            int totalLinesOfCode = 0; // This would need actual file reading
            Map<String, Integer> packageDistribution = new HashMap<>();
            
            for (ClassRepresentation clazz : project.getClasses()) {
                // Count types
                if (clazz.isInterface()) {
                    totalInterfaces++;
                } else if (clazz.isEnum()) {
                    totalEnums++;
                } else {
                    totalClasses++;
                }
                
                // Count methods and fields
                totalMethods += clazz.getMethods().size();
                totalFields += clazz.getFields().size();
                
                // Track package distribution
                String pkg = clazz.getPackageName();
                packageDistribution.merge(pkg, 1, Integer::sum);
            }
            
            return new ProjectStatistics(
                totalClasses,
                totalInterfaces,
                totalEnums,
                0, // totalAbstractClasses
                totalMethods,
                totalFields,
                0, // totalConstructors
                0, // totalPublicMethods
                0, // totalPrivateMethods
                0, // totalProtectedMethods
                0, // totalStaticMethods
                0, // totalAnnotations
                totalLinesOfCode,
                new HashMap<>(), // annotationUsage
                packageDistribution,
                new ArrayList<>() // classComplexities
            );
        }
    }

    /**
     * Analyzes a ProjectRepresentation to identify dependencies
     */
    public static class DependencyAnalyzer implements Analyzer<ProjectDependencyResult, ProjectRepresentation> {
        @Override
        public ProjectDependencyResult analyze(ProjectRepresentation project) {
            Map<String, Set<String>> dependencies = new HashMap<>();
            Set<List<String>> circularDeps = new HashSet<>();
            Set<String> unusedClasses = new HashSet<>();
            
            // Build dependency graph
            for (ClassRepresentation clazz : project.getClasses()) {
                String className = clazz.getFullyQualifiedName();
                Set<String> deps = new HashSet<>();
                
                // Add extended types
                deps.addAll(clazz.getExtendedTypes());
                
                // Add implemented interfaces
                deps.addAll(clazz.getImplementedInterfaces());
                
                // Add field types
                for (FieldRepresentation field : clazz.getFields()) {
                    deps.add(field.getType());
                }
                
                // Add method return and parameter types
                for (MethodRepresentation method : clazz.getMethods()) {
                    deps.add(method.getReturnType());
                    for (ParameterRepresentation param : method.getParameters()) {
                        deps.add(param.getType());
                    }
                }
                
                dependencies.put(className, deps);
            }
            
            // Detect circular dependencies (simplified)
            for (String classA : dependencies.keySet()) {
                for (String classB : dependencies.get(classA)) {
                    if (dependencies.containsKey(classB) && 
                        dependencies.get(classB).contains(classA)) {
                        circularDeps.add(Arrays.asList(classA, classB));
                    }
                }
            }
            
            // Find unused classes (no incoming dependencies)
            Set<String> allClasses = project.getClasses().stream()
                .map(ClassRepresentation::getFullyQualifiedName)
                .collect(Collectors.toSet());
            
            Set<String> referencedClasses = dependencies.values().stream()
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
            
            unusedClasses.addAll(allClasses);
            unusedClasses.removeAll(referencedClasses);
            
            return new ProjectDependencyResult(
                dependencies,
                new HashMap<>(), // methodCalls
                new HashMap<>(), // fieldReferences
                new ArrayList<>(circularDeps.stream().map(ArrayList::new).collect(Collectors.toList())),
                unusedClasses,
                new HashMap<>(), // afferentCoupling
                new HashMap<>(), // efferentCoupling
                new HashMap<>(), // instability
                new HashMap<>() // packageDependencies
            );
        }
    }

    /**
     * Analyzes a ProjectRepresentation to calculate code metrics
     */
    public static class MetricsAnalyzer implements Analyzer<MetricsResult, ProjectRepresentation> {
        @Override
        public MetricsResult analyze(ProjectRepresentation project) {
            List<ClassMetrics> classMetricsList = new ArrayList<>();
            Set<CodeDuplication> duplicates = new HashSet<>();
            
            // Calculate metrics for each class
            for (ClassRepresentation clazz : project.getClasses()) {
                ClassMetrics metrics = analyzeClass(clazz);
                classMetricsList.add(metrics);
            }
            
            // Find code duplicates (simplified - would need actual implementation)
            // This is a placeholder
            
            // Calculate project-wide metrics
            ProjectMetrics projectMetrics = calculateProjectMetrics(classMetricsList, project);
            
            return new MetricsResult(
                classMetricsList,
                new ArrayList<>(duplicates), // codeDuplicates
                new ArrayList<>(), // methodUsages
                new HashMap<>(), // mostUsedClasses
                new HashSet<>(), // unusedMethods
                new HashSet<>(), // unusedClasses
                classMetricsList.stream()
                    .collect(Collectors.toMap(
                        cm -> cm.className(),
                        cm -> cm.complexity()
                    )), // classComplexity
                classMetricsList.stream()
                    .collect(Collectors.toMap(
                        cm -> cm.className(),
                        cm -> cm.linesOfCode()
                    )), // linesOfCode
                projectMetrics
            );
        }
        
        private ClassMetrics analyzeClass(ClassRepresentation clazz) {
            // Simplified metrics calculation
            int methodCount = clazz.getMethods().size();
            int fieldCount = clazz.getFields().size();
            double complexity = calculateComplexity(clazz);
            double maintainability = calculateMaintainability(clazz);
            int linesOfCode = estimateLinesOfCode(clazz);
            int efferentCoupling = calculateEfferentCoupling(clazz);
            int afferentCoupling = 0; // Would need project-wide analysis
            double cohesion = calculateCohesion(clazz);
            
            return new ClassMetrics(
                clazz.getFullyQualifiedName(),
                methodCount,
                fieldCount,
                complexity,
                afferentCoupling,
                efferentCoupling,
                cohesion,
                maintainability,
                linesOfCode,
                0 // usageCount
            );
        }
        
        private double calculateComplexity(ClassRepresentation clazz) {
            // Simplified: base complexity + method count
            return 1.0 + clazz.getMethods().size() * 0.5;
        }
        
        private double calculateMaintainability(ClassRepresentation clazz) {
            // Simplified maintainability index
            int volume = clazz.getMethods().size() + clazz.getFields().size();
            return Math.max(0, 100 - volume * 2);
        }
        
        private int estimateLinesOfCode(ClassRepresentation clazz) {
            // Rough estimate
            int lines = 10; // Class declaration
            lines += clazz.getFields().size() * 2;
            lines += clazz.getMethods().size() * 10;
            lines += clazz.getConstructors().size() * 5;
            return lines;
        }
        
        private int calculateEfferentCoupling(ClassRepresentation clazz) {
            Set<String> dependencies = new HashSet<>();
            dependencies.addAll(clazz.getExtendedTypes());
            dependencies.addAll(clazz.getImplementedInterfaces());
            
            for (FieldRepresentation field : clazz.getFields()) {
                dependencies.add(field.getType());
            }
            
            return dependencies.size();
        }
        
        private double calculateCohesion(ClassRepresentation clazz) {
            // Simplified LCOM (Lack of Cohesion of Methods)
            if (clazz.getMethods().isEmpty()) return 1.0;
            return 1.0 / (1.0 + clazz.getMethods().size() * 0.1);
        }
        
        private ProjectMetrics calculateProjectMetrics(List<ClassMetrics> classMetrics, 
                                                       ProjectRepresentation project) {
            double avgComplexity = classMetrics.stream()
                .mapToDouble(ClassMetrics::complexity)
                .average()
                .orElse(0.0);
            
            double avgMaintainability = classMetrics.stream()
                .mapToDouble(ClassMetrics::maintainabilityIndex)
                .average()
                .orElse(0.0);
            
            // Simplified calculations
            return new ProjectMetrics(
                project.getClasses().size(), // totalClasses
                (int) classMetrics.stream().mapToInt(cm -> cm.methodCount()).sum(), // totalMethods
                (int) classMetrics.stream().mapToInt(cm -> cm.linesOfCode()).sum(), // totalLinesOfCode
                avgComplexity,
                avgMaintainability,
                0.05, // duplicationRatio
                0.3,  // codeReuse
                0,    // unusedMethods
                0,    // unusedClasses
                calculateTechnicalDebt(avgComplexity, avgMaintainability)
            );
        }
        
        private double calculateTechnicalDebt(double complexity, double maintainability) {
            return (complexity * 10) + (100 - maintainability);
        }
        
        private List<String> generateRecommendations(double complexity, double maintainability) {
            List<String> recommendations = new ArrayList<>();
            
            if (complexity > 10) {
                recommendations.add("Consider refactoring complex methods to reduce cyclomatic complexity");
            }
            
            if (maintainability < 50) {
                recommendations.add("Improve code maintainability by adding documentation and reducing coupling");
            }
            
            return recommendations;
        }
        
        private String calculateHealthGrade(double complexity, double maintainability) {
            double score = (100 - complexity * 5) * 0.5 + maintainability * 0.5;
            
            if (score >= 90) return "A";
            if (score >= 80) return "B";
            if (score >= 70) return "C";
            if (score >= 60) return "D";
            return "F";
        }
        
    }

    /**
     * Analyzes a ProjectRepresentation to find code patterns
     */
    public static class PatternAnalyzer implements Analyzer<PatternSearchResult, ProjectRepresentation> {
        private final Set<CodePattern> patternsToSearch;
        
        public PatternAnalyzer(Set<CodePattern> patternsToSearch) {
            this.patternsToSearch = patternsToSearch;
        }
        
        @Override
        public PatternSearchResult analyze(ProjectRepresentation project) {
            Map<CodePattern, List<PatternMatch>> matches = new HashMap<>();
            
            for (CodePattern pattern : patternsToSearch) {
                List<PatternMatch> patternMatches = findPattern(pattern, project);
                if (!patternMatches.isEmpty()) {
                    matches.put(pattern, patternMatches);
                }
            }
            
            return new PatternSearchResult(matches);
        }
        
        private List<PatternMatch> findPattern(CodePattern pattern, ProjectRepresentation project) {
            List<PatternMatch> matches = new ArrayList<>();
            
            for (ClassRepresentation clazz : project.getClasses()) {
                // Check if class matches pattern
                if (matchesPattern(clazz, pattern)) {
                    matches.add(new PatternMatch(
                        pattern,
                        clazz.getFullyQualifiedName(),
                        clazz.getFilePath(),
                        clazz.isInterface() ? "interface" : "class",
                        clazz.getName()
                    ));
                }
                
                // Check methods
                for (MethodRepresentation method : clazz.getMethods()) {
                    if (matchesPattern(method, pattern)) {
                        matches.add(new PatternMatch(
                            pattern,
                            clazz.getFullyQualifiedName() + "." + method.getName(),
                            clazz.getFilePath(),
                            "method",
                            method.getName()
                        ));
                    }
                }
            }
            
            return matches;
        }
        
        private boolean matchesPattern(ClassRepresentation clazz, CodePattern pattern) {
            // Simplified pattern matching - would need actual implementation
            switch (pattern.getName()) {
                case "Singleton":
                    return hasSingletonCharacteristics(clazz);
                case "Factory":
                    return hasFactoryCharacteristics(clazz);
                default:
                    return false;
            }
        }
        
        private boolean matchesPattern(MethodRepresentation method, CodePattern pattern) {
            // Simplified pattern matching for methods
            return false;
        }
        
        private boolean hasSingletonCharacteristics(ClassRepresentation clazz) {
            // Check for private constructor and static instance
            boolean hasPrivateConstructor = clazz.getConstructors().stream()
                .anyMatch(c -> "private".equals(c.getVisibility()));
            
            boolean hasStaticInstance = clazz.getFields().stream()
                .anyMatch(f -> f.isStatic() && f.getType().equals(clazz.getName()));
            
            return hasPrivateConstructor && hasStaticInstance;
        }
        
        private boolean hasFactoryCharacteristics(ClassRepresentation clazz) {
            // Check for factory method patterns
            return clazz.getMethods().stream()
                .anyMatch(m -> m.getName().startsWith("create") && m.isStatic());
        }
    }

    /**
     * Analyzes a ProjectRepresentation to assess project health
     */
    public static class HealthAnalyzer implements Analyzer<ProjectHealthAssessment, ProjectAnalysisResult> {
        @Override
        public ProjectHealthAssessment analyze(ProjectAnalysisResult result) {
            double healthScore = calculateHealthScore(result);
            String healthGrade = calculateGrade(healthScore);
            List<String> recommendations = generateHealthRecommendations(result);
            List<ProjectRisk> risks = identifyRisks(result);
            
            return new ProjectHealthAssessment(
                healthScore,
                healthGrade,
                recommendations,
                risks
            );
        }
        
        private double calculateHealthScore(ProjectAnalysisResult result) {
            double score = 100.0;
            
            // Deduct for circular dependencies
            if (result.hasCircularDependencies()) {
                score -= result.getDependencyResult().circularDependencies().size() * 5;
            }
            
            // Deduct for code duplication
            if (result.hasCodeDuplication()) {
                score -= result.getMetricsResult().codeDuplicates().size() * 2;
            }
            
            // Factor in metrics
            if (result.getMetricsResult() != null) {
                double avgMaintainability = result.getMetricsResult().projectMetrics().averageMaintainability();
                score = score * 0.5 + avgMaintainability * 0.5;
            }
            
            return Math.max(0, score);
        }
        
        private String calculateGrade(double score) {
            if (score >= 90) return "A";
            if (score >= 80) return "B";
            if (score >= 70) return "C";
            if (score >= 60) return "D";
            return "F";
        }
        
        private List<String> generateHealthRecommendations(ProjectAnalysisResult result) {
            List<String> recommendations = new ArrayList<>();
            
            if (result.hasCircularDependencies()) {
                recommendations.add("Refactor circular dependencies to improve architecture");
            }
            
            if (result.hasCodeDuplication()) {
                recommendations.add("Extract common code to reduce duplication");
            }
            
            if (result.getMetricsResult() != null) {
                // Add metric-specific recommendations
                if (result.getMetricsResult().projectMetrics().averageComplexity() > 10) {
                    recommendations.add("Consider refactoring complex methods to reduce cyclomatic complexity");
                }
                if (result.getMetricsResult().projectMetrics().averageMaintainability() < 50) {
                    recommendations.add("Improve code maintainability by adding documentation and reducing coupling");
                }
            }
            
            return recommendations;
        }
        
        private List<ProjectRisk> identifyRisks(ProjectAnalysisResult result) {
            List<ProjectRisk> risks = new ArrayList<>();
            
            if (result.hasCircularDependencies()) {
                risks.add(new ProjectRisk(
                    RiskLevel.HIGH,
                    "Architecture",
                    "Circular dependencies detected",
                    "Refactor to break dependency cycles"
                ));
            }
            
            if (result.getMetricsResult() != null) {
                ProjectMetrics pm = result.getMetricsResult().projectMetrics();
                if (pm.averageComplexity() > 15) {
                    risks.add(new ProjectRisk(
                        RiskLevel.MEDIUM,
                        "Complexity",
                        "High average cyclomatic complexity",
                        "Simplify complex methods"
                    ));
                }
            }
            
            return risks;
        }
    }

    /**
     * Analyzes a ProjectRepresentation to create a summary
     */
    public static class SummaryAnalyzer implements Analyzer<ProjectSummary, ProjectRepresentation> {
        @Override
        public ProjectSummary analyze(ProjectRepresentation project) {
            StatisticsAnalyzer statsAnalyzer = new StatisticsAnalyzer();
            ProjectStatistics stats = statsAnalyzer.analyze(project);
            
            return new ProjectSummary(
                project.getName(),
                project.getRootPath(),
                stats.totalClasses(),
                stats.totalInterfaces(),
                stats.totalEnums(),
                stats.totalMethods(),
                stats.totalLinesOfCode(),
                stats.packageDistribution()
            );
        }
    }

    /**
     * Composite analyzer that runs all analyses and produces ProjectAnalysisResult
     */
    public static class CompositeProjectAnalyzer implements Analyzer<ProjectAnalysisResult, ProjectRepresentation> {
        @Override
        public ProjectAnalysisResult analyze(ProjectRepresentation project) {
            long startTime = System.currentTimeMillis();
            
            // Run all analyses
            ProjectStatistics statistics = new StatisticsAnalyzer().analyze(project);
            ProjectDependencyResult dependencies = new DependencyAnalyzer().analyze(project);
            MetricsResult metrics = new MetricsAnalyzer().analyze(project);
            
            // GraphResult would need a separate implementation
            GraphResult graph = null;
            
            long analysisTime = System.currentTimeMillis() - startTime;
            
            return new ProjectAnalysisResult(
                project,
                statistics,
                dependencies,
                metrics,
                graph,
                analysisTime
            );
        }
    }

    /**
     * Analyzer that bridges Java code analysis results with statistical analysis
     * Converts code metrics to statistical data for further analysis
     */
    public static class CodeMetricsToStatisticsAnalyzer implements Analyzer<BasicStatistics, MetricsResult> {
        @Override
        public BasicStatistics analyze(MetricsResult metricsResult) {
            // Extract complexity values from all classes
            List<Double> complexityValues = metricsResult.classMetrics().stream()
                .map(cm -> cm.complexity())
                .collect(Collectors.toList());
            
            // Use the existing BasicStatisticAnalyzer
            return StatisticAnalyzer.forBasicStatistics().analyze(complexityValues);
        }
    }
    
    /**
     * Analyzer that performs correlation analysis on code metrics
     */
    public static class CodeMetricsCorrelationAnalyzer implements Analyzer<CorrelationAnalysis, MetricsResult> {
        @Override
        public CorrelationAnalysis analyze(MetricsResult metricsResult) {
            // Create pairs of metrics for correlation analysis
            List<double[]> metricsPairs = metricsResult.classMetrics().stream()
                .map(cm -> new double[]{
                    cm.complexity(),
                    cm.maintainabilityIndex()
                })
                .collect(Collectors.toList());
            
            return StatisticAnalyzer.forCorrelation().analyze(metricsPairs);
        }
    }
    
    /**
     * Analyzer that identifies outliers in code metrics
     */
    public static class CodeMetricsOutlierAnalyzer implements Analyzer<OutlierAnalysis, MetricsResult> {
        @Override
        public OutlierAnalysis analyze(MetricsResult metricsResult) {
            // Analyze complexity for outliers
            List<Double> complexityValues = metricsResult.classMetrics().stream()
                .map(cm -> cm.complexity())
                .collect(Collectors.toList());
            
            return StatisticAnalyzer.forOutliers().analyze(complexityValues);
        }
    }
    
    /**
     * Analyzer that generates charts from project analysis results
     */
    public static class ProjectMetricsChartAnalyzer implements Analyzer<ChartGenerationResult, ProjectAnalysisResult> {
        @Override
        public ChartGenerationResult analyze(ProjectAnalysisResult result) {
            if (result.getMetricsResult() == null) {
                return new ChartGenerationResult(
                    "",
                    "No metrics data available for chart generation",
                    false,
                    Map.of()
                );
            }
            
            // Create chart data for complexity distribution
            List<Double> complexities = result.getMetricsResult().classMetrics().stream()
                .map(cm -> cm.complexity())
                .collect(Collectors.toList());
            
            List<Double> indices = IntStream.range(0, complexities.size())
                .mapToDouble(i -> (double) i)
                .boxed()
                .collect(Collectors.toList());
            
            ChartData chartData = new ChartData(
                "Code Complexity Distribution",
                "Class Index",
                "Cyclomatic Complexity",
                "bar",
                List.of(new SeriesData("Complexity", indices, complexities))
            );
            
            return StatisticAnalyzer.forChartGeneration().generate(chartData);
        }
    }
    
    /**
     * Analyzer that performs regression analysis on code metrics over time
     * (requires historical data)
     */
    public static class CodeMetricsTrendAnalyzer implements Analyzer<RegressionAnalysis, List<MetricsResult>> {
        @Override
        public RegressionAnalysis analyze(List<MetricsResult> historicalResults) {
            // Create time series data for regression
            List<double[]> timeSeriesData = IntStream.range(0, historicalResults.size())
                .mapToObj(i -> new double[]{
                    (double) i,  // time index
                    historicalResults.get(i).projectMetrics().averageComplexity()
                })
                .collect(Collectors.toList());
            
            return StatisticAnalyzer.forRegression().analyze(timeSeriesData);
        }
    }
    
    /**
     * Analyzer that assesses data quality of project metrics
     */
    public static class MetricsDataQualityAnalyzer implements Analyzer<DataQualityMetrics, MetricsResult> {
        @Override
        public DataQualityMetrics analyze(MetricsResult metricsResult) {
            Table metricsTable = Table.create("Metrics");
            
            // Convert metrics to table format
            DoubleColumn complexityCol = DoubleColumn.create("complexity");
            DoubleColumn maintainabilityCol = DoubleColumn.create("maintainability");
            IntColumn locCol = IntColumn.create("linesOfCode");
            
            for (ClassMetrics cm : metricsResult.classMetrics()) {
                complexityCol.append(cm.complexity());
                maintainabilityCol.append(cm.maintainabilityIndex());
                locCol.append(cm.linesOfCode());
            }
            
            metricsTable.addColumns(complexityCol, maintainabilityCol, locCol);
            
            return StatisticAnalyzer.forDataQuality().analyze(metricsTable);
        }
    }
    
    /**
     * Factory method to create specific analyzers
     */
    public static <T, R> Analyzer<T, R> create(Class<T> resultType, Class<R> inputType) {
        // Java code analysis analyzers
        if (resultType == ProjectStatistics.class && inputType == ProjectRepresentation.class) {
            return (Analyzer<T, R>) new StatisticsAnalyzer();
        } else if (resultType == ProjectDependencyResult.class && inputType == ProjectRepresentation.class) {
            return (Analyzer<T, R>) new DependencyAnalyzer();
        } else if (resultType == MetricsResult.class && inputType == ProjectRepresentation.class) {
            return (Analyzer<T, R>) new MetricsAnalyzer();
        } else if (resultType == ProjectSummary.class && inputType == ProjectRepresentation.class) {
            return (Analyzer<T, R>) new SummaryAnalyzer();
        } else if (resultType == ProjectHealthAssessment.class && inputType == ProjectAnalysisResult.class) {
            return (Analyzer<T, R>) new HealthAnalyzer();
        } else if (resultType == ProjectAnalysisResult.class && inputType == ProjectRepresentation.class) {
            return (Analyzer<T, R>) new CompositeProjectAnalyzer();
        }
        // Statistical analyzers for code metrics
        else if (resultType == BasicStatistics.class && inputType == MetricsResult.class) {
            return (Analyzer<T, R>) new CodeMetricsToStatisticsAnalyzer();
        } else if (resultType == CorrelationAnalysis.class && inputType == MetricsResult.class) {
            return (Analyzer<T, R>) new CodeMetricsCorrelationAnalyzer();
        } else if (resultType == OutlierAnalysis.class && inputType == MetricsResult.class) {
            return (Analyzer<T, R>) new CodeMetricsOutlierAnalyzer();
        } else if (resultType == ChartGenerationResult.class && inputType == ProjectAnalysisResult.class) {
            return (Analyzer<T, R>) new ProjectMetricsChartAnalyzer();
        } else if (resultType == DataQualityMetrics.class && inputType == MetricsResult.class) {
            return (Analyzer<T, R>) new MetricsDataQualityAnalyzer();
        }
        
        throw new IllegalArgumentException("No analyzer found for " + resultType + " from " + inputType);
    }
    
    /**
     * Usage examples for the composite analyzers
     */
    public static class Examples {
        
        public static void demonstrateUsage(ProjectRepresentation project) {
            // 1. Run complete Java code analysis
            ProjectAnalysisResult fullAnalysis = new CompositeProjectAnalyzer().analyze(project);
            
            // 2. Extract specific results
            MetricsResult metrics = fullAnalysis.getMetricsResult();
            
            // 3. Perform statistical analysis on code metrics
            BasicStatistics complexityStats = new CodeMetricsToStatisticsAnalyzer().analyze(metrics);
            System.out.println("Average complexity: " + complexityStats.mean());
            System.out.println("Complexity std dev: " + complexityStats.standardDeviation());
            
            // 4. Find outlier classes (unusually complex)
            OutlierAnalysis outliers = new CodeMetricsOutlierAnalyzer().analyze(metrics);
            System.out.println("Number of outlier classes: " + outliers.outliers().size());
            
            // 5. Check correlation between complexity and maintainability
            CorrelationAnalysis correlation = new CodeMetricsCorrelationAnalyzer().analyze(metrics);
            System.out.println("Correlation coefficient: " + correlation.pearsonCoefficient());
            
            // 6. Generate visualization
            ChartGenerationResult chart = new ProjectMetricsChartAnalyzer().analyze(fullAnalysis);
            if (chart.success()) {
                System.out.println("Chart saved to: " + chart.filePath());
            }
            
            // 7. Using factory method for type safety
            Analyzer<BasicStatistics, MetricsResult> statsAnalyzer = 
                CompositeAnalyzers.create(BasicStatistics.class, MetricsResult.class);
            BasicStatistics stats = statsAnalyzer.analyze(metrics);
            
            // 8. Chain multiple analyses
            ProjectAnalysisResult result = new CompositeProjectAnalyzer().analyze(project);
            ProjectHealthAssessment health = new HealthAnalyzer().analyze(result);
            
            if (!health.isHealthy()) {
                // Generate detailed statistical report
                OutlierAnalysis complexityOutliers = new CodeMetricsOutlierAnalyzer()
                    .analyze(result.getMetricsResult());
                
                // Focus refactoring efforts on outlier classes
                System.out.println("Classes needing refactoring: " + complexityOutliers.outlierIndices());
            }
        }
    }
}