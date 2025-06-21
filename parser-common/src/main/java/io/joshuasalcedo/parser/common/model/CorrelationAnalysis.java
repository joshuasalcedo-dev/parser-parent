package io.joshuasalcedo.parser.common.model;

// Correlation analysis record
public record CorrelationAnalysis(
    double pearsonCoefficient,
    double pValue,
    double[][] correlationMatrix,
    String strength,
    boolean isSignificant
) {}
