package io.joshuasalcedo.parser.java.model;

public class ParameterRepresentation {
    private final String name;
    private final String type;
    
    public ParameterRepresentation(String name, String type) {
        this.name = name;
        this.type = type;
    }
    
    public String getName() { return name; }
    public String getType() { return type; }
}