package io.joshuasalcedo.parser.common.model;

// Statistical test result record
public record StatisticalTestResult(
    String testName,
    double testStatistic,
    double pValue,
    double criticalValue,
    boolean rejectNullHypothesis,
    String conclusion
) {}
