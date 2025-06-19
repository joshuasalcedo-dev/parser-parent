package io.joshuasalcedo.parser.java.analyzer;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import io.joshuasalcedo.parser.java.result.DependencyResult;
import io.joshuasalcedo.parser.java.model.*;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyzes project dependencies using JavaParser's symbol resolver for accurate type resolution
 */
public class DependencyAnalyzer extends AbstractAnalyzer<DependencyResult> {
    
    private final String projectPath;
    private final JavaSymbolSolver symbolSolver;
    private final Map<String, Set<String>> classDependencies = new HashMap<>();
    private final Map<String, Set<String>> methodCalls = new HashMap<>();
    private final Map<String, Set<String>> fieldReferences = new HashMap<>();
    private final Map<String, CompilationUnit> compilationUnits = new HashMap<>();
    
    public DependencyAnalyzer(ProjectRepresentation project, String projectPath) {
        super(project);
        this.projectPath = projectPath;
        this.symbolSolver = createSymbolSolver();
    }
    
    private JavaSymbolSolver createSymbolSolver() {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        
        // Add reflection type solver for JDK classes
        combinedTypeSolver.add(new ReflectionTypeSolver());
        
        // Add project source directories
        addProjectTypeSolver(combinedTypeSolver, "src/main/java");
        addProjectTypeSolver(combinedTypeSolver, "src/test/java");
        
        // Add dependency JARs if needed (could be extended to parse pom.xml)
        File libDir = new File(projectPath, "lib");
        if (libDir.exists() && libDir.isDirectory()) {
            File[] jars = libDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jars != null) {
                for (File jar : jars) {
                    try {
                        combinedTypeSolver.add(new JarTypeSolver(jar.getPath()));
                    } catch (Exception e) {
                        System.err.println("Failed to add JAR to type solver: " + jar.getName());
                    }
                }
            }
        }
        
        return new JavaSymbolSolver(combinedTypeSolver);
    }
    
    private void addProjectTypeSolver(CombinedTypeSolver solver, String relativePath) {
        File sourceDir = new File(projectPath, relativePath);
        if (sourceDir.exists() && sourceDir.isDirectory()) {
            solver.add(new JavaParserTypeSolver(sourceDir));
        }
    }
    
    @Override
    public DependencyResult analyze() {
        // Parse all compilation units and inject symbol solver
        parseAndInjectSymbolSolver();
        
        // Analyze dependencies with type resolution
        analyzeDependencies();
        
        // Calculate additional metrics
        Map<String, Integer> afferentCoupling = calculateAfferentCoupling();
        Map<String, Integer> efferentCoupling = calculateEfferentCoupling();
        Map<String, Double> instability = calculateInstability(afferentCoupling, efferentCoupling);
        List<List<String>> circularDependencies = detectCircularDependencies();
        Set<String> unusedClasses = detectUnusedClasses();
        Map<String, Set<String>> packageDependencies = analyzePackageLevelDependencies();
        
        return new DependencyResult(
            classDependencies,
            methodCalls,
            fieldReferences,
            circularDependencies,
            unusedClasses,
            afferentCoupling,
            efferentCoupling,
            instability,
            packageDependencies
        );
    }
    
    private void parseAndInjectSymbolSolver() {
        for (ClassRepresentation classRep : project.getClasses()) {
            try {
                File javaFile = new File(classRep.getFilePath());
                CompilationUnit cu = com.github.javaparser.StaticJavaParser.parse(javaFile);
                symbolSolver.inject(cu);
                compilationUnits.put(classRep.getFullyQualifiedName(), cu);
            } catch (Exception e) {
                System.err.println("Failed to parse file: " + classRep.getFilePath());
            }
        }
    }
    
    private void analyzeDependencies() {
        for (Map.Entry<String, CompilationUnit> entry : compilationUnits.entrySet()) {
            String className = entry.getKey();
            CompilationUnit cu = entry.getValue();
            
            Set<String> dependencies = new HashSet<>();
            Set<String> methods = new HashSet<>();
            Set<String> fields = new HashSet<>();
            
            cu.accept(new DependencyVisitor(className, dependencies, methods, fields), null);
            
            classDependencies.put(className, dependencies);
            methodCalls.put(className, methods);
            fieldReferences.put(className, fields);
        }
    }
    
    private class DependencyVisitor extends VoidVisitorAdapter<Void> {
        private final String currentClass;
        private final Set<String> dependencies;
        private final Set<String> methodCalls;
        private final Set<String> fieldRefs;
        
        public DependencyVisitor(String currentClass, Set<String> dependencies, 
                               Set<String> methodCalls, Set<String> fieldRefs) {
            this.currentClass = currentClass;
            this.dependencies = dependencies;
            this.methodCalls = methodCalls;
            this.fieldRefs = fieldRefs;
        }
        
        @Override
        public void visit(ClassOrInterfaceDeclaration n, Void arg) {
            // Analyze extended classes
            n.getExtendedTypes().forEach(extended -> {
                try {
                    ResolvedReferenceType resolvedType = extended.resolve().asReferenceType();
                    String qualifiedName = resolvedType.getQualifiedName();
                    if (isProjectClass(qualifiedName)) {
                        dependencies.add(qualifiedName);
                    }
                } catch (Exception e) {
                    // Fall back to string representation if resolution fails
                    dependencies.add(extended.getNameAsString());
                }
            });
            
            // Analyze implemented interfaces
            n.getImplementedTypes().forEach(implemented -> {
                try {
                    ResolvedReferenceType resolvedType = implemented.resolve().asReferenceType();
                    String qualifiedName = resolvedType.getQualifiedName();
                    if (isProjectClass(qualifiedName)) {
                        dependencies.add(qualifiedName);
                    }
                } catch (Exception e) {
                    dependencies.add(implemented.getNameAsString());
                }
            });
            
            super.visit(n, arg);
        }
        
        @Override
        public void visit(FieldDeclaration n, Void arg) {
            try {
                ResolvedType resolvedType = n.resolve().getType();
                if (resolvedType.isReferenceType()) {
                    String typeName = resolvedType.asReferenceType().getQualifiedName();
                    if (isProjectClass(typeName)) {
                        dependencies.add(typeName);
                    }
                }
            } catch (Exception e) {
                // Fall back to string representation
                String type = n.getCommonType().asString();
                if (isProjectClass(type)) {
                    dependencies.add(type);
                }
            }
            super.visit(n, arg);
        }
        
        @Override
        public void visit(MethodCallExpr n, Void arg) {
            try {
                ResolvedMethodDeclaration resolved = n.resolve();
                String declaringType = resolved.declaringType().getQualifiedName();
                
                if (isProjectClass(declaringType)) {
                    dependencies.add(declaringType);
                    methodCalls.add(declaringType + "." + resolved.getName());
                }
            } catch (Exception e) {
                // Could not resolve - track the method name at least
                methodCalls.add("UNRESOLVED." + n.getNameAsString());
            }
            super.visit(n, arg);
        }
        
        @Override
        public void visit(ObjectCreationExpr n, Void arg) {
            try {
                ResolvedType resolvedType = n.calculateResolvedType();
                if (resolvedType.isReferenceType()) {
                    String typeName = resolvedType.asReferenceType().getQualifiedName();
                    if (isProjectClass(typeName)) {
                        dependencies.add(typeName);
                    }
                }
            } catch (Exception e) {
                // Fall back to string representation
                String type = n.getType().asString();
                if (isProjectClass(type)) {
                    dependencies.add(type);
                }
            }
            super.visit(n, arg);
        }
        
        @Override
        public void visit(FieldAccessExpr n, Void arg) {
            try {
                ResolvedType scopeType = n.getScope().calculateResolvedType();
                if (scopeType.isReferenceType()) {
                    String typeName = scopeType.asReferenceType().getQualifiedName();
                    if (isProjectClass(typeName)) {
                        fieldRefs.add(typeName + "." + n.getNameAsString());
                    }
                }
            } catch (Exception e) {
                // Could not resolve
                fieldRefs.add("UNRESOLVED." + n.getNameAsString());
            }
            super.visit(n, arg);
        }
    }
    
    private boolean isProjectClass(String className) {
        // Check if this class belongs to the project
        return project.getClasses().stream()
            .anyMatch(c -> c.getFullyQualifiedName().equals(className) || 
                          c.getName().equals(className));
    }
    
    private Map<String, Integer> calculateAfferentCoupling() {
        Map<String, Integer> afferent = new HashMap<>();
        
        // Initialize all classes with 0
        project.getClasses().forEach(c -> afferent.put(c.getFullyQualifiedName(), 0));
        
        // Count incoming dependencies
        classDependencies.forEach((source, targets) -> {
            targets.forEach(target -> {
                afferent.merge(target, 1, Integer::sum);
            });
        });
        
        return afferent;
    }
    
    private Map<String, Integer> calculateEfferentCoupling() {
        Map<String, Integer> efferent = new HashMap<>();
        
        classDependencies.forEach((source, targets) -> {
            efferent.put(source, targets.size());
        });
        
        return efferent;
    }
    
    private Map<String, Double> calculateInstability(Map<String, Integer> afferent, 
                                                   Map<String, Integer> efferent) {
        Map<String, Double> instability = new HashMap<>();
        
        project.getClasses().forEach(c -> {
            String className = c.getFullyQualifiedName();
            int ca = afferent.getOrDefault(className, 0);
            int ce = efferent.getOrDefault(className, 0);
            
            if (ca + ce == 0) {
                instability.put(className, 0.0);
            } else {
                instability.put(className, (double) ce / (ca + ce));
            }
        });
        
        return instability;
    }
    
    private List<List<String>> detectCircularDependencies() {
        List<List<String>> cycles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        
        for (String node : classDependencies.keySet()) {
            if (!visited.contains(node)) {
                List<String> currentPath = new ArrayList<>();
                detectCycles(node, visited, recursionStack, currentPath, cycles);
            }
        }
        
        return cycles;
    }
    
    private void detectCycles(String node, Set<String> visited, Set<String> recursionStack,
                            List<String> currentPath, List<List<String>> cycles) {
        visited.add(node);
        recursionStack.add(node);
        currentPath.add(node);
        
        Set<String> neighbors = classDependencies.getOrDefault(node, new HashSet<>());
        for (String neighbor : neighbors) {
            if (!visited.contains(neighbor)) {
                detectCycles(neighbor, visited, recursionStack, currentPath, cycles);
            } else if (recursionStack.contains(neighbor)) {
                // Found a cycle
                int cycleStart = currentPath.indexOf(neighbor);
                if (cycleStart != -1) {
                    cycles.add(new ArrayList<>(currentPath.subList(cycleStart, currentPath.size())));
                }
            }
        }
        
        currentPath.remove(currentPath.size() - 1);
        recursionStack.remove(node);
    }
    
    private Set<String> detectUnusedClasses() {
        Set<String> allClasses = project.getClasses().stream()
            .map(ClassRepresentation::getFullyQualifiedName)
            .collect(Collectors.toSet());
        
        Set<String> referencedClasses = new HashSet<>();
        classDependencies.values().forEach(referencedClasses::addAll);
        
        Set<String> unreferencedClasses = new HashSet<>(allClasses);
        unreferencedClasses.removeAll(referencedClasses);
        
        // Filter out entry points (main classes, test classes, Spring beans, etc.)
        return unreferencedClasses.stream()
            .filter(this::isNotEntryPoint)
            .collect(Collectors.toSet());
    }
    
    private boolean isNotEntryPoint(String className) {
        ClassRepresentation classRep = project.getClasses().stream()
            .filter(c -> c.getFullyQualifiedName().equals(className))
            .findFirst()
            .orElse(null);
        
        if (classRep == null) return true;
        
        // Check for main method
        boolean hasMain = classRep.getMethods().stream()
            .anyMatch(m -> "main".equals(m.getName()) && 
                          m.isStatic() && 
                          "public".equals(m.getVisibility()));
        
        // Check for test class
        boolean isTest = className.endsWith("Test") || 
                        className.endsWith("Tests") ||
                        classRep.getFilePath().contains("/test/");
        
        // Check for Spring annotations
        boolean isSpringBean = classRep.getAnnotations().stream()
            .anyMatch(a -> Set.of("Component", "Service", "Repository", 
                                 "Controller", "RestController", "Configuration")
                          .contains(a));
        
        return !hasMain && !isTest && !isSpringBean;
    }
    
    private Map<String, Set<String>> analyzePackageLevelDependencies() {
        Map<String, Set<String>> packageDeps = new HashMap<>();
        
        classDependencies.forEach((sourceClass, targetClasses) -> {
            String sourcePackage = getPackageName(sourceClass);
            
            targetClasses.forEach(targetClass -> {
                String targetPackage = getPackageName(targetClass);
                
                if (!sourcePackage.equals(targetPackage)) {
                    packageDeps.computeIfAbsent(sourcePackage, k -> new HashSet<>())
                              .add(targetPackage);
                }
            });
        });
        
        return packageDeps;
    }
    
    private String getPackageName(String fullyQualifiedClassName) {
        int lastDot = fullyQualifiedClassName.lastIndexOf('.');
        return lastDot > 0 ? fullyQualifiedClassName.substring(0, lastDot) : "";
    }
    
    @Override
    public String getAnalyzerName() {
        return "Dependency Analyzer";
    }
    
    @Override
    public void printResults(DependencyResult results) {
        System.out.println("\n=== Dependency Analysis Results ===");
        
        // Circular dependencies
        if (!results.circularDependencies().isEmpty()) {
            System.out.println("\n⚠️  Circular Dependencies:");
            results.circularDependencies().forEach(cycle -> 
                System.out.println("  " + String.join(" -> ", cycle) + " -> " + cycle.get(0)));
        } else {
            System.out.println("\n✓ No circular dependencies found!");
        }
        
        // Unused classes
        if (!results.unusedClasses().isEmpty()) {
            System.out.println("\n⚠️  Potentially Unused Classes:");
            results.unusedClasses().forEach(cls -> System.out.println("  - " + cls));
        }
        
        // High coupling
        System.out.println("\nClasses with High Coupling (>10 dependencies):");
        results.efferentCoupling().entrySet().stream()
            .filter(e -> e.getValue() > 10)
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .forEach(e -> System.out.printf("  %s: %d outgoing dependencies%n", 
                                          e.getKey(), e.getValue()));
        
        // Instability metrics
        System.out.println("\nMost Unstable Classes (I > 0.8):");
        results.instability().entrySet().stream()
            .filter(e -> e.getValue() > 0.8)
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(10)
            .forEach(e -> System.out.printf("  %s: %.2f%n", e.getKey(), e.getValue()));
        
        // Package dependencies
        System.out.println("\nPackage Dependencies:");
        results.packageDependencies().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(e -> {
                System.out.println("  " + e.getKey() + " depends on:");
                e.getValue().forEach(dep -> System.out.println("    - " + dep));
            });
    }
    
    @Override
    public void exportResults(DependencyResult results, String outputPath) {
        // Export to various formats (JSON, GraphML, DOT)
        // Implementation would go here
    }
}