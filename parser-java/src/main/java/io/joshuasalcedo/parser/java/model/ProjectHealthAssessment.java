package io.joshuasalcedo.parser.java.model;

import io.joshuasalcedo.parser.java.parser.RiskLevel;

import java.util.List; /**
 * Project health assessment
 */
public class ProjectHealthAssessment {
    private final double healthScore;
    private final String healthGrade;
    private final List<String> recommendations;
    private final List<ProjectRisk> risks;
    
    public ProjectHealthAssessment(double healthScore, String healthGrade,
                                  List<String> recommendations, List<ProjectRisk> risks) {
        this.healthScore = healthScore;
        this.healthGrade = healthGrade;
        this.recommendations = recommendations;
        this.risks = risks;
    }
    
    public double getHealthScore() { return healthScore; }
    public String getHealthGrade() { return healthGrade; }
    public List<String> getRecommendations() { return recommendations; }
    public List<ProjectRisk> getRisks() { return risks; }
    
    public boolean isHealthy() {
        return healthScore > 70.0 && (risks.isEmpty() || 
            risks.stream().noneMatch(r -> r.getLevel() == RiskLevel.HIGH || r.getLevel() == RiskLevel.CRITICAL));
    }
}
