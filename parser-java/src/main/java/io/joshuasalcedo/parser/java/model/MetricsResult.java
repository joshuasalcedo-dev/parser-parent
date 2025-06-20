package io.joshuasalcedo.parser.java.model;

import java.util.*;

/**
 * Comprehensive metrics analysis result
 */
public record MetricsResult(
    List<ClassMetrics> classMetrics,
    List<CodeDuplication> codeDuplicates,
    List<MethodUsage> methodUsages,
    Map<String, Integer> mostUsedClasses,
    Set<String> unusedMethods,
    Set<String> unusedClasses,
    Map<String, Double> classComplexity,
    Map<String, Integer> linesOfCode,
    ProjectMetrics projectMetrics
) {
    
    public List<ClassMetrics> getMostComplexClasses(int limit) {
        return classMetrics.stream()
            .sorted((a, b) -> Double.compare(b.complexity(), a.complexity()))
            .limit(limit)
            .toList();
    }
    
    public List<ClassMetrics> getLeastMaintainableClasses(int limit) {
        return classMetrics.stream()
            .sorted(Comparator.comparingDouble(ClassMetrics::maintainabilityIndex))
            .limit(limit)
            .toList();
    }
    
    public List<CodeDuplication> getMostImpactfulDuplicates(int limit) {
        return codeDuplicates.stream()
            .sorted((a, b) -> Double.compare(b.impact(), a.impact()))
            .limit(limit)
            .toList();
    }
    
    public Map<String, List<String>> getDuplicationClusters() {
        Map<String, List<String>> clusters = new HashMap<>();
        
        for (CodeDuplication dup : codeDuplicates) {
            String key = dup.methods().stream()
                .sorted()
                .findFirst()
                .orElse("");
            
            clusters.computeIfAbsent(key, k -> new ArrayList<>())
                   .addAll(dup.methods());
        }
        
        return clusters;
    }
    
    public double getOverallHealthScore() {
        double maintainability = projectMetrics.averageMaintainability() / 100;
        double duplication = 1 - projectMetrics.duplicationRatio();
        double reuse = projectMetrics.codeReuse();
        double unusedCode = 1 - ((double)(unusedMethods.size() + unusedClasses.size()) / 
                                (projectMetrics.totalMethods() + projectMetrics.totalClasses()));
        
        return (maintainability + duplication + reuse + unusedCode) / 4 * 100;
    }
}

