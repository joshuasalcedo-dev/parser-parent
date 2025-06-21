package io.joshuasalcedo.parser.common.model;

import java.util.Map;

public record ChartGenerationResult(
    String filePath,
    String message,
    boolean success,
    Map<String, Object> metadata
) {}
