package io.joshuasalcedo.parser.java.model;

import java.util.List;

public record SubmoduleAnalysis(
    String name,
    String path,
    ProjectStatistics statistics,
    List<ClassComplexity> topComplexClasses
) {}