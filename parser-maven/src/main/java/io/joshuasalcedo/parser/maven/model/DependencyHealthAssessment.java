package io.joshuasalcedo.parser.maven.model;

import java.util.List;
import java.util.Map;

/**
     * Result record for dependency health assessment
     */
    public record DependencyHealthAssessment(
        double healthScore,
        String healthGrade,
        List<String> criticalIssues,
        List<String> warnings,
        List<String> recommendations,
        Map<String, Double> scoreBreakdown
    ) {}