package io.joshuasalcedo.parser.spring.core;

import io.joshuasalcedo.parser.common.model.*;
import io.joshuasalcedo.parser.java.model.*;
import io.joshuasalcedo.parser.maven.analyzer.MavenCompositeAnalyzers;
import io.joshuasalcedo.parser.maven.model.ProjectModule;

public class ParserJavaAnalysisImpl implements ParserJavaAnalysis {
    @Override
    public ProjectStatistics getProjectStatistics() {
        MavenCompositeAnalyzers.create(ProjectStatistics.class, ProjectModule.class);
        return null;
    }

    @Override
    public ProjectDependencyResult getDependencyResult() {
        return null;
    }

    @Override
    public MetricsResult getMetricsResult() {
        return null;
    }

    @Override
    public PatternSearchResult getPatternSearchResult() {
        return null;
    }

    @Override
    public ProjectHealthAssessment getProjectHealthAssessment() {
        return null;
    }

    @Override
    public ProjectSummary getProjectSummary() {
        return null;
    }

    @Override
    public ProjectAnalysisResult getProjectAnalysisResult() {
        return null;
    }

    @Override
    public BasicStatistics getBasicStatistics() {
        return null;
    }

    @Override
    public CorrelationAnalysis getCorrelationAnalysis() {
        return null;
    }

    @Override
    public OutlierAnalysis getOutlierAnalysis() {
        return null;
    }

    @Override
    public ChartGenerationResult getChartGenerationResult() {
        return null;
    }

    @Override
    public RegressionAnalysis getRegressionAnalysis() {
        return null;
    }

    @Override
    public DataQualityMetrics getDataQualityMetrics() {
        return null;
    }
}
