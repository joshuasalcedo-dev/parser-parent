package io.joshuasalcedo.parser.java.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a field in a Java class
 */
public class FieldRepresentation {
    private String name;
    private String type;
    private String visibility;
    private boolean isStatic;
    private boolean isFinal;
    private List<String> annotations = new ArrayList<>();
    
    public void printStructure(int indent) {
        String indentStr = "  ".repeat(indent);
        System.out.print(indentStr);
        
        if (!annotations.isEmpty()) {
            System.out.print(annotations.stream()
                .map(a -> "@" + a)
                .collect(Collectors.joining(" ")) + " ");
        }
        
        System.out.print(visibility + " ");
        if (isStatic) System.out.print("static ");
        if (isFinal) System.out.print("final ");
        System.out.println(type + " " + name);
    }
    
    // Getters and setters
    public String getName() { return name; }
    public String getType() { return type; }
    public String getVisibility() { return visibility; }
    public boolean isStatic() { return isStatic; }
    public boolean isFinal() { return isFinal; }
    public List<String> getAnnotations() { return annotations; }
    
    public void setName(String name) { this.name = name; }
    public void setType(String type) { this.type = type; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public void setStatic(boolean isStatic) { this.isStatic = isStatic; }
    public void setFinal(boolean isFinal) { this.isFinal = isFinal; }
    public void addAnnotation(String annotation) { annotations.add(annotation); }
}

