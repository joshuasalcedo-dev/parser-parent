package io.joshuasalcedo.parser.spring.core;

import io.joshuasalcedo.parser.common.model.*;
import io.joshuasalcedo.parser.java.model.*;

public interface ParserJavaAnalysis {

    ProjectStatistics getProjectStatistics();
    ProjectDependencyResult getDependencyResult();
    MetricsResult getMetricsResult();
    PatternSearchResult getPatternSearchResult();
    ProjectHealthAssessment getProjectHealthAssessment();
    ProjectSummary getProjectSummary();
    ProjectAnalysisResult  getProjectAnalysisResult();
    BasicStatistics getBasicStatistics();
    CorrelationAnalysis getCorrelationAnalysis();
    OutlierAnalysis getOutlierAnalysis();
    ChartGenerationResult getChartGenerationResult();
    RegressionAnalysis getRegressionAnalysis();
    DataQualityMetrics getDataQualityMetrics();

}

