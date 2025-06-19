package io.joshuasalcedo.parser.java.model;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ConstructorRepresentation {
    private String visibility;
    private List<ParameterRepresentation> parameters = new ArrayList<>();
    
    public void printStructure(int indent) {
        String indentStr = "  ".repeat(indent);
        System.out.print(indentStr);
        System.out.print(visibility + " <constructor>(");
        
        System.out.print(parameters.stream()
            .map(p -> p.getType() + " " + p.getName())
            .collect(Collectors.joining(", ")));
        
        System.out.println(")");
    }
    
    // Getters and setters
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public void addParameter(ParameterRepresentation param) { parameters.add(param); }
}