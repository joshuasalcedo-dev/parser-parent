package io.joshuasalcedo.parser.java.analyzer;

import io.joshuasalcedo.parser.java.model.ProjectRepresentation;

public abstract class AbstractAnalyzer<T> {
    protected final ProjectRepresentation project;
    
    public AbstractAnalyzer(ProjectRepresentation project) {
        this.project = project;
    }
    
    public abstract T analyze();
    
    public abstract String getAnalyzerName();
    
    public abstract void printResults(T results);
    
    public abstract void exportResults(T results, String outputPath);
}