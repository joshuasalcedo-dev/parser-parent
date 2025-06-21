package io.joshuasalcedo.parser.maven.model;

import java.util.List;
import java.util.Map;

/**
     * Result record for project structure analysis
     */
    public record ProjectStructureAnalysis(
        int totalModules,
        int maxDepth,
        Map<String, ModuleAnalysis> moduleAnalyses,
        List<String> rootModules,
        List<String> leafModules,
        double averageDependenciesPerModule
    ) {}