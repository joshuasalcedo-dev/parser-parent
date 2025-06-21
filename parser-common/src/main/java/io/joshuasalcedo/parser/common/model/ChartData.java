package io.joshuasalcedo.parser.common.model;

import java.util.List;

// Supporting records for chart generation
public record ChartData(
    String title,
    String xLabel,
    String yLabel,
    String chartType,
    List<SeriesData> series
) {}
