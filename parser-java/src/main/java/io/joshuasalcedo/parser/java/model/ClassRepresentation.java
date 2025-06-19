package io.joshuasalcedo.parser.java.model;



import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClassRepresentation {
    private String name;
    private String packageName;
    private String filePath;
    private boolean isInterface;
    private boolean isEnum;
    private boolean isAbstract;
    private List<String> annotations = new ArrayList<>();
    private List<String> extendedTypes = new ArrayList<>();
    private List<String> implementedInterfaces = new ArrayList<>();
    private List<FieldRepresentation> fields = new ArrayList<>();
    private List<MethodRepresentation> methods = new ArrayList<>();
    private List<ConstructorRepresentation> constructors = new ArrayList<>();
    
    public String getFullyQualifiedName() {
        return packageName.isEmpty() ? name : packageName + "." + name;
    }
    
    public void printStructure(int indent) {
        String indentStr = "  ".repeat(indent);
        
        // Print class header
        System.out.print(indentStr);
        if (!annotations.isEmpty()) {
            System.out.print(annotations.stream()
                .map(a -> "@" + a)
                .collect(Collectors.joining(" ")) + " ");
        }
        
        if (isInterface) {
            System.out.print("interface ");
        } else if (isEnum) {
            System.out.print("enum ");
        } else {
            if (isAbstract) System.out.print("abstract ");
            System.out.print("class ");
        }
        
        System.out.print(name);
        
        if (!extendedTypes.isEmpty()) {
            System.out.print(" extends " + String.join(", ", extendedTypes));
        }
        
        if (!implementedInterfaces.isEmpty()) {
            System.out.print(" implements " + String.join(", ", implementedInterfaces));
        }
        
        System.out.println();
        System.out.println(indentStr + "Package: " + packageName);
        System.out.println(indentStr + "File: " + filePath);
        
        // Print fields
        if (!fields.isEmpty()) {
            System.out.println(indentStr + "Fields:");
            fields.forEach(field -> field.printStructure(indent + 1));
        }
        
        // Print constructors
        if (!constructors.isEmpty()) {
            System.out.println(indentStr + "Constructors:");
            constructors.forEach(constructor -> constructor.printStructure(indent + 1));
        }
        
        // Print methods
        if (!methods.isEmpty()) {
            System.out.println(indentStr + "Methods:");
            methods.forEach(method -> method.printStructure(indent + 1));
        }
    }
    
    // Getters and setters
    public String getName() { return name; }
    public String getPackageName() { return packageName; }
    public String getFilePath() { return filePath; }
    public boolean isInterface() { return isInterface; }
    public boolean isEnum() { return isEnum; }
    public boolean isAbstract() { return isAbstract; }
    public List<String> getAnnotations() { return annotations; }
    public List<String> getExtendedTypes() { return extendedTypes; }
    public List<String> getImplementedInterfaces() { return implementedInterfaces; }
    public List<FieldRepresentation> getFields() { return fields; }
    public List<MethodRepresentation> getMethods() { return methods; }
    public List<ConstructorRepresentation> getConstructors() { return constructors; }
    
    public void setName(String name) { this.name = name; }
    public void setPackageName(String packageName) { this.packageName = packageName; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public void setInterface(boolean isInterface) { this.isInterface = isInterface; }
    public void setEnum(boolean isEnum) { this.isEnum = isEnum; }
    public void setAbstract(boolean isAbstract) { this.isAbstract = isAbstract; }
    public void addAnnotation(String annotation) { annotations.add(annotation); }
    public void addExtendedType(String type) { extendedTypes.add(type); }
    public void addImplementedInterface(String interfaceName) { implementedInterfaces.add(interfaceName); }
    public void addField(FieldRepresentation field) { fields.add(field); }
    public void addMethod(MethodRepresentation method) { methods.add(method); }
    public void addConstructor(ConstructorRepresentation constructor) { constructors.add(constructor); }
}