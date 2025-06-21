package io.joshuasalcedo.parser.maven.model;

import java.util.List;
import java.util.Map;

/**
     * Result record for dependency analysis
     */
    public record MavenDependencyAnalysis(
        int totalDependencies,
        int directDependencies,
        int transitiveDependencies,
        Map<String, Integer> scopeDistribution,
        List<ParsedDependency> outdatedDependencies,
        Map<String, List<ParsedDependency>> duplicateDependencies,
        List<String> securityVulnerabilities,
        double outdatedPercentage
    ) {}