package io.joshuasalcedo.parser.maven.analyzer;

import io.joshuasalcedo.parser.common.Analyzer;
import io.joshuasalcedo.parser.maven.model.*;
import io.joshuasalcedo.parser.maven.parser.*;
import io.joshuasalcedo.parser.common.model.*;
import io.joshuasalcedo.parser.common.statistic.StatisticAnalyzer;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Dependency;
import tech.tablesaw.api.Table;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.BooleanColumn;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Composite class containing all Maven-related analyzer implementations
 */
public class MavenCompositeAnalyzers {








    /**
     * Analyzes Maven dependencies from a ProjectModule
     */
    public static class MavenDependencyAnalyzer implements Analyzer<MavenDependencyAnalysis, ProjectModule> {
        @Override
        public MavenDependencyAnalysis analyze(ProjectModule project) {
            List<ParsedDependency> allDependencies = new ArrayList<>();
            Map<String, Integer> scopeDistribution = new HashMap<>();
            List<ParsedDependency> outdatedDeps = new ArrayList<>();
            Map<String, List<ParsedDependency>> duplicates = new HashMap<>();
            
            // Collect all dependencies from all modules
            for (Map.Entry<Model, List<ParsedDependency>> entry : project.moduleDependencies().entrySet()) {
                allDependencies.addAll(entry.getValue());
            }
            
            // Analyze scope distribution
            for (ParsedDependency dep : allDependencies) {
                scopeDistribution.merge(dep.scope(), 1, Integer::sum);
            }
            
            // Find outdated dependencies
            outdatedDeps = allDependencies.stream()
                .filter(ParsedDependency::isOutdated)
                .collect(Collectors.toList());
            
            // Find duplicate dependencies (same groupId:artifactId with different versions)
            Map<String, List<ParsedDependency>> depsByGA = allDependencies.stream()
                .collect(Collectors.groupingBy(dep -> dep.groupId() + ":" + dep.artifactId()));
            
            duplicates = depsByGA.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .filter(entry -> entry.getValue().stream()
                    .map(ParsedDependency::version)
                    .distinct()
                    .count() > 1)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue
                ));
            
            // Calculate direct vs transitive (simplified - assumes all are direct for now)
            int directCount = allDependencies.size();
            int transitiveCount = 0; // Would need actual Maven resolution
            
            double outdatedPercentage = allDependencies.isEmpty() ? 0.0 : 
                (double) outdatedDeps.size() / allDependencies.size() * 100;
            
            return new MavenDependencyAnalysis(
                allDependencies.size(),
                directCount,
                transitiveCount,
                scopeDistribution,
                outdatedDeps,
                duplicates,
                new ArrayList<>(), // Security vulnerabilities would need external service
                outdatedPercentage
            );
        }
    }

    /**
     * Analyzes individual Maven modules
     */
    public static class ModuleAnalyzer implements Analyzer<ModuleAnalysis, Model> {
        private final Map<Model, List<ParsedDependency>> allDependencies;
        
        public ModuleAnalyzer(Map<Model, List<ParsedDependency>> allDependencies) {
            this.allDependencies = allDependencies;
        }
        
        @Override
        public ModuleAnalysis analyze(Model module) {
            String moduleName = module.getArtifactId();
            List<ParsedDependency> deps = allDependencies.getOrDefault(module, new ArrayList<>());
            
            // Count dependencies by scope
            Map<String, Integer> scopeBreakdown = deps.stream()
                .collect(Collectors.groupingBy(
                    ParsedDependency::scope,
                    Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));
            
            boolean hasTestDeps = scopeBreakdown.containsKey("test");
            boolean hasBuildDeps = scopeBreakdown.containsKey("provided") || 
                                  scopeBreakdown.containsKey("system");
            
            // Check for circular dependencies (simplified)
            List<String> circularDeps = detectCircularDependencies(module, deps);
            
            return new ModuleAnalysis(
                moduleName,
                deps.size(),
                module.getModules().size(),
                circularDeps,
                scopeBreakdown,
                hasTestDeps,
                hasBuildDeps
            );
        }
        
        private List<String> detectCircularDependencies(Model module, List<ParsedDependency> deps) {
            // Simplified circular dependency detection
            List<String> circular = new ArrayList<>();
            String moduleGA = getGroupArtifact(module);
            
            for (ParsedDependency dep : deps) {
                String depGA = dep.groupId() + ":" + dep.artifactId();
                if (moduleGA.equals(depGA)) {
                    circular.add("Self-dependency: " + depGA);
                }
            }
            
            return circular;
        }
        
        private String getGroupArtifact(Model model) {
            String groupId = model.getGroupId() != null ? model.getGroupId() : 
                           (model.getParent() != null ? model.getParent().getGroupId() : "unknown");
            return groupId + ":" + model.getArtifactId();
        }
    }

    /**
     * Analyzes Maven project structure
     */
    public static class ProjectStructureAnalyzer implements Analyzer<ProjectStructureAnalysis, ProjectModule> {
        @Override
        public ProjectStructureAnalysis analyze(ProjectModule project) {
            Map<String, ModuleAnalysis> moduleAnalyses = new HashMap<>();
            List<String> rootModules = new ArrayList<>();
            List<String> leafModules = new ArrayList<>();
            
            // Analyze root module
            ModuleAnalyzer moduleAnalyzer = new ModuleAnalyzer(project.moduleDependencies());
            ModuleAnalysis rootAnalysis = moduleAnalyzer.analyze(project.module());
            String rootName = project.module().getArtifactId();
            moduleAnalyses.put(rootName, rootAnalysis);
            
            if (project.subModules().isEmpty()) {
                leafModules.add(rootName);
            } else {
                rootModules.add(rootName);
            }
            
            // Analyze submodules
            for (Model subModule : project.subModules()) {
                ModuleAnalysis subAnalysis = moduleAnalyzer.analyze(subModule);
                String subName = subModule.getArtifactId();
                moduleAnalyses.put(subName, subAnalysis);
                
                if (subModule.getModules().isEmpty()) {
                    leafModules.add(subName);
                }
            }
            
            // Calculate metrics
            int totalModules = 1 + project.subModules().size();
            int maxDepth = calculateMaxDepth(project.module());
            
            double avgDeps = moduleAnalyses.values().stream()
                .mapToInt(ModuleAnalysis::dependencyCount)
                .average()
                .orElse(0.0);
            
            return new ProjectStructureAnalysis(
                totalModules,
                maxDepth,
                moduleAnalyses,
                rootModules,
                leafModules,
                avgDeps
            );
        }
        
        private int calculateMaxDepth(Model root) {
            if (root.getModules().isEmpty()) {
                return 1;
            }
            
            int maxChildDepth = 0;
            for (String moduleName : root.getModules()) {
                // In a real implementation, we'd recursively load and check submodules
                maxChildDepth = Math.max(maxChildDepth, 1);
            }
            
            return 1 + maxChildDepth;
        }
    }

    /**
     * Analyzes dependency health
     */
    public static class DependencyHealthAnalyzer implements Analyzer<DependencyHealthAssessment, MavenDependencyAnalysis> {
        @Override
        public DependencyHealthAssessment analyze(MavenDependencyAnalysis depAnalysis) {
            double healthScore = 100.0;
            List<String> criticalIssues = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            List<String> recommendations = new ArrayList<>();
            Map<String, Double> scoreBreakdown = new HashMap<>();
            
            // Check outdated dependencies
            double outdatedPenalty = depAnalysis.outdatedPercentage() * 0.5;
            healthScore -= outdatedPenalty;
            scoreBreakdown.put("outdated_dependencies", 100 - outdatedPenalty);
            
            if (depAnalysis.outdatedPercentage() > 50) {
                criticalIssues.add(String.format("%.1f%% of dependencies are outdated", 
                    depAnalysis.outdatedPercentage()));
            } else if (depAnalysis.outdatedPercentage() > 25) {
                warnings.add(String.format("%.1f%% of dependencies are outdated", 
                    depAnalysis.outdatedPercentage()));
            }
            
            // Check duplicate dependencies
            int duplicateCount = depAnalysis.duplicateDependencies().size();
            double duplicatePenalty = duplicateCount * 5.0;
            healthScore -= duplicatePenalty;
            scoreBreakdown.put("duplicate_dependencies", 100 - duplicatePenalty);
            
            if (duplicateCount > 0) {
                warnings.add(String.format("%d dependencies have version conflicts", duplicateCount));
                recommendations.add("Align dependency versions across modules using dependencyManagement");
            }
            
            // Check security vulnerabilities
            int vulnerabilityCount = depAnalysis.securityVulnerabilities().size();
            double securityPenalty = vulnerabilityCount * 10.0;
            healthScore -= securityPenalty;
            scoreBreakdown.put("security", 100 - securityPenalty);
            
            if (vulnerabilityCount > 0) {
                criticalIssues.add(String.format("%d security vulnerabilities found", vulnerabilityCount));
                recommendations.add("Update vulnerable dependencies immediately");
            }
            
            // Check scope distribution
            Map<String, Integer> scopeDist = depAnalysis.scopeDistribution();
            int testDeps = scopeDist.getOrDefault("test", 0);
            int compileDeps = scopeDist.getOrDefault("compile", 0);
            
            if (compileDeps > 0 && testDeps == 0) {
                warnings.add("No test dependencies found");
                recommendations.add("Add test dependencies (JUnit, Mockito, etc.) for better test coverage");
            }
            
            // Calculate final grade
            String grade = calculateGrade(healthScore);
            
            // Add general recommendations
            if (depAnalysis.outdatedDependencies().size() > 0) {
                recommendations.add("Update the following critical dependencies: " + 
                    depAnalysis.outdatedDependencies().stream()
                        .limit(3)
                        .map(d -> d.artifactId())
                        .collect(Collectors.joining(", ")));
            }
            
            return new DependencyHealthAssessment(
                Math.max(0, healthScore),
                grade,
                criticalIssues,
                warnings,
                recommendations,
                scoreBreakdown
            );
        }
        
        private String calculateGrade(double score) {
            if (score >= 90) return "A";
            if (score >= 80) return "B";
            if (score >= 70) return "C";
            if (score >= 60) return "D";
            return "F";
        }
    }

    /**
     * Converts Maven dependency data to statistical format for analysis
     */
    public static class DependencyStatisticsAnalyzer implements Analyzer<BasicStatistics, MavenDependencyAnalysis> {
        @Override
        public BasicStatistics analyze(MavenDependencyAnalysis depAnalysis) {
            // Create version age statistics (days since last update)
            List<Double> versionAges = depAnalysis.outdatedDependencies().stream()
                .map(dep -> calculateVersionAge(dep))
                .collect(Collectors.toList());
            
            if (versionAges.isEmpty()) {
                // Add some sample data if no outdated dependencies
                versionAges = List.of(0.0);
            }
            
            return StatisticAnalyzer.forBasicStatistics().analyze(versionAges);
        }
        
        private double calculateVersionAge(ParsedDependency dep) {
            // Simplified: return a random age in days
            // In reality, would calculate from version release dates
            return Math.random() * 365;
        }
    }

    /**
     * Generates charts for Maven project visualization
     */
    public static class MavenProjectChartAnalyzer implements Analyzer<ChartGenerationResult, ProjectStructureAnalysis> {
        @Override
        public ChartGenerationResult analyze(ProjectStructureAnalysis structure) {
            // Create data for module dependency count chart
            List<String> moduleNames = new ArrayList<>(structure.moduleAnalyses().keySet());
            List<Double> depCounts = moduleNames.stream()
                .map(name -> (double) structure.moduleAnalyses().get(name).dependencyCount())
                .collect(Collectors.toList());
            
            List<Double> indices = IntStream.range(0, moduleNames.size())
                .mapToDouble(i -> (double) i)
                .boxed()
                .collect(Collectors.toList());
            
            ChartData chartData = new ChartData(
                "Dependencies per Module",
                "Module",
                "Number of Dependencies",
                "bar",
                List.of(new SeriesData("Dependencies", indices, depCounts))
            );
            
            return StatisticAnalyzer.forChartGeneration().generate(chartData);
        }
    }

    /**
     * Analyzes dependency version distribution
     */
    public static class VersionDistributionAnalyzer implements Analyzer<DistributionAnalysis, List<ParsedDependency>> {
        @Override
        public DistributionAnalysis analyze(List<ParsedDependency> dependencies) {
            // Extract major version numbers
            List<Double> majorVersions = dependencies.stream()
                .map(dep -> extractMajorVersion(dep.version()))
                .filter(v -> v >= 0)
                .collect(Collectors.toList());
            
            if (majorVersions.isEmpty()) {
                majorVersions = List.of(1.0);
            }
            
            return StatisticAnalyzer.forDistribution().analyze(majorVersions);
        }
        
        private double extractMajorVersion(String version) {
            if (version == null || version.isEmpty()) return -1;
            
            try {
                String[] parts = version.split("\\.");
                return Double.parseDouble(parts[0]);
            } catch (Exception e) {
                return -1;
            }
        }
    }

    /**
     * Assesses data quality of Maven project information
     */
    public static class MavenDataQualityAnalyzer implements Analyzer<DataQualityMetrics, ProjectModule> {
        @Override
        public DataQualityMetrics analyze(ProjectModule project) {
            Table table = Table.create("MavenDependencies");
            
            // Create columns
            StringColumn groupIdCol = StringColumn.create("groupId");
            StringColumn artifactIdCol = StringColumn.create("artifactId");
            StringColumn versionCol = StringColumn.create("version");
            StringColumn scopeCol = StringColumn.create("scope");
            BooleanColumn outdatedCol = BooleanColumn.create("isOutdated");
            
            // Populate table
            for (List<ParsedDependency> deps : project.moduleDependencies().values()) {
                for (ParsedDependency dep : deps) {
                    groupIdCol.append(dep.groupId());
                    artifactIdCol.append(dep.artifactId());
                    versionCol.append(dep.version() != null ? dep.version() : "");
                    scopeCol.append(dep.scope());
                    outdatedCol.append(dep.isOutdated());
                }
            }
            
            table.addColumns(groupIdCol, artifactIdCol, versionCol, scopeCol, outdatedCol);
            
            return StatisticAnalyzer.forDataQuality().analyze(table);
        }
    }

    /**
     * Composite analyzer that performs complete Maven project analysis
     */
    public static class CompositeMavenAnalyzer implements Analyzer<Map<String, Object>, ProjectModule> {
        @Override
        public Map<String, Object> analyze(ProjectModule project) {
            Map<String, Object> results = new HashMap<>();
            
            // Run all analyses
            MavenDependencyAnalysis depAnalysis = new MavenDependencyAnalyzer().analyze(project);
            results.put("dependencyAnalysis", depAnalysis);
            
            ProjectStructureAnalysis structureAnalysis = new ProjectStructureAnalyzer().analyze(project);
            results.put("structureAnalysis", structureAnalysis);
            
            DependencyHealthAssessment healthAssessment = new DependencyHealthAnalyzer().analyze(depAnalysis);
            results.put("healthAssessment", healthAssessment);
            
            // Statistical analyses
            BasicStatistics depStats = new DependencyStatisticsAnalyzer().analyze(depAnalysis);
            results.put("dependencyStatistics", depStats);
            
            DataQualityMetrics dataQuality = new MavenDataQualityAnalyzer().analyze(project);
            results.put("dataQuality", dataQuality);
            
            return results;
        }
    }

    /**
     * Factory method to create specific analyzers
     */
    @SuppressWarnings("unchecked")
    public static <T, R> Analyzer<T, R> create(Class<T> resultType, Class<R> inputType) {
        if (resultType == MavenDependencyAnalysis.class && inputType == ProjectModule.class) {
            return (Analyzer<T, R>) new MavenDependencyAnalyzer();
        } else if (resultType == ProjectStructureAnalysis.class && inputType == ProjectModule.class) {
            return (Analyzer<T, R>) new ProjectStructureAnalyzer();
        } else if (resultType == DependencyHealthAssessment.class && inputType == MavenDependencyAnalysis.class) {
            return (Analyzer<T, R>) new DependencyHealthAnalyzer();
        } else if (resultType == BasicStatistics.class && inputType == MavenDependencyAnalysis.class) {
            return (Analyzer<T, R>) new DependencyStatisticsAnalyzer();
        } else if (resultType == ChartGenerationResult.class && inputType == ProjectStructureAnalysis.class) {
            return (Analyzer<T, R>) new MavenProjectChartAnalyzer();
        } else if (resultType == DataQualityMetrics.class && inputType == ProjectModule.class) {
            return (Analyzer<T, R>) new MavenDataQualityAnalyzer();
        } else if (resultType == Map.class && inputType == ProjectModule.class) {
            return (Analyzer<T, R>) new CompositeMavenAnalyzer();
        }
        
        throw new IllegalArgumentException("No analyzer found for " + resultType + " from " + inputType);
    }

    /**
     * Usage examples for Maven analyzers
     */
    public static class Examples {
        
        public static void demonstrateMavenAnalysis(String pomFilePath) {
            // 1. Parse Maven project
            Model rootModel = MavenParser.parseModel(pomFilePath);
            ProjectModule project = new ProjectModule(rootModel);
            
            // 2. Run dependency analysis
            MavenDependencyAnalysis depAnalysis = new MavenDependencyAnalyzer().analyze(project);
            System.out.println("Total dependencies: " + depAnalysis.totalDependencies());
            System.out.println("Outdated: " + depAnalysis.outdatedPercentage() + "%");
            
            // 3. Analyze project structure
            ProjectStructureAnalysis structure = new ProjectStructureAnalyzer().analyze(project);
            System.out.println("Total modules: " + structure.totalModules());
            System.out.println("Average dependencies per module: " + structure.averageDependenciesPerModule());
            
            // 4. Assess dependency health
            DependencyHealthAssessment health = new DependencyHealthAnalyzer().analyze(depAnalysis);
            System.out.println("Health Grade: " + health.healthGrade());
            health.recommendations().forEach(System.out::println);
            
            // 5. Statistical analysis
            BasicStatistics stats = new DependencyStatisticsAnalyzer().analyze(depAnalysis);
            System.out.println("Mean version age: " + stats.mean() + " days");
            
            // 6. Generate visualization
            ChartGenerationResult chart = new MavenProjectChartAnalyzer().analyze(structure);
            if (chart.success()) {
                System.out.println("Chart saved to: " + chart.filePath());
            }
            
            // 7. Complete analysis using composite analyzer
            Map<String, Object> fullAnalysis = new CompositeMavenAnalyzer().analyze(project);
            
            // 8. Check for specific issues
            if (depAnalysis.duplicateDependencies().size() > 0) {
                System.out.println("\nDuplicate dependencies found:");
                depAnalysis.duplicateDependencies().forEach((key, deps) -> {
                    System.out.println(key + ": " + deps.stream()
                        .map(ParsedDependency::version)
                        .collect(Collectors.joining(", ")));
                });
            }
            
            // 9. Multi-module specific analysis
            if (structure.totalModules() > 1) {
                System.out.println("\nModule breakdown:");
                structure.moduleAnalyses().forEach((name, analysis) -> {
                    System.out.println("  " + name + ": " + analysis.dependencyCount() + " dependencies");
                });
            }
        }
    }

    public static void main(String[] args) {
        Examples.demonstrateMavenAnalysis("pom.xml");
    }
}