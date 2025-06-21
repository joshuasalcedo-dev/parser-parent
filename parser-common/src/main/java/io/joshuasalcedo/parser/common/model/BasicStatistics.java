package io.joshuasalcedo.parser.common.model;

// Basic statistical metrics record
public record BasicStatistics(
    long count,
    double mean,
    double standardDeviation,
    double variance,
    double min,
    double max,
    double sum,
    double q1,
    double median,
    double q3,
    double skewness
) {}
