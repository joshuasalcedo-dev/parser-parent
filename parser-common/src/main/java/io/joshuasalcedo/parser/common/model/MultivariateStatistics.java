package io.joshuasalcedo.parser.common.model;

// Multivariate statistics record
public record MultivariateStatistics(
    double[][] covarianceMatrix,
    double[][] correlationMatrix,
    double[] eigenvalues,
    double[][] eigenVectors,
    double[] principalComponents,
    double explainedVariance
) {}
