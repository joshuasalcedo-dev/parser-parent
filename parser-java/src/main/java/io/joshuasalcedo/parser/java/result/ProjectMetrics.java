package io.joshuasalcedo.parser.java.result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map; /**
 * Overall project metrics
 */
public record ProjectMetrics(
    int totalClasses,
    int totalMethods,
    int totalLinesOfCode,
    double averageComplexity,
    double averageMaintainability,
    double duplicationRatio,
    double codeReuse,
    int unusedMethods,
    int unusedClasses,
    double technicalDebtScore
) {
    
    public String getProjectHealthGrade() {
        double score = (100 - technicalDebtScore) * (averageMaintainability / 100) * (1 - duplicationRatio);
        
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "F";
    }
    
    public Map<String, Object> getSummaryMetrics() {
        Map<String, Object> summary = new HashMap<>();
        
        summary.put("totalClasses", totalClasses);
        summary.put("totalMethods", totalMethods);
        summary.put("totalLOC", totalLinesOfCode);
        summary.put("avgComplexity", String.format("%.2f", averageComplexity));
        summary.put("avgMaintainability", String.format("%.2f", averageMaintainability));
        summary.put("duplicationPercentage", String.format("%.1f%%", duplicationRatio * 100));
        summary.put("codeReusePercentage", String.format("%.1f%%", codeReuse * 100));
        summary.put("unusedCode", unusedMethods + unusedClasses);
        summary.put("technicalDebt", String.format("%.0f", technicalDebtScore));
        summary.put("healthGrade", getProjectHealthGrade());
        
        return summary;
    }
    
    public List<String> getRecommendations() {
        List<String> recommendations = new ArrayList<>();
        
        if (duplicationRatio > 0.1) {
            recommendations.add("High code duplication detected (" + 
                String.format("%.1f%%", duplicationRatio * 100) + 
                "). Consider extracting common functionality into shared utilities.");
        }
        
        if (averageComplexity > 15) {
            recommendations.add("High average complexity (" + 
                String.format("%.1f", averageComplexity) + 
                "). Consider breaking down complex methods.");
        }
        
        if (averageMaintainability < 60) {
            recommendations.add("Low maintainability index (" + 
                String.format("%.1f", averageMaintainability) + 
                "). Focus on reducing complexity and improving code structure.");
        }
        
        if (unusedMethods > totalMethods * 0.2) {
            recommendations.add("Significant amount of unused code detected. " +
                "Consider removing " + unusedMethods + " unused methods.");
        }
        
        if (codeReuse < 0.3) {
            recommendations.add("Low code reuse (" + 
                String.format("%.1f%%", codeReuse * 100) + 
                "). Look for opportunities to create reusable components.");
        }
        
        return recommendations;
    }
}
