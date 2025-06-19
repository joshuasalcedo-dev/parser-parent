package io.joshuasalcedo.parser.java.analyzer;

import io.joshuasalcedo.parser.java.model.*;
import io.joshuasalcedo.parser.java.result.GraphResult;
import io.joshuasalcedo.parser.java.result.DependencyResult;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates various graph visualizations of the project structure
 */
public class GraphAnalyzer extends AbstractAnalyzer<GraphResult> {
    
    private final DependencyResult dependencyResult;
    private final Map<String, ClassRepresentation> classMap;
    
    public GraphAnalyzer(ProjectRepresentation project, DependencyResult dependencyResult) {
        super(project);
        this.dependencyResult = dependencyResult;
        this.classMap = project.getClasses().stream()
            .collect(Collectors.toMap(ClassRepresentation::getFullyQualifiedName, c -> c));
    }
    
    @Override
    public GraphResult analyze() {
        String dotGraph = generateDOTGraph();
        String mermaidGraph = generateMermaidDiagram();
        String graphMLContent = generateGraphML();
        String plantUMLGraph = generateEnhancedPlantUML();
        String d2Graph = generateD2Diagram();
        
        return new GraphResult(
            dotGraph,
            mermaidGraph,
            graphMLContent,
            plantUMLGraph,
            d2Graph,
            calculateGraphMetrics()
        );
    }
    
    /**
     * Generates Graphviz DOT format with advanced styling
     */
    private String generateDOTGraph() {
        StringBuilder dot = new StringBuilder();
        dot.append("digraph ProjectDependencies {\n");
        dot.append("  rankdir=TB;\n");
        dot.append("  node [shape=box, style=\"rounded,filled\", fontname=\"Arial\"];\n");
        dot.append("  edge [fontname=\"Arial\", fontsize=10];\n");
        dot.append("\n");
        
        // Define node styles based on class types
        dot.append("  // Node styles\n");
        dot.append("  node [fillcolor=\"#E8F4FD\"] // default for classes\n");
        dot.append("\n");
        
        // Group nodes by package
        Map<String, List<ClassRepresentation>> packageGroups = project.getClasses().stream()
            .collect(Collectors.groupingBy(ClassRepresentation::getPackageName));
        
        int clusterIndex = 0;
        for (Map.Entry<String, List<ClassRepresentation>> entry : packageGroups.entrySet()) {
            String packageName = entry.getKey();
            List<ClassRepresentation> classes = entry.getValue();
            
            dot.append("  subgraph cluster_").append(clusterIndex++).append(" {\n");
            dot.append("    label=\"").append(packageName).append("\";\n");
            dot.append("    style=filled;\n");
            dot.append("    fillcolor=\"#F0F0F0\";\n");
            dot.append("    fontsize=12;\n");
            dot.append("\n");
            
            for (ClassRepresentation cls : classes) {
                String nodeId = sanitizeNodeId(cls.getFullyQualifiedName());
                String label = cls.getName();
                String fillColor = getNodeColor(cls);
                String shape = getNodeShape(cls);
                
                dot.append("    ").append(nodeId);
                dot.append(" [label=\"").append(label).append("\"");
                dot.append(", fillcolor=\"").append(fillColor).append("\"");
                dot.append(", shape=").append(shape);
                
                // Add tooltip with details
                String tooltip = generateTooltip(cls);
                dot.append(", tooltip=\"").append(tooltip).append("\"");
                
                dot.append("];\n");
            }
            dot.append("  }\n\n");
        }
        
        // Add edges with different styles for different dependency types
        dot.append("  // Dependencies\n");
        for (Map.Entry<String, Set<String>> entry : dependencyResult.classDependencies().entrySet()) {
            String source = sanitizeNodeId(entry.getKey());
            
            for (String target : entry.getValue()) {
                String targetId = sanitizeNodeId(target);
                ClassRepresentation sourceClass = classMap.get(entry.getKey());
                ClassRepresentation targetClass = classMap.get(target);
                
                if (sourceClass != null && targetClass != null) {
                    String edgeStyle = getEdgeStyle(sourceClass, targetClass);
                    String edgeLabel = getEdgeLabel(sourceClass, targetClass);
                    
                    dot.append("  ").append(source).append(" -> ").append(targetId);
                    dot.append(" [").append(edgeStyle);
                    if (!edgeLabel.isEmpty()) {
                        dot.append(", label=\"").append(edgeLabel).append("\"");
                    }
                    dot.append("];\n");
                }
            }
        }
        
        // Add legend
        dot.append("\n  // Legend\n");
        dot.append("  subgraph cluster_legend {\n");
        dot.append("    label=\"Legend\";\n");
        dot.append("    style=filled;\n");
        dot.append("    fillcolor=white;\n");
        dot.append("    node [shape=box, style=\"rounded,filled\"];\n");
        dot.append("    \n");
        dot.append("    class_node [label=\"Class\", fillcolor=\"#E8F4FD\"];\n");
        dot.append("    interface_node [label=\"Interface\", fillcolor=\"#E8FDE8\", shape=diamond];\n");
        dot.append("    abstract_node [label=\"Abstract\", fillcolor=\"#FFF4E8\"];\n");
        dot.append("    enum_node [label=\"Enum\", fillcolor=\"#F4E8FD\", shape=octagon];\n");
        dot.append("    \n");
        dot.append("    edge [style=invis];\n");
        dot.append("    class_node -> interface_node -> abstract_node -> enum_node;\n");
        dot.append("  }\n");
        
        dot.append("}\n");
        return dot.toString();
    }
    
    /**
     * Generates Mermaid diagram with interactive features
     */
    private String generateMermaidDiagram() {
        StringBuilder mermaid = new StringBuilder();
        mermaid.append("%%{init: {'theme':'base', 'themeVariables': {");
        mermaid.append("'primaryColor':'#E8F4FD',");
        mermaid.append("'primaryBorderColor':'#5B9BD5',");
        mermaid.append("'fontFamily':'Arial'}}}%%\n\n");
        
        mermaid.append("graph TB\n");
        mermaid.append("  %% Class Definitions\n");
        
        // Create nodes with custom styling
        Map<String, String> nodeIds = new HashMap<>();
        int nodeCounter = 0;
        
        for (ClassRepresentation cls : project.getClasses()) {
            String nodeId = "N" + (nodeCounter++);
            nodeIds.put(cls.getFullyQualifiedName(), nodeId);
            
            String label = cls.getName();
            if (cls.isInterface()) {
                mermaid.append("  ").append(nodeId).append("{{").append(label).append("}}\n");
            } else if (cls.isAbstract()) {
                mermaid.append("  ").append(nodeId).append("[/").append(label).append("/]\n");
            } else if (cls.isEnum()) {
                mermaid.append("  ").append(nodeId).append("[(").append(label).append(")]\n");
            } else {
                mermaid.append("  ").append(nodeId).append("[").append(label).append("]\n");
            }
        }
        
        mermaid.append("\n  %% Dependencies\n");
        
        // Add edges with labels
        for (Map.Entry<String, Set<String>> entry : dependencyResult.classDependencies().entrySet()) {
            String sourceId = nodeIds.get(entry.getKey());
            
            for (String target : entry.getValue()) {
                String targetId = nodeIds.get(target);
                if (sourceId != null && targetId != null) {
                    ClassRepresentation sourceClass = classMap.get(entry.getKey());
                    ClassRepresentation targetClass = classMap.get(target);
                    
                    if (isInheritance(sourceClass, targetClass)) {
                        mermaid.append("  ").append(sourceId).append(" -.->|extends| ").append(targetId).append("\n");
                    } else if (isImplementation(sourceClass, targetClass)) {
                        mermaid.append("  ").append(sourceId).append(" -.->|implements| ").append(targetId).append("\n");
                    } else {
                        mermaid.append("  ").append(sourceId).append(" --> ").append(targetId).append("\n");
                    }
                }
            }
        }
        
        // Add styling
        mermaid.append("\n  %% Styling\n");
        mermaid.append("  classDef interface fill:#E8FDE8,stroke:#4CAF50,stroke-width:2px\n");
        mermaid.append("  classDef abstract fill:#FFF4E8,stroke:#FF9800,stroke-width:2px\n");
        mermaid.append("  classDef enum fill:#F4E8FD,stroke:#9C27B0,stroke-width:2px\n");
        mermaid.append("  classDef default fill:#E8F4FD,stroke:#2196F3,stroke-width:2px\n");
        
        // Apply styles
        for (Map.Entry<String, String> entry : nodeIds.entrySet()) {
            ClassRepresentation cls = classMap.get(entry.getKey());
            if (cls != null) {
                String nodeId = entry.getValue();
                if (cls.isInterface()) {
                    mermaid.append("  class ").append(nodeId).append(" interface\n");
                } else if (cls.isAbstract()) {
                    mermaid.append("  class ").append(nodeId).append(" abstract\n");
                } else if (cls.isEnum()) {
                    mermaid.append("  class ").append(nodeId).append(" enum\n");
                }
            }
        }
        
        // Add click events for interactivity
        mermaid.append("\n  %% Click events\n");
        for (Map.Entry<String, String> entry : nodeIds.entrySet()) {
            String nodeId = entry.getValue();
            String className = entry.getKey();
            mermaid.append("  click ").append(nodeId).append(" \"#").append(className).append("\"\n");
        }
        
        return mermaid.toString();
    }
    
    /**
     * Generates GraphML format for advanced graph analysis tools
     */
    private String generateGraphML() {
        StringBuilder graphml = new StringBuilder();
        graphml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        graphml.append("<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\"\n");
        graphml.append("         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        graphml.append("         xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns\n");
        graphml.append("         http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd\">\n");
        
        // Define attributes
        graphml.append("  <key id=\"type\" for=\"node\" attr.name=\"type\" attr.type=\"string\"/>\n");
        graphml.append("  <key id=\"package\" for=\"node\" attr.name=\"package\" attr.type=\"string\"/>\n");
        graphml.append("  <key id=\"methods\" for=\"node\" attr.name=\"methods\" attr.type=\"int\"/>\n");
        graphml.append("  <key id=\"fields\" for=\"node\" attr.name=\"fields\" attr.type=\"int\"/>\n");
        graphml.append("  <key id=\"complexity\" for=\"node\" attr.name=\"complexity\" attr.type=\"double\"/>\n");
        graphml.append("  <key id=\"coupling\" for=\"node\" attr.name=\"coupling\" attr.type=\"int\"/>\n");
        graphml.append("  <key id=\"instability\" for=\"node\" attr.name=\"instability\" attr.type=\"double\"/>\n");
        graphml.append("  <key id=\"deptype\" for=\"edge\" attr.name=\"dependency_type\" attr.type=\"string\"/>\n");
        graphml.append("\n");
        
        graphml.append("  <graph id=\"ProjectGraph\" edgedefault=\"directed\">\n");
        
        // Add nodes
        for (ClassRepresentation cls : project.getClasses()) {
            String nodeId = sanitizeNodeId(cls.getFullyQualifiedName());
            graphml.append("    <node id=\"").append(nodeId).append("\">\n");
            graphml.append("      <data key=\"type\">").append(getClassType(cls)).append("</data>\n");
            graphml.append("      <data key=\"package\">").append(cls.getPackageName()).append("</data>\n");
            graphml.append("      <data key=\"methods\">").append(cls.getMethods().size()).append("</data>\n");
            graphml.append("      <data key=\"fields\">").append(cls.getFields().size()).append("</data>\n");
            
            // Add metrics if available
            Integer coupling = dependencyResult.efferentCoupling().get(cls.getFullyQualifiedName());
            if (coupling != null) {
                graphml.append("      <data key=\"coupling\">").append(coupling).append("</data>\n");
            }
            
            Double instability = dependencyResult.instability().get(cls.getFullyQualifiedName());
            if (instability != null) {
                graphml.append("      <data key=\"instability\">").append(String.format("%.2f", instability)).append("</data>\n");
            }
            
            graphml.append("    </node>\n");
        }
        
        // Add edges
        int edgeId = 0;
        for (Map.Entry<String, Set<String>> entry : dependencyResult.classDependencies().entrySet()) {
            String source = sanitizeNodeId(entry.getKey());
            
            for (String target : entry.getValue()) {
                String targetId = sanitizeNodeId(target);
                ClassRepresentation sourceClass = classMap.get(entry.getKey());
                ClassRepresentation targetClass = classMap.get(target);
                
                if (sourceClass != null && targetClass != null) {
                    graphml.append("    <edge id=\"e").append(edgeId++).append("\" ");
                    graphml.append("source=\"").append(source).append("\" ");
                    graphml.append("target=\"").append(targetId).append("\">\n");
                    
                    String depType = getDependencyType(sourceClass, targetClass);
                    graphml.append("      <data key=\"deptype\">").append(depType).append("</data>\n");
                    graphml.append("    </edge>\n");
                }
            }
        }
        
        graphml.append("  </graph>\n");
        graphml.append("</graphml>\n");
        
        return graphml.toString();
    }
    
    /**
     * Generates enhanced PlantUML with better visualization
     */
    private String generateEnhancedPlantUML() {
        StringBuilder puml = new StringBuilder();
        puml.append("@startuml\n");
        puml.append("!theme plain\n");
        puml.append("skinparam packageStyle rectangle\n");
        puml.append("skinparam shadowing false\n");
        puml.append("skinparam classFontSize 12\n");
        puml.append("skinparam classAttributeIconSize 0\n");
        
        // Color schemes
        puml.append("skinparam class {\n");
        puml.append("  BackgroundColor<<interface>> #E8FDE8\n");
        puml.append("  BorderColor<<interface>> #4CAF50\n");
        puml.append("  BackgroundColor<<abstract>> #FFF4E8\n");
        puml.append("  BorderColor<<abstract>> #FF9800\n");
        puml.append("  BackgroundColor<<enum>> #F4E8FD\n");
        puml.append("  BorderColor<<enum>> #9C27B0\n");
        puml.append("  BackgroundColor #E8F4FD\n");
        puml.append("  BorderColor #2196F3\n");
        puml.append("}\n\n");
        
        // Group by package with better layout
        Map<String, List<ClassRepresentation>> packageGroups = project.getClasses().stream()
            .collect(Collectors.groupingBy(ClassRepresentation::getPackageName));
        
        // Calculate package dependencies for layout
        Map<String, Set<String>> packageDeps = dependencyResult.packageDependencies();
        List<String> sortedPackages = topologicalSort(packageDeps);
        
        for (String pkg : sortedPackages) {
            List<ClassRepresentation> classes = packageGroups.get(pkg);
            if (classes != null && !classes.isEmpty()) {
                puml.append("package \"").append(pkg).append("\" {\n");
                
                for (ClassRepresentation cls : classes) {
                    // Class declaration with stereotype
                    if (cls.isInterface()) {
                        puml.append("  interface ");
                    } else if (cls.isAbstract()) {
                        puml.append("  abstract class ");
                    } else if (cls.isEnum()) {
                        puml.append("  enum ");
                    } else {
                        puml.append("  class ");
                    }
                    
                    puml.append(cls.getName());
                    
                    // Add stereotypes
                    if (cls.isInterface()) {
                        puml.append(" <<interface>>");
                    } else if (cls.isAbstract()) {
                        puml.append(" <<abstract>>");
                    } else if (cls.isEnum()) {
                        puml.append(" <<enum>>");
                    }
                    
                    // Add complexity indicator
                    int complexity = cls.getMethods().size() + cls.getFields().size();
                    if (complexity > 20) {
                        puml.append(" <<complex>>");
                    }
                    
                    puml.append(" {\n");
                    
                    // Add key fields (limit to avoid clutter)
                    List<FieldRepresentation> publicFields = cls.getFields().stream()
                        .filter(f -> "public".equals(f.getVisibility()))
                        .limit(5)
                        .collect(Collectors.toList());
                    
                    for (FieldRepresentation field : publicFields) {
                        puml.append("    ").append(getVisibilitySymbol(field.getVisibility()));
                        if (field.isStatic()) puml.append("{static} ");
                        puml.append(field.getType()).append(" ").append(field.getName()).append("\n");
                    }
                    
                    if (cls.getFields().size() > 5) {
                        puml.append("    ...\n");
                    }
                    
                    // Add key methods (limit to avoid clutter)
                    List<MethodRepresentation> publicMethods = cls.getMethods().stream()
                        .filter(m -> "public".equals(m.getVisibility()))
                        .limit(5)
                        .collect(Collectors.toList());
                    
                    for (MethodRepresentation method : publicMethods) {
                        puml.append("    ").append(getVisibilitySymbol(method.getVisibility()));
                        if (method.isStatic()) puml.append("{static} ");
                        if (method.isAbstract()) puml.append("{abstract} ");
                        puml.append(method.getReturnType()).append(" ").append(method.getName());
                        puml.append("(");
                        puml.append(method.getParameters().stream()
                            .map(p -> p.getType())
                            .collect(Collectors.joining(", ")));
                        puml.append(")\n");
                    }
                    
                    if (cls.getMethods().size() > 5) {
                        puml.append("    ...\n");
                    }
                    
                    puml.append("  }\n");
                }
                
                puml.append("}\n\n");
            }
        }
        
        // Add relationships with better styling
        puml.append("' Relationships\n");
        Set<String> addedRelationships = new HashSet<>();
        
        for (ClassRepresentation cls : project.getClasses()) {
            String source = cls.getName();
            
            // Inheritance
            for (String extended : cls.getExtendedTypes()) {
                String relationship = source + " --|> " + getSimpleName(extended);
                if (addedRelationships.add(relationship)) {
                    puml.append(relationship).append(" : extends\n");
                }
            }
            
            // Implementation
            for (String implemented : cls.getImplementedInterfaces()) {
                String relationship = source + " ..|> " + getSimpleName(implemented);
                if (addedRelationships.add(relationship)) {
                    puml.append(relationship).append(" : implements\n");
                }
            }
            
            // Dependencies (aggregation/composition based on field types)
            Set<String> fieldDependencies = cls.getFields().stream()
                .map(FieldRepresentation::getType)
                .map(this::getSimpleName)
                .filter(type -> classMap.containsKey(type) || 
                               project.getClasses().stream().anyMatch(c -> c.getName().equals(type)))
                .collect(Collectors.toSet());
            
            for (String dep : fieldDependencies) {
                String relationship = source + " o-- " + dep;
                if (addedRelationships.add(relationship)) {
                    puml.append(relationship).append(" : has\n");
                }
            }
        }
        
        // Add notes for circular dependencies
        if (!dependencyResult.circularDependencies().isEmpty()) {
            puml.append("\nnote top : Circular Dependencies Detected!\n");
            for (List<String> cycle : dependencyResult.circularDependencies()) {
                puml.append("  ").append(String.join(" -> ", cycle)).append("\n");
            }
            puml.append("end note\n");
        }
        
        puml.append("\n@enduml\n");
        return puml.toString();
    }
    
    /**
     * Generates D2 diagram (modern diagramming language)
     */
    private String generateD2Diagram() {
        StringBuilder d2 = new StringBuilder();
        d2.append("# Project Architecture Diagram\n\n");
        d2.append("direction: down\n\n");
        
        // Define shapes and styles
        d2.append("classes: {\n");
        d2.append("  interface: {\n");
        d2.append("    shape: hexagon\n");
        d2.append("    style.fill: \"#E8FDE8\"\n");
        d2.append("    style.stroke: \"#4CAF50\"\n");
        d2.append("  }\n");
        d2.append("  abstract: {\n");
        d2.append("    shape: rectangle\n");
        d2.append("    style.fill: \"#FFF4E8\"\n");
        d2.append("    style.stroke: \"#FF9800\"\n");
        d2.append("    style.stroke-dash: 3\n");
        d2.append("  }\n");
        d2.append("  enum: {\n");
        d2.append("    shape: cylinder\n");
        d2.append("    style.fill: \"#F4E8FD\"\n");
        d2.append("    style.stroke: \"#9C27B0\"\n");
        d2.append("  }\n");
        d2.append("}\n\n");
        
        // Group by package
        Map<String, List<ClassRepresentation>> packageGroups = project.getClasses().stream()
            .collect(Collectors.groupingBy(ClassRepresentation::getPackageName));
        
        for (Map.Entry<String, List<ClassRepresentation>> entry : packageGroups.entrySet()) {
            String packageName = entry.getKey();
            List<ClassRepresentation> classes = entry.getValue();
            
            String pkgId = sanitizeNodeId(packageName);
            d2.append(pkgId).append(": {\n");
            d2.append("  label: \"").append(packageName).append("\"\n");
            d2.append("  style.stroke-width: 2\n");
            d2.append("  style.border-radius: 8\n");
            d2.append("  style.fill: \"#F5F5F5\"\n\n");
            
            for (ClassRepresentation cls : classes) {
                String nodeId = sanitizeNodeId(cls.getName());
                d2.append("  ").append(nodeId).append(": {\n");
                d2.append("    label: \"").append(cls.getName()).append("\"\n");
                
                if (cls.isInterface()) {
                    d2.append("    class: interface\n");
                } else if (cls.isAbstract()) {
                    d2.append("    class: abstract\n");
                } else if (cls.isEnum()) {
                    d2.append("    class: enum\n");
                }
                
                // Add tooltip with metrics
                d2.append("    tooltip: |md\n");
                d2.append("      **").append(cls.getName()).append("**\n");
                d2.append("      - Methods: ").append(cls.getMethods().size()).append("\n");
                d2.append("      - Fields: ").append(cls.getFields().size()).append("\n");
                Integer coupling = dependencyResult.efferentCoupling().get(cls.getFullyQualifiedName());
                if (coupling != null) {
                    d2.append("      - Dependencies: ").append(coupling).append("\n");
                }
                d2.append("    |\n");
                
                d2.append("  }\n");
            }
            
            d2.append("}\n\n");
        }
        
        // Add connections
        d2.append("# Connections\n");
        for (Map.Entry<String, Set<String>> entry : dependencyResult.classDependencies().entrySet()) {
            String source = entry.getKey();
            ClassRepresentation sourceClass = classMap.get(source);
            
            if (sourceClass != null) {
                String sourceId = sanitizeNodeId(sourceClass.getPackageName()) + "." + 
                                 sanitizeNodeId(sourceClass.getName());
                
                for (String target : entry.getValue()) {
                    ClassRepresentation targetClass = classMap.get(target);
                    if (targetClass != null) {
                        String targetId = sanitizeNodeId(targetClass.getPackageName()) + "." + 
                                        sanitizeNodeId(targetClass.getName());
                        
                        d2.append(sourceId).append(" -> ").append(targetId);
                        
                        if (isInheritance(sourceClass, targetClass)) {
                            d2.append(": extends {\n");
                            d2.append("  style.stroke: \"#2196F3\"\n");
                            d2.append("  style.stroke-width: 2\n");
                            d2.append("  target-arrowhead: triangle\n");
                            d2.append("}\n");
                        } else if (isImplementation(sourceClass, targetClass)) {
                            d2.append(": implements {\n");
                            d2.append("  style.stroke: \"#4CAF50\"\n");
                            d2.append("  style.stroke-dash: 3\n");
                            d2.append("  target-arrowhead: triangle\n");
                            d2.append("}\n");
                        } else {
                            d2.append(": uses {\n");
                            d2.append("  style.stroke: \"#757575\"\n");
                            d2.append("}\n");
                        }
                    }
                }
            }
        }
        
        return d2.toString();
    }
    
    // Helper methods
    
    private String sanitizeNodeId(String name) {
        return name.replaceAll("[^a-zA-Z0-9_]", "_");
    }
    
    private String getNodeColor(ClassRepresentation cls) {
        if (cls.isInterface()) return "#E8FDE8";
        if (cls.isAbstract()) return "#FFF4E8";
        if (cls.isEnum()) return "#F4E8FD";
        return "#E8F4FD";
    }
    
    private String getNodeShape(ClassRepresentation cls) {
        if (cls.isInterface()) return "diamond";
        if (cls.isEnum()) return "octagon";
        return "box";
    }
    
    private String generateTooltip(ClassRepresentation cls) {
        return String.format("%s\\nMethods: %d\\nFields: %d\\nPackage: %s",
            cls.getFullyQualifiedName(),
            cls.getMethods().size(),
            cls.getFields().size(),
            cls.getPackageName()
        ).replace("\"", "\\\"");
    }
    
    private String getEdgeStyle(ClassRepresentation source, ClassRepresentation target) {
        if (isInheritance(source, target)) {
            return "style=bold, color=blue";
        }
        if (isImplementation(source, target)) {
            return "style=dashed, color=green";
        }
        return "color=gray";
    }
    
    private String getEdgeLabel(ClassRepresentation source, ClassRepresentation target) {
        if (isInheritance(source, target)) return "extends";
        if (isImplementation(source, target)) return "implements";
        return "";
    }
    
    private boolean isInheritance(ClassRepresentation source, ClassRepresentation target) {
        return source.getExtendedTypes().contains(target.getName()) ||
               source.getExtendedTypes().contains(target.getFullyQualifiedName());
    }
    
    private boolean isImplementation(ClassRepresentation source, ClassRepresentation target) {
        return source.getImplementedInterfaces().contains(target.getName()) ||
               source.getImplementedInterfaces().contains(target.getFullyQualifiedName());
    }
    
    private String getClassType(ClassRepresentation cls) {
        if (cls.isInterface()) return "interface";
        if (cls.isAbstract()) return "abstract";
        if (cls.isEnum()) return "enum";
        return "class";
    }
    
    private String getDependencyType(ClassRepresentation source, ClassRepresentation target) {
        if (isInheritance(source, target)) return "inheritance";
        if (isImplementation(source, target)) return "implementation";
        return "association";
    }
    
    private String getVisibilitySymbol(String visibility) {
        return switch (visibility) {
            case "public" -> "+";
            case "private" -> "-";
            case "protected" -> "#";
            default -> "~";
        };
    }
    
    private String getSimpleName(String fullyQualifiedName) {
        int lastDot = fullyQualifiedName.lastIndexOf('.');
        return lastDot > 0 ? fullyQualifiedName.substring(lastDot + 1) : fullyQualifiedName;
    }
    
    private List<String> topologicalSort(Map<String, Set<String>> dependencies) {
        List<String> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        
        for (String node : dependencies.keySet()) {
            if (!visited.contains(node)) {
                topologicalSortUtil(node, dependencies, visited, recursionStack, result);
            }
        }
        
        Collections.reverse(result);
        return result;
    }
    
    private void topologicalSortUtil(String node, Map<String, Set<String>> dependencies,
                                   Set<String> visited, Set<String> recursionStack,
                                   List<String> result) {
        visited.add(node);
        recursionStack.add(node);
        
        Set<String> neighbors = dependencies.getOrDefault(node, new HashSet<>());
        for (String neighbor : neighbors) {
            if (!visited.contains(neighbor)) {
                topologicalSortUtil(neighbor, dependencies, visited, recursionStack, result);
            }
        }
        
        recursionStack.remove(node);
        result.add(node);
    }
    
    private Map<String, Object> calculateGraphMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        int nodeCount = project.getClasses().size();
        int edgeCount = dependencyResult.classDependencies().values().stream()
            .mapToInt(Set::size)
            .sum();
        
        metrics.put("nodeCount", nodeCount);
        metrics.put("edgeCount", edgeCount);
        metrics.put("density", nodeCount > 1 ? (double) edgeCount / (nodeCount * (nodeCount - 1)) : 0.0);
        metrics.put("circularDependencies", dependencyResult.circularDependencies().size());
        
        return metrics;
    }
    
    @Override
    public String getAnalyzerName() {
        return "Graph Analyzer";
    }
    
    @Override
    public void printResults(GraphResult results) {
        System.out.println("\n=== Graph Analysis Results ===");
        System.out.println("Generated graph formats:");
        System.out.println("  - DOT (Graphviz)");
        System.out.println("  - Mermaid");
        System.out.println("  - GraphML");
        System.out.println("  - PlantUML");
        System.out.println("  - D2");
        
        System.out.println("\nGraph Metrics:");
        results.graphMetrics().forEach((key, value) -> 
            System.out.println("  " + key + ": " + value));
    }
    
    @Override
    public void exportResults(GraphResult results, String outputPath) {
        String baseName = outputPath.substring(0, outputPath.lastIndexOf('.'));
        
        try {
            // Export DOT
            try (FileWriter writer = new FileWriter(baseName + ".dot")) {
                writer.write(results.dotGraph());
            }
            
            // Export Mermaid
            try (FileWriter writer = new FileWriter(baseName + ".mmd")) {
                writer.write(results.mermaidDiagram());
            }
            
            // Export GraphML
            try (FileWriter writer = new FileWriter(baseName + ".graphml")) {
                writer.write(results.graphML());
            }
            
            // Export PlantUML
            try (FileWriter writer = new FileWriter(baseName + ".puml")) {
                writer.write(results.plantUML());
            }
            
            // Export D2
            try (FileWriter writer = new FileWriter(baseName + ".d2")) {
                writer.write(results.d2Diagram());
            }
            
            System.out.println("Graph files exported to:");
            System.out.println("  - " + baseName + ".dot (use: dot -Tpng " + baseName + ".dot -o " + baseName + ".png)");
            System.out.println("  - " + baseName + ".mmd (paste into mermaid.live)");
            System.out.println("  - " + baseName + ".graphml (open with yEd or Gephi)");
            System.out.println("  - " + baseName + ".puml (use: plantuml " + baseName + ".puml)");
            System.out.println("  - " + baseName + ".d2 (use: d2 " + baseName + ".d2 " + baseName + ".svg)");
            
        } catch (IOException e) {
            System.err.println("Error exporting graph files: " + e.getMessage());
        }
    }
}