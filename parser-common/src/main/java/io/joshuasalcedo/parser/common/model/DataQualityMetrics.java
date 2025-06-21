package io.joshuasalcedo.parser.common.model;

import java.util.List;
import java.util.Map;

// Data quality metrics record
public record DataQualityMetrics(
    long totalRecords,
    long missingValues,
    long duplicates,
    double completenessRatio,
    Map<String, Long> valueFrequencies,
    List<String> anomalies
) {}
