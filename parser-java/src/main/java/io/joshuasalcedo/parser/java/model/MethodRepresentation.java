package io.joshuasalcedo.parser.java.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors; /**
 * Represents a method in a Java class
 */
public class MethodRepresentation {
    private String name;
    private String returnType;
    private String visibility;
    private boolean isStatic;
    private boolean isAbstract;
    private List<String> annotations = new ArrayList<>();
    private List<ParameterRepresentation> parameters = new ArrayList<>();
    
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
        if (isAbstract) System.out.print("abstract ");
        System.out.print(returnType + " " + name + "(");
        
        System.out.print(parameters.stream()
            .map(p -> p.getType() + " " + p.getName())
            .collect(Collectors.joining(", ")));
        
        System.out.println(")");
    }
    
    // Getters and setters
    public String getName() { return name; }
    public String getReturnType() { return returnType; }
    public String getVisibility() { return visibility; }
    public boolean isStatic() { return isStatic; }
    public boolean isAbstract() { return isAbstract; }
    public List<String> getAnnotations() { return annotations; }
    public List<ParameterRepresentation> getParameters() { return parameters; }
    
    public void setName(String name) { this.name = name; }
    public void setReturnType(String returnType) { this.returnType = returnType; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public void setStatic(boolean isStatic) { this.isStatic = isStatic; }
    public void setAbstract(boolean isAbstract) { this.isAbstract = isAbstract; }
    public void addAnnotation(String annotation) { annotations.add(annotation); }
    public void addParameter(ParameterRepresentation param) { parameters.add(param); }
}
