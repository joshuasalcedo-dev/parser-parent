package io.joshuasalcedo.parser.common.model;

import java.util.Map;

// Summary statistics record
public record SummaryStatistics(
    Map<String, Double> centralTendency,
    Map<String, Double> dispersion,
    Map<String, Double> shape,
    Map<String, Double> percentiles,
    long sampleSize
) {}
