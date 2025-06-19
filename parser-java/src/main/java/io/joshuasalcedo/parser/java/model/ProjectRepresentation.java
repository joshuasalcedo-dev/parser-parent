package io.joshuasalcedo.parser.java.model;// Representation classes

import java.util.ArrayList;
import java.util.List;

public class ProjectRepresentation {
    private final String name;
    private final String rootPath;
    private final List<ClassRepresentation> classes = new ArrayList<>();
    
    public ProjectRepresentation(String name, String rootPath) {
        this.name = name;
        this.rootPath = rootPath;
    }
    
    public void addClass(ClassRepresentation classRep) {
        classes.add(classRep);
    }
    
    public List<ClassRepresentation> getClasses() {
        return classes;
    }
    
    public String getName() {
        return name;
    }
    
    public String getRootPath() {
        return rootPath;
    }
    
    public void printStructure() {
        System.out.println("Project: " + name);
        System.out.println("Root: " + rootPath);
        System.out.println("Classes found: " + classes.size());
        System.out.println();
        
        classes.forEach(classRep -> {
            classRep.printStructure(0);
            System.out.println();
        });
    }
}