package io.joshuasalcedo.parser.common.model;

// Regression analysis record
public record RegressionAnalysis(
    double slope,
    double intercept,
    double rSquared,
    double standardError,
    double[] residuals,
    double[] predictedValues
) {}
