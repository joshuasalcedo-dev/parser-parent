package io.joshuasalcedo.parser.common.model;

import java.util.List;

// Outlier analysis record
public record OutlierAnalysis(
    List<Double> outliers,
    List<Integer> outlierIndices,
    double lowerBound,
    double upperBound,
    double iqr,
    String method
) {}
