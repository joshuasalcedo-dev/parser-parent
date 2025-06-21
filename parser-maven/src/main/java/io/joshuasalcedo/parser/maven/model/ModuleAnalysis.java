package io.joshuasalcedo.parser.maven.model;

import java.util.List;
import java.util.Map;

/**
     * Result record for module analysis
     */
    public record ModuleAnalysis(
        String moduleName,
        int dependencyCount,
        int submoduleCount,
        List<String> circularDependencies,
        Map<String, Integer> dependencyScopeBreakdown,
        boolean hasTestDependencies,
        boolean hasBuildDependencies
    ) {}
