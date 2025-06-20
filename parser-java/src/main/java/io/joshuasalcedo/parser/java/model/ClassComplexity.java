package io.joshuasalcedo.parser.java.model;

/**
 * Represents the complexity of a class
 */
public record ClassComplexity(
    String className,
    int methodCount,
    int fieldCount,
    int dependencyCount
) {}
