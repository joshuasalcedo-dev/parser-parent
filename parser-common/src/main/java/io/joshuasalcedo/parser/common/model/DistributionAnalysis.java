package io.joshuasalcedo.parser.common.model;

// Distribution analysis record
public record DistributionAnalysis(
    String distributionType,
    double mean,
    double variance,
    double[] confidenceInterval,
    double kolmogorovSmirnovStatistic,
    double pValue,
    boolean isNormallyDistributed
) {}
