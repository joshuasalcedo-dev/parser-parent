package io.joshuasalcedo.parser.java.model;


import java.util.List;
import java.util.Map;

// Statistics Records
public record ProjectStatistics(
    int totalClasses,
    int totalInterfaces,
    int totalEnums,
    int totalAbstractClasses,
    int totalMethods,
    int totalFields,
    int totalConstructors,
    int totalPublicMethods,
    int totalPrivateMethods,
    int totalProtectedMethods,
    int totalStaticMethods,
    int totalAnnotations,
    int totalLinesOfCode,
    Map<String, Integer> annotationUsage,
    Map<String, Integer> packageDistribution,
    List<ClassComplexity> classComplexities
) {}