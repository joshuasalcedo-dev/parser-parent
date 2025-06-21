package io.joshuasalcedo.parser.common.model;

// Time series statistics record
public record TimeSeriesStatistics(
    double trend,
    double seasonality,
    double[] movingAverage,
    double[] exponentialSmoothing,
    double volatility,
    double autocorrelation
) {}
