package io.joshuasalcedo.parser.java.model;

public record ClassComplexity(
    String className,
    int methodCount,
    int fieldCount,
    int dependencyCount
) {}
