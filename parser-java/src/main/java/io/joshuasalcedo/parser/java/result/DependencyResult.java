package io.joshuasalcedo.parser.java.result;

import java.util.*;

/**
 * Result of dependency analysis including coupling metrics and circular dependencies
 */
public record DependencyResult(
    Map<String, Set<String>> classDependencies,
    Map<String, Set<String>> methodCalls,
    Map<String, Set<String>> fieldReferences,
    List<List<String>> circularDependencies,
    Set<String> unusedClasses,
    Map<String, Integer> afferentCoupling,  // Incoming dependencies
    Map<String, Integer> efferentCoupling,  // Outgoing dependencies
    Map<String, Double> instability,        // Instability metric (Ce / (Ca + Ce))
    Map<String, Set<String>> packageDependencies
) {
    
    public boolean hasCircularDependencies() {
        return !circularDependencies.isEmpty();
    }
    
    public boolean hasUnusedClasses() {
        return !unusedClasses.isEmpty();
    }
    
    public int getMaxEfferentCoupling() {
        return efferentCoupling.values().stream()
            .max(Integer::compareTo)
            .orElse(0);
    }
    
    public int getMaxAfferentCoupling() {
        return afferentCoupling.values().stream()
            .max(Integer::compareTo)
            .orElse(0);
    }
    
    public double getAverageInstability() {
        return instability.values().stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
    }
    
    public List<String> getMostDependedUponClasses(int limit) {
        return afferentCoupling.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .toList();
    }
    
    public List<String> getMostDependentClasses(int limit) {
        return efferentCoupling.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .toList();
    }
    
    public Map<String, String> getStabilityAssessment() {
        Map<String, String> assessment = new HashMap<>();
        
        instability.forEach((className, value) -> {
            if (value == 0.0) {
                assessment.put(className, "Maximally Stable");
            } else if (value < 0.3) {
                assessment.put(className, "Stable");
            } else if (value < 0.7) {
                assessment.put(className, "Moderately Stable");
            } else if (value < 0.9) {
                assessment.put(className, "Unstable");
            } else {
                assessment.put(className, "Maximally Unstable");
            }
        });
        
        return assessment;
    }
}