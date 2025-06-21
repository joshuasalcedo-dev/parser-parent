package io.joshuasalcedo.parser.common.model;

import java.util.List;

public record SeriesData(
    String name,
    List<Double> xData,
    List<Double> yData
) {}
