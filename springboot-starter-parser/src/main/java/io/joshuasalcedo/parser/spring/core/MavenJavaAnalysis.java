package io.joshuasalcedo.parser.spring.core;

import io.joshuasalcedo.parser.common.model.BasicStatistics;
import io.joshuasalcedo.parser.common.model.DataQualityMetrics;
import io.joshuasalcedo.parser.maven.model.DependencyHealthAssessment;
import io.joshuasalcedo.parser.maven.model.MavenDependencyAnalysis;
import io.joshuasalcedo.parser.maven.model.ProjectStructureAnalysis;

public interface  MavenJavaAnalysis {
    MavenDependencyAnalysis getDependencyAnalysis();
    ProjectStructureAnalysis getProjectStructureAnalysis();
    DependencyHealthAssessment getDependencyHealthAssessment();
    BasicStatistics getBasicStatistics();
    DataQualityMetrics getDataQualityMetrics();

}
