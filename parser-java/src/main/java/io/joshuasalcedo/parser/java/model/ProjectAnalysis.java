package io.joshuasalcedo.parser.java.model;


public record ProjectAnalysis(
    ProjectRepresentation project,
    ProjectStatistics statistics,
    long analysisTimestamp
) {}