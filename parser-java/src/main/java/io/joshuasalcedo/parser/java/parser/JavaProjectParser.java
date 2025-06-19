package io.joshuasalcedo.parser.java.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.*;

import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.joshuasalcedo.parser.java.model.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

// Statistics Records
record ProjectStatistics(
    int totalClasses,
    int totalInterfaces,
    int totalEnums,
    int totalAbstractClasses,
    int totalMethods,
    int totalFields,
    int totalConstructors,
    int totalPublicMethods,
    int totalPrivateMethods,
    int totalProtectedMethods,
    int totalStaticMethods,
    int totalAnnotations,
    int totalLinesOfCode,
    Map<String, Integer> annotationUsage,
    Map<String, Integer> packageDistribution,
    List<ClassComplexity> classComplexities
) {}

record ClassComplexity(
    String className,
    int methodCount,
    int fieldCount,
    int dependencyCount
) {}

record ProjectAnalysis(
    ProjectRepresentation project,
    ProjectStatistics statistics,
    long analysisTimestamp
) {}

public class JavaProjectParser {
    
    private final Map<String, ClassRepresentation> classMap = new HashMap<>();
    private final JavaParser javaParser;
    
    public JavaProjectParser() {
        ParserConfiguration config = new ParserConfiguration();
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        this.javaParser = new JavaParser(config);
    }
    
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }
        
        String command = args[0];
        String[] commandArgs = Arrays.copyOfRange(args, 1, args.length);
        
        switch (command) {
            case "analyze" -> mainAnalyze(commandArgs);
            case "dependency" -> mainDependencyAnalysis(commandArgs);
            case "metrics" -> mainCodeMetrics(commandArgs);
            case "api" -> mainApiDocumentation(commandArgs);
            case "refactor" -> mainRefactoringSuggestions(commandArgs);
            case "security" -> mainSecurityAudit(commandArgs);
            case "test" -> mainTestCoverage(commandArgs);
            case "diagram" -> mainClassDiagram(commandArgs);
            case "compare" -> mainProjectComparison(commandArgs);
            case "lint" -> mainCodeLinter(commandArgs);
            default -> {
                System.err.println("Unknown command: " + command);
                printUsage();
            }
        }
    }
    
    private static void printUsage() {
        System.out.println("Usage: java JavaProjectParser <command> [options]");
        System.out.println("\nCommands:");
        System.out.println("  analyze <project-dir> [output.json]     - Full project analysis with statistics");
        System.out.println("  dependency <project-dir>                - Analyze class dependencies and circular dependencies");
        System.out.println("  metrics <project-dir>                   - Calculate code metrics (complexity, coupling, etc.)");
        System.out.println("  api <project-dir> [output.html]         - Generate API documentation");
        System.out.println("  refactor <project-dir>                  - Suggest refactoring opportunities");
        System.out.println("  security <project-dir>                  - Security audit for common vulnerabilities");
        System.out.println("  test <project-dir>                      - Analyze test coverage and suggest missing tests");
        System.out.println("  diagram <project-dir> [output.puml]     - Generate PlantUML class diagram");
        System.out.println("  compare <project1> <project2>           - Compare two projects");
        System.out.println("  lint <project-dir>                      - Check code style and conventions");
    }
    
    // Original main method for backward compatibility
    public static void mainAnalyze(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java JavaProjectParser analyze <project-root-directory> [output-json-file]");
            return;
        }
        
        JavaProjectParser parser = new JavaProjectParser();
        ProjectRepresentation project = parser.parseProject(args[0]);
        
        // Calculate statistics
        ProjectStatistics statistics = parser.calculateStatistics(project);
        
        // Create analysis object
        ProjectAnalysis analysis = new ProjectAnalysis(
            project,
            statistics,
            System.currentTimeMillis()
        );
        
        // Print the project structure
        System.out.println("Project Structure:");
        System.out.println("==================");
        project.printStructure();
        
        // Print statistics
        System.out.println("\nProject Statistics:");
        System.out.println("==================");
        parser.printStatistics(statistics);
        
        // Export to JSON if output file specified
        if (args.length > 1) {
            parser.exportToJson(analysis, args[1]);
            System.out.println("\nProject analysis exported to: " + args[1]);
        }
    }
    
    // Dependency Analysis - finds circular dependencies, unused classes, etc.
    public static void mainDependencyAnalysis(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java JavaProjectParser dependency <project-root-directory>");
            return;
        }
        
        JavaProjectParser parser = new JavaProjectParser();
        ProjectRepresentation project = parser.parseProject(args[0]);
        DependencyAnalyzer analyzer = parser.new DependencyAnalyzer();
        
        System.out.println("Dependency Analysis");
        System.out.println("==================");
        
        Map<String, Set<String>> dependencies = analyzer.analyzeDependencies(project);
        List<List<String>> circularDeps = analyzer.findCircularDependencies(dependencies);
        Set<String> unusedClasses = analyzer.findUnusedClasses(project, dependencies);
        
        System.out.println("\nClass Dependencies:");
        dependencies.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                if (!entry.getValue().isEmpty()) {
                    System.out.println(entry.getKey() + " depends on:");
                    entry.getValue().forEach(dep -> System.out.println("  - " + dep));
                }
            });
        
        if (!circularDeps.isEmpty()) {
            System.out.println("\n⚠️  Circular Dependencies Found:");
            circularDeps.forEach(cycle -> {
                System.out.println("  " + String.join(" -> ", cycle) + " -> " + cycle.get(0));
            });
        } else {
            System.out.println("\n✓ No circular dependencies found!");
        }
        
        if (!unusedClasses.isEmpty()) {
            System.out.println("\n⚠️  Potentially Unused Classes:");
            unusedClasses.forEach(cls -> System.out.println("  - " + cls));
        }
    }
    
    // Code Metrics - complexity, coupling, cohesion
    public static void mainCodeMetrics(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java JavaProjectParser metrics <project-root-directory>");
            return;
        }
        
        JavaProjectParser parser = new JavaProjectParser();
        ProjectRepresentation project = parser.parseProject(args[0]);
        MetricsCalculator calculator = parser.new MetricsCalculator();
        
        System.out.println("Code Metrics Analysis");
        System.out.println("====================");
        
        List<ClassMetrics> metrics = calculator.calculateMetrics(project);
        
        // Sort by complexity
        metrics.sort((a, b) -> Double.compare(b.complexity, a.complexity));
        
        System.out.println("\nClass Metrics (sorted by complexity):");
        System.out.println("Class Name | Methods | Fields | Complexity | Coupling | Cohesion | Maintainability Index");
        System.out.println("-".repeat(90));
        
        metrics.forEach(m -> {
            System.out.printf("%-30s | %7d | %6d | %10.2f | %8d | %8.2f | %20.2f%n",
                m.className.length() > 30 ? "..." + m.className.substring(Math.max(0, m.className.length() - 27)) : m.className,
                m.methodCount,
                m.fieldCount,
                m.complexity,
                m.coupling,
                m.cohesion,
                m.maintainabilityIndex
            );
        });
        
        // Summary statistics
        double avgComplexity = metrics.stream().mapToDouble(m -> m.complexity).average().orElse(0);
        double avgMaintainability = metrics.stream().mapToDouble(m -> m.maintainabilityIndex).average().orElse(0);
        
        System.out.println("\nSummary:");
        System.out.println("Average Complexity: " + String.format("%.2f", avgComplexity));
        System.out.println("Average Maintainability Index: " + String.format("%.2f", avgMaintainability));
        
        // Warnings
        System.out.println("\n⚠️  Classes with High Complexity (>10):");
        metrics.stream()
            .filter(m -> m.complexity > 10)
            .forEach(m -> System.out.println("  - " + m.className + " (complexity: " + String.format("%.2f", m.complexity) + ")"));
    }
    
    // API Documentation Generator
    public static void mainApiDocumentation(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java JavaProjectParser api <project-root-directory> [output.html]");
            return;
        }
        
        JavaProjectParser parser = new JavaProjectParser();
        ProjectRepresentation project = parser.parseProject(args[0]);
        ApiDocGenerator docGen = parser.new ApiDocGenerator();
        
        String outputFile = args.length > 1 ? args[1] : "api-docs.html";
        
        System.out.println("Generating API Documentation...");
        docGen.generateHtmlDocs(project, outputFile);
        System.out.println("API documentation generated: " + outputFile);
        
        // Also generate summary
        System.out.println("\nAPI Summary:");
        System.out.println("============");
        Map<String, List<ClassRepresentation>> packageGroups = project.getClasses().stream()
            .collect(Collectors.groupingBy(ClassRepresentation::getPackageName));
        
        packageGroups.forEach((pkg, classes) -> {
            System.out.println("\nPackage: " + pkg);
            classes.forEach(cls -> {
                System.out.println("  " + (cls.isInterface() ? "Interface" : "Class") + ": " + cls.getName());
                long publicMethods = cls.getMethods().stream()
                    .filter(m -> "public".equals(m.getVisibility()))
                    .count();
                if (publicMethods > 0) {
                    System.out.println("    Public methods: " + publicMethods);
                }
            });
        });
    }
    
    // Refactoring Suggestions
    public static void mainRefactoringSuggestions(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java JavaProjectParser refactor <project-root-directory>");
            return;
        }
        
        JavaProjectParser parser = new JavaProjectParser();
        ProjectRepresentation project = parser.parseProject(args[0]);
        RefactoringAnalyzer analyzer = parser.new RefactoringAnalyzer();
        
        System.out.println("Refactoring Suggestions");
        System.out.println("======================");
        
        List<RefactoringSuggestion> suggestions = analyzer.analyzePotentialRefactorings(project);
        
        suggestions.forEach(suggestion -> {
            System.out.println("\n" + suggestion.type + ": " + suggestion.target);
            System.out.println("  Reason: " + suggestion.reason);
            System.out.println("  Suggestion: " + suggestion.suggestion);
        });
        
        System.out.println("\nTotal suggestions: " + suggestions.size());
    }
    
    // Security Audit
    public static void mainSecurityAudit(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java JavaProjectParser security <project-root-directory>");
            return;
        }
        
        JavaProjectParser parser = new JavaProjectParser();
        ProjectRepresentation project = parser.parseProject(args[0]);
        SecurityAuditor auditor = parser.new SecurityAuditor();
        
        System.out.println("Security Audit");
        System.out.println("==============");
        
        List<SecurityIssue> issues = auditor.auditProject(project);
        
        if (issues.isEmpty()) {
            System.out.println("\n✓ No security issues found!");
        } else {
            System.out.println("\n⚠️  Security Issues Found:");
            issues.forEach(issue -> {
                System.out.println("\n" + issue.severity + ": " + issue.type);
                System.out.println("  Location: " + issue.location);
                System.out.println("  Description: " + issue.description);
                System.out.println("  Recommendation: " + issue.recommendation);
            });
        }
    }
    
    // Test Coverage Analysis
    public static void mainTestCoverage(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java JavaProjectParser test <project-root-directory>");
            return;
        }
        
        JavaProjectParser parser = new JavaProjectParser();
        ProjectRepresentation project = parser.parseProject(args[0]);
        TestAnalyzer analyzer = parser.new TestAnalyzer();
        
        System.out.println("Test Coverage Analysis");
        System.out.println("=====================");
        
        TestCoverageReport report = analyzer.analyzeTestCoverage(project);
        
        System.out.println("\nTest Statistics:");
        System.out.println("Total Classes: " + report.totalClasses);
        System.out.println("Test Classes: " + report.testClasses);
        System.out.println("Classes with Tests: " + report.classesWithTests);
        System.out.println("Classes without Tests: " + report.classesWithoutTests.size());
        
        if (!report.classesWithoutTests.isEmpty()) {
            System.out.println("\n⚠️  Classes without tests:");
            report.classesWithoutTests.forEach(cls -> System.out.println("  - " + cls));
        }
        
        System.out.println("\nTest Methods by Type:");
        report.testMethodsByAnnotation.forEach((annotation, count) -> 
            System.out.println("  @" + annotation + ": " + count));
    }
    
    // PlantUML Class Diagram Generator
    public static void mainClassDiagram(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java JavaProjectParser diagram <project-root-directory> [output.puml]");
            return;
        }
        
        JavaProjectParser parser = new JavaProjectParser();
        ProjectRepresentation project = parser.parseProject(args[0]);
        DiagramGenerator generator = parser.new DiagramGenerator();
        
        String outputFile = args.length > 1 ? args[1] : "class-diagram.puml";
        
        System.out.println("Generating PlantUML Class Diagram...");
        generator.generatePlantUML(project, outputFile);
        System.out.println("PlantUML diagram generated: " + outputFile);
        System.out.println("\nTo render the diagram, use:");
        System.out.println("  plantuml " + outputFile);
    }
    
    // Project Comparison
    public static void mainProjectComparison(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java JavaProjectParser compare <project1-dir> <project2-dir>");
            return;
        }
        
        JavaProjectParser parser = new JavaProjectParser();
        ProjectRepresentation project1 = parser.parseProject(args[0]);
        ProjectRepresentation project2 = parser.parseProject(args[1]);
        
        System.out.println("Project Comparison");
        System.out.println("==================");
        System.out.println("Project 1: " + project1.getName());
        System.out.println("Project 2: " + project2.getName());
        
        ProjectStatistics stats1 = parser.calculateStatistics(project1);
        ProjectStatistics stats2 = parser.calculateStatistics(project2);
        
        System.out.println("\nMetrics Comparison:");
        System.out.println("Metric | Project 1 | Project 2 | Difference");
        System.out.println("-".repeat(50));
        
        compareMetric("Classes", stats1.totalClasses(), stats2.totalClasses());
        compareMetric("Interfaces", stats1.totalInterfaces(), stats2.totalInterfaces());
        compareMetric("Methods", stats1.totalMethods(), stats2.totalMethods());
        compareMetric("Fields", stats1.totalFields(), stats2.totalFields());
        compareMetric("Lines of Code", stats1.totalLinesOfCode(), stats2.totalLinesOfCode());
    }
    
    // Code Linter
    public static void mainCodeLinter(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java JavaProjectParser lint <project-root-directory>");
            return;
        }
        
        JavaProjectParser parser = new JavaProjectParser();
        ProjectRepresentation project = parser.parseProject(args[0]);
        CodeLinter linter = parser.new CodeLinter();
        
        System.out.println("Code Style Analysis");
        System.out.println("==================");
        
        List<LintIssue> issues = linter.lintProject(project);
        
        if (issues.isEmpty()) {
            System.out.println("\n✓ No style issues found!");
        } else {
            Map<String, List<LintIssue>> issuesByType = issues.stream()
                .collect(Collectors.groupingBy(i -> i.type));
            
            issuesByType.forEach((type, typeIssues) -> {
                System.out.println("\n" + type + " (" + typeIssues.size() + " issues):");
                typeIssues.stream().limit(5).forEach(issue -> 
                    System.out.println("  - " + issue.location + ": " + issue.message));
                if (typeIssues.size() > 5) {
                    System.out.println("  ... and " + (typeIssues.size() - 5) + " more");
                }
            });
        }
    }
    
    private static void compareMetric(String name, int val1, int val2) {
        int diff = val2 - val1;
        String sign = diff > 0 ? "+" : "";
        System.out.printf("%-15s | %9d | %9d | %s%d%n", name, val1, val2, sign, diff);
    }
    
    public ProjectRepresentation parseProject(String rootPath) {
        File rootDir = new File(rootPath);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            throw new IllegalArgumentException("Invalid project root directory: " + rootPath);
        }
        
        ProjectRepresentation project = new ProjectRepresentation(rootDir.getName(), rootPath);
        
        // Parse all Java files in src/main/java and src/test/java (Maven structure)
        List<String> sourcePaths = Arrays.asList(
            "src/main/java",
            "src/test/java"
        );
        
        for (String sourcePath : sourcePaths) {
            Path sourceDir = Paths.get(rootPath, sourcePath);
            if (Files.exists(sourceDir)) {
                parseDirectory(sourceDir, project);
            }
        }
        
        return project;
    }
    
    private void parseDirectory(Path directory, ProjectRepresentation project) {
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".java")) {
                        try {
                            ClassRepresentation classRep = parseJavaFile(file.toFile());
                            if (classRep != null) {
                                project.addClass(classRep);
                                classMap.put(classRep.getFullyQualifiedName(), classRep);
                            }
                        } catch (Exception e) {
                            System.err.println("Error parsing file: " + file + " - " + e.getMessage());
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.err.println("Error walking directory: " + directory + " - " + e.getMessage());
        }
    }
    
    private ClassRepresentation parseJavaFile(File file) throws IOException {
        ParseResult<CompilationUnit> parseResult = javaParser.parse(file);
        
        if (!parseResult.isSuccessful()) {
            System.err.println("Failed to parse: " + file);
            return null;
        }
        
        CompilationUnit cu = parseResult.getResult().orElse(null);
        if (cu == null) return null;
        
        // Get package name
        String packageName = cu.getPackageDeclaration()
            .map(pd -> pd.getNameAsString())
            .orElse("");
        
        // Process all type declarations (classes, interfaces, enums)
        List<TypeDeclaration<?>> types = cu.getTypes();
        if (types.isEmpty()) return null;
        
        // For simplicity, we'll process the first public type or the first type
        TypeDeclaration<?> mainType = types.stream()
            .filter(t -> t.isPublic())
            .findFirst()
            .orElse(types.get(0));
        
        return parseTypeDeclaration(mainType, packageName, file.getPath());
    }
    
    private ClassRepresentation parseTypeDeclaration(TypeDeclaration<?> type, String packageName, String filePath) {
        ClassRepresentation classRep = new ClassRepresentation();
        classRep.setName(type.getNameAsString());
        classRep.setPackageName(packageName);
        classRep.setFilePath(filePath);
        
        // Determine type
        if (type instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration cid = (ClassOrInterfaceDeclaration) type;
            classRep.setInterface(cid.isInterface());
            classRep.setAbstract(cid.isAbstract());
            
            // Extended types
            cid.getExtendedTypes().forEach(extended -> {
                classRep.addExtendedType(extended.getNameAsString());
            });
            
            // Implemented interfaces
            cid.getImplementedTypes().forEach(implemented -> {
                classRep.addImplementedInterface(implemented.getNameAsString());
            });
        } else if (type instanceof EnumDeclaration) {
            classRep.setEnum(true);
        }
        
        // Annotations
        type.getAnnotations().forEach(annotation -> {
            classRep.addAnnotation(annotation.getNameAsString());
        });
        
        // Fields
        type.getFields().forEach(field -> {
            FieldRepresentation fieldRep = parseField(field);
            classRep.addField(fieldRep);
        });
        
        // Methods
        type.getMethods().forEach(method -> {
            MethodRepresentation methodRep = parseMethod(method);
            classRep.addMethod(methodRep);
        });
        
        // Constructors
        type.getConstructors().forEach(constructor -> {
            ConstructorRepresentation constructorRep = parseConstructor(constructor);
            classRep.addConstructor(constructorRep);
        });
        
        return classRep;
    }
    
    private FieldRepresentation parseField(FieldDeclaration field) {
        FieldRepresentation fieldRep = new FieldRepresentation();
        
        field.getVariables().forEach(variable -> {
            fieldRep.setName(variable.getNameAsString());
            fieldRep.setType(field.getElementType().asString());
        });
        
        fieldRep.setStatic(field.isStatic());
        fieldRep.setFinal(field.isFinal());
        fieldRep.setVisibility(getVisibility(field));
        
        field.getAnnotations().forEach(annotation -> {
            fieldRep.addAnnotation(annotation.getNameAsString());
        });
        
        return fieldRep;
    }
    
    private MethodRepresentation parseMethod(MethodDeclaration method) {
        MethodRepresentation methodRep = new MethodRepresentation();
        methodRep.setName(method.getNameAsString());
        methodRep.setReturnType(method.getType().asString());
        methodRep.setStatic(method.isStatic());
        methodRep.setAbstract(method.isAbstract());
        methodRep.setVisibility(getVisibility(method));
        
        // Parameters
        method.getParameters().forEach(param -> {
            ParameterRepresentation paramRep = new ParameterRepresentation(
                param.getNameAsString(),
                param.getType().asString()
            );
            methodRep.addParameter(paramRep);
        });
        
        // Annotations
        method.getAnnotations().forEach(annotation -> {
            methodRep.addAnnotation(annotation.getNameAsString());
        });
        
        return methodRep;
    }
    
    private ConstructorRepresentation parseConstructor(ConstructorDeclaration constructor) {
        ConstructorRepresentation constructorRep = new ConstructorRepresentation();
        constructorRep.setVisibility(getVisibility(constructor));
        
        // Parameters
        constructor.getParameters().forEach(param -> {
            ParameterRepresentation paramRep = new ParameterRepresentation(
                param.getNameAsString(),
                param.getType().asString()
            );
            constructorRep.addParameter(paramRep);
        });
        
        return constructorRep;
    }
    
    private String getVisibility(BodyDeclaration<?> declaration) {
        // Use the NodeWithModifiers interface to check modifiers
        if (declaration instanceof NodeWithModifiers) {
            NodeWithModifiers<?> nodeWithModifiers = (NodeWithModifiers<?>) declaration;
            if (nodeWithModifiers.hasModifier(Modifier.Keyword.PUBLIC)) return "public";
            if (nodeWithModifiers.hasModifier(Modifier.Keyword.PROTECTED)) return "protected";
            if (nodeWithModifiers.hasModifier(Modifier.Keyword.PRIVATE)) return "private";
        }
        return "package-private";
    }
    
    private ProjectStatistics calculateStatistics(ProjectRepresentation project) {
        int totalClasses = 0, totalInterfaces = 0, totalEnums = 0, totalAbstractClasses = 0;
        int totalMethods = 0, totalFields = 0, totalConstructors = 0;
        int totalPublicMethods = 0, totalPrivateMethods = 0, totalProtectedMethods = 0;
        int totalStaticMethods = 0, totalAnnotations = 0, totalLinesOfCode = 0;
        
        Map<String, Integer> annotationUsage = new HashMap<>();
        Map<String, Integer> packageDistribution = new HashMap<>();
        List<ClassComplexity> classComplexities = new ArrayList<>();
        
        for (ClassRepresentation clazz : project.getClasses()) {
            // Count class types
            if (clazz.isInterface()) {
                totalInterfaces++;
            } else if (clazz.isEnum()) {
                totalEnums++;
            } else {
                totalClasses++;
                if (clazz.isAbstract()) {
                    totalAbstractClasses++;
                }
            }
            
            // Package distribution
            packageDistribution.merge(clazz.getPackageName(), 1, Integer::sum);
            
            // Count annotations
            for (String annotation : clazz.getAnnotations()) {
                annotationUsage.merge(annotation, 1, Integer::sum);
                totalAnnotations++;
            }
            
            // Fields
            totalFields += clazz.getFields().size();
            for (FieldRepresentation field : clazz.getFields()) {
                for (String annotation : field.getAnnotations()) {
                    annotationUsage.merge(annotation, 1, Integer::sum);
                    totalAnnotations++;
                }
            }
            
            // Constructors
            totalConstructors += clazz.getConstructors().size();
            
            // Methods
            totalMethods += clazz.getMethods().size();
            for (MethodRepresentation method : clazz.getMethods()) {
                switch (method.getVisibility()) {
                    case "public" -> totalPublicMethods++;
                    case "private" -> totalPrivateMethods++;
                    case "protected" -> totalProtectedMethods++;
                }
                if (method.isStatic()) {
                    totalStaticMethods++;
                }
                for (String annotation : method.getAnnotations()) {
                    annotationUsage.merge(annotation, 1, Integer::sum);
                    totalAnnotations++;
                }
            }
            
            // Calculate dependencies (simplified - based on extended/implemented types)
            int dependencies = clazz.getExtendedTypes().size() + clazz.getImplementedInterfaces().size();
            
            // Create complexity entry
            classComplexities.add(new ClassComplexity(
                clazz.getFullyQualifiedName(),
                clazz.getMethods().size(),
                clazz.getFields().size(),
                dependencies
            ));
            
            // Estimate lines of code (rough approximation)
            totalLinesOfCode += estimateLinesOfCode(clazz);
        }
        
        // Sort complexities by method count descending
        classComplexities.sort((a, b) -> Integer.compare(b.methodCount(), a.methodCount()));
        
        return new ProjectStatistics(
            totalClasses,
            totalInterfaces,
            totalEnums,
            totalAbstractClasses,
            totalMethods,
            totalFields,
            totalConstructors,
            totalPublicMethods,
            totalPrivateMethods,
            totalProtectedMethods,
            totalStaticMethods,
            totalAnnotations,
            totalLinesOfCode,
            annotationUsage,
            packageDistribution,
            classComplexities.stream().limit(10).collect(Collectors.toList()) // Top 10 complex classes
        );
    }
    
    private int estimateLinesOfCode(ClassRepresentation clazz) {
        // Rough estimation based on class structure
        int lines = 10; // Class declaration and braces
        lines += clazz.getFields().size() * 2;
        lines += clazz.getConstructors().size() * 5;
        lines += clazz.getMethods().size() * 10;
        return lines;
    }
    
    private void printStatistics(ProjectStatistics stats) {
        System.out.println("Total Classes: " + stats.totalClasses());
        System.out.println("Total Interfaces: " + stats.totalInterfaces());
        System.out.println("Total Enums: " + stats.totalEnums());
        System.out.println("Total Abstract Classes: " + stats.totalAbstractClasses());
        System.out.println("Total Methods: " + stats.totalMethods());
        System.out.println("  - Public: " + stats.totalPublicMethods());
        System.out.println("  - Private: " + stats.totalPrivateMethods());
        System.out.println("  - Protected: " + stats.totalProtectedMethods());
        System.out.println("  - Static: " + stats.totalStaticMethods());
        System.out.println("Total Fields: " + stats.totalFields());
        System.out.println("Total Constructors: " + stats.totalConstructors());
        System.out.println("Total Annotations Used: " + stats.totalAnnotations());
        System.out.println("Estimated Lines of Code: " + stats.totalLinesOfCode());
        
        System.out.println("\nPackage Distribution:");
        stats.packageDistribution().entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(10)
            .forEach(entry -> System.out.println("  " + entry.getKey() + ": " + entry.getValue() + " classes"));
        
        System.out.println("\nMost Used Annotations:");
        stats.annotationUsage().entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(10)
            .forEach(entry -> System.out.println("  @" + entry.getKey() + ": " + entry.getValue() + " times"));
        
        System.out.println("\nMost Complex Classes (by method count):");
        stats.classComplexities().forEach(complexity -> 
            System.out.println("  " + complexity.className() + 
                " (Methods: " + complexity.methodCount() + 
                ", Fields: " + complexity.fieldCount() + 
                ", Dependencies: " + complexity.dependencyCount() + ")"));
    }
    
    private void exportToJson(ProjectAnalysis analysis, String outputFile) {
        Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
        
        try (FileWriter writer = new FileWriter(outputFile)) {
            gson.toJson(analysis, writer);
        } catch (IOException e) {
            System.err.println("Error writing JSON file: " + e.getMessage());
        }
    }
    
    // Inner classes for different analyzers
    
    class DependencyAnalyzer {
        Map<String, Set<String>> analyzeDependencies(ProjectRepresentation project) {
            Map<String, Set<String>> dependencies = new HashMap<>();
            
            for (ClassRepresentation cls : project.getClasses()) {
                Set<String> deps = new HashSet<>();
                deps.addAll(cls.getExtendedTypes());
                deps.addAll(cls.getImplementedInterfaces());
                
                // Add field type dependencies
                for (FieldRepresentation field : cls.getFields()) {
                    String type = field.getType();
                    if (!isPrimitiveType(type) && !type.startsWith("java.")) {
                        deps.add(type);
                    }
                }
                
                // Add method return type and parameter dependencies
                for (MethodRepresentation method : cls.getMethods()) {
                    String returnType = method.getReturnType();
                    if (!isPrimitiveType(returnType) && !returnType.startsWith("java.")) {
                        deps.add(returnType);
                    }
                    for (ParameterRepresentation param : method.getParameters()) {
                        String paramType = param.getType();
                        if (!isPrimitiveType(paramType) && !paramType.startsWith("java.")) {
                            deps.add(paramType);
                        }
                    }
                }
                
                dependencies.put(cls.getName(), deps);
            }
            
            return dependencies;
        }
        
        List<List<String>> findCircularDependencies(Map<String, Set<String>> dependencies) {
            List<List<String>> cycles = new ArrayList<>();
            Set<String> visited = new HashSet<>();
            Set<String> recursionStack = new HashSet<>();
            
            for (String node : dependencies.keySet()) {
                if (!visited.contains(node)) {
                    List<String> currentPath = new ArrayList<>();
                    findCycles(node, dependencies, visited, recursionStack, currentPath, cycles);
                }
            }
            
            return cycles;
        }
        
        private void findCycles(String node, Map<String, Set<String>> graph, 
                               Set<String> visited, Set<String> recursionStack,
                               List<String> currentPath, List<List<String>> cycles) {
            visited.add(node);
            recursionStack.add(node);
            currentPath.add(node);
            
            Set<String> neighbors = graph.getOrDefault(node, new HashSet<>());
            for (String neighbor : neighbors) {
                if (!visited.contains(neighbor)) {
                    findCycles(neighbor, graph, visited, recursionStack, currentPath, cycles);
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
        
        Set<String> findUnusedClasses(ProjectRepresentation project, Map<String, Set<String>> dependencies) {
            Set<String> allClasses = project.getClasses().stream()
                .map(ClassRepresentation::getName)
                .collect(Collectors.toSet());
            
            Set<String> usedClasses = new HashSet<>();
            dependencies.values().forEach(usedClasses::addAll);
            
            Set<String> unusedClasses = new HashSet<>(allClasses);
            unusedClasses.removeAll(usedClasses);
            
            // Remove main classes and test classes from unused
            unusedClasses.removeIf(cls -> {
                ClassRepresentation classRep = classMap.get(cls);
                return classRep != null && (hasMainMethod(classRep) || isTestClass(classRep));
            });
            
            return unusedClasses;
        }
        
        private boolean hasMainMethod(ClassRepresentation cls) {
            return cls.getMethods().stream()
                .anyMatch(m -> "main".equals(m.getName()) && 
                             "public".equals(m.getVisibility()) &&
                             m.isStatic());
        }
        
        private boolean isTestClass(ClassRepresentation cls) {
            return cls.getName().endsWith("Test") || 
                   cls.getName().endsWith("Tests") ||
                   cls.getAnnotations().stream().anyMatch(a -> a.contains("Test"));
        }
        
        private boolean isPrimitiveType(String type) {
            return Set.of("int", "long", "double", "float", "boolean", "char", "byte", "short", "void")
                .contains(type);
        }
    }
    
    class MetricsCalculator {
        List<ClassMetrics> calculateMetrics(ProjectRepresentation project) {
            return project.getClasses().stream()
                .map(this::calculateClassMetrics)
                .collect(Collectors.toList());
        }
        
        private ClassMetrics calculateClassMetrics(ClassRepresentation cls) {
            int methodCount = cls.getMethods().size();
            int fieldCount = cls.getFields().size();
            
            // Cyclomatic complexity (simplified)
            double complexity = 1; // Base complexity
            for (MethodRepresentation method : cls.getMethods()) {
                complexity += estimateMethodComplexity(method);
            }
            
            // Coupling (number of dependencies)
            int coupling = cls.getExtendedTypes().size() + 
                          cls.getImplementedInterfaces().size() +
                          countTypeDependencies(cls);
            
            // Cohesion (simplified - ratio of internal method calls)
            double cohesion = calculateCohesion(cls);
            
            // Maintainability Index
            double maintainabilityIndex = calculateMaintainabilityIndex(
                complexity, coupling, cohesion, methodCount);
            
            return new ClassMetrics(
                cls.getFullyQualifiedName(),
                methodCount,
                fieldCount,
                complexity,
                coupling,
                cohesion,
                maintainabilityIndex
            );
        }
        
        private double estimateMethodComplexity(MethodRepresentation method) {
            // Simplified complexity estimation
            double complexity = 1;
            complexity += method.getParameters().size() * 0.5;
            if (method.isAbstract()) complexity -= 0.5;
            return complexity;
        }
        
        private int countTypeDependencies(ClassRepresentation cls) {
            Set<String> types = new HashSet<>();
            
            cls.getFields().forEach(f -> types.add(f.getType()));
            cls.getMethods().forEach(m -> {
                types.add(m.getReturnType());
                m.getParameters().forEach(p -> types.add(p.getType()));
            });
            
            return (int) types.stream()
                .filter(t -> !isPrimitiveType(t) && !t.startsWith("java."))
                .count();
        }
        
        private double calculateCohesion(ClassRepresentation cls) {
            if (cls.getMethods().size() <= 1) return 1.0;
            // Simplified cohesion - based on method/field ratio
            return Math.min(1.0, (double) cls.getFields().size() / cls.getMethods().size());
        }
        
        private double calculateMaintainabilityIndex(double complexity, int coupling, 
                                                   double cohesion, int methodCount) {
            // Simplified maintainability index
            double mi = 171 - 5.2 * Math.log(complexity) - 0.23 * coupling + 16.2 * Math.log(methodCount + 1);
            mi *= cohesion; // Factor in cohesion
            return Math.max(0, Math.min(100, mi));
        }
        
        private boolean isPrimitiveType(String type) {
            return Set.of("int", "long", "double", "float", "boolean", "char", "byte", "short", "void")
                .contains(type);
        }
    }
    
    class ApiDocGenerator {
        void generateHtmlDocs(ProjectRepresentation project, String outputFile) {
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n<html>\n<head>\n");
            html.append("<title>").append(project.getName()).append(" API Documentation</title>\n");
            html.append("<style>\n");
            html.append("body { font-family: Arial, sans-serif; margin: 20px; }\n");
            html.append(".package { margin: 20px 0; padding: 10px; background: #f0f0f0; }\n");
            html.append(".class { margin: 10px 0; padding: 10px; background: white; border: 1px solid #ddd; }\n");
            html.append(".method { margin: 5px 0; padding: 5px; background: #f9f9f9; }\n");
            html.append(".interface { border-left: 4px solid #4CAF50; }\n");
            html.append(".abstract { border-left: 4px solid #FF9800; }\n");
            html.append("</style>\n</head>\n<body>\n");
            
            html.append("<h1>").append(project.getName()).append(" API Documentation</h1>\n");
            
            Map<String, List<ClassRepresentation>> packageGroups = project.getClasses().stream()
                .collect(Collectors.groupingBy(ClassRepresentation::getPackageName));
            
            packageGroups.forEach((pkg, classes) -> {
                html.append("<div class='package'>\n");
                html.append("<h2>Package: ").append(pkg).append("</h2>\n");
                
                classes.forEach(cls -> {
                    String classType = cls.isInterface() ? "interface" : 
                                     cls.isAbstract() ? "abstract" : "class";
                    html.append("<div class='class ").append(classType).append("'>\n");
                    html.append("<h3>").append(cls.getName()).append("</h3>\n");
                    
                    // Public methods
                    List<MethodRepresentation> publicMethods = cls.getMethods().stream()
                        .filter(m -> "public".equals(m.getVisibility()))
                        .collect(Collectors.toList());
                    
                    if (!publicMethods.isEmpty()) {
                        html.append("<h4>Public Methods:</h4>\n");
                        publicMethods.forEach(method -> {
                            html.append("<div class='method'>");
                            html.append(method.getReturnType()).append(" ");
                            html.append("<strong>").append(method.getName()).append("</strong>(");
                            html.append(method.getParameters().stream()
                                .map(p -> p.getType() + " " + p.getName())
                                .collect(Collectors.joining(", ")));
                            html.append(")</div>\n");
                        });
                    }
                    
                    html.append("</div>\n");
                });
                
                html.append("</div>\n");
            });
            
            html.append("</body>\n</html>");
            
            try (FileWriter writer = new FileWriter(outputFile)) {
                writer.write(html.toString());
            } catch (IOException e) {
                System.err.println("Error writing HTML file: " + e.getMessage());
            }
        }
    }
    
    class RefactoringAnalyzer {
        List<RefactoringSuggestion> analyzePotentialRefactorings(ProjectRepresentation project) {
            List<RefactoringSuggestion> suggestions = new ArrayList<>();
            
            for (ClassRepresentation cls : project.getClasses()) {
                // Large class
                if (cls.getMethods().size() > 20) {
                    suggestions.add(new RefactoringSuggestion(
                        "Large Class",
                        cls.getName(),
                        "Class has " + cls.getMethods().size() + " methods",
                        "Consider breaking this class into smaller, more focused classes"
                    ));
                }
                
                // God class (too many responsibilities)
                if (cls.getFields().size() > 15 && cls.getMethods().size() > 15) {
                    suggestions.add(new RefactoringSuggestion(
                        "God Class",
                        cls.getName(),
                        "Class has too many fields (" + cls.getFields().size() + 
                        ") and methods (" + cls.getMethods().size() + ")",
                        "Apply Single Responsibility Principle - split into multiple classes"
                    ));
                }
                
                // Long parameter lists
                for (MethodRepresentation method : cls.getMethods()) {
                    if (method.getParameters().size() > 5) {
                        suggestions.add(new RefactoringSuggestion(
                            "Long Parameter List",
                            cls.getName() + "." + method.getName(),
                            "Method has " + method.getParameters().size() + " parameters",
                            "Consider using a parameter object or builder pattern"
                        ));
                    }
                }
                
                // Empty catch blocks (would need method body analysis)
                // Duplicate code detection (would need method body analysis)
            }
            
            return suggestions;
        }
    }
    
    class SecurityAuditor {
        List<SecurityIssue> auditProject(ProjectRepresentation project) {
            List<SecurityIssue> issues = new ArrayList<>();
            
            for (ClassRepresentation cls : project.getClasses()) {
                // Check for SQL injection vulnerabilities
                for (MethodRepresentation method : cls.getMethods()) {
                    if (method.getName().toLowerCase().contains("sql") || 
                        method.getName().toLowerCase().contains("query")) {
                        if (method.getParameters().stream()
                            .anyMatch(p -> p.getType().equals("String"))) {
                            issues.add(new SecurityIssue(
                                "MEDIUM",
                                "Potential SQL Injection",
                                cls.getName() + "." + method.getName(),
                                "Method accepts String parameters and appears to handle SQL",
                                "Use parameterized queries or prepared statements"
                            ));
                        }
                    }
                }
                
                // Check for hardcoded secrets
                for (FieldRepresentation field : cls.getFields()) {
                    String fieldName = field.getName().toLowerCase();
                    if (fieldName.contains("password") || fieldName.contains("secret") || 
                        fieldName.contains("key") || fieldName.contains("token")) {
                        if (field.isStatic() && field.isFinal()) {
                            issues.add(new SecurityIssue(
                                "HIGH",
                                "Hardcoded Secret",
                                cls.getName() + "." + field.getName(),
                                "Potential hardcoded sensitive information",
                                "Use environment variables or secure configuration management"
                            ));
                        }
                    }
                }
            }
            
            return issues;
        }
    }
    
    class TestAnalyzer {
        TestCoverageReport analyzeTestCoverage(ProjectRepresentation project) {
            List<ClassRepresentation> testClasses = project.getClasses().stream()
                .filter(this::isTestClass)
                .collect(Collectors.toList());
            
            List<ClassRepresentation> productionClasses = project.getClasses().stream()
                .filter(cls -> !isTestClass(cls))
                .collect(Collectors.toList());
            
            Set<String> classesWithTests = new HashSet<>();
            Map<String, Integer> testMethodsByAnnotation = new HashMap<>();
            
            for (ClassRepresentation testClass : testClasses) {
                // Extract tested class name (assuming naming convention)
                String testedClassName = testClass.getName()
                    .replace("Test", "")
                    .replace("Tests", "");
                classesWithTests.add(testedClassName);
                
                // Count test methods by annotation
                for (MethodRepresentation method : testClass.getMethods()) {
                    for (String annotation : method.getAnnotations()) {
                        if (annotation.toLowerCase().contains("test")) {
                            testMethodsByAnnotation.merge(annotation, 1, Integer::sum);
                        }
                    }
                }
            }
            
            List<String> classesWithoutTests = productionClasses.stream()
                .map(ClassRepresentation::getName)
                .filter(name -> !classesWithTests.contains(name))
                .filter(name -> !name.contains("Exception") && !name.contains("Config"))
                .collect(Collectors.toList());
            
            return new TestCoverageReport(
                productionClasses.size(),
                testClasses.size(),
                classesWithTests.size(),
                classesWithoutTests,
                testMethodsByAnnotation
            );
        }
        
        private boolean isTestClass(ClassRepresentation cls) {
            return cls.getName().endsWith("Test") || 
                   cls.getName().endsWith("Tests") ||
                   cls.getFilePath().contains("/test/");
        }
    }
    
    class DiagramGenerator {
        void generatePlantUML(ProjectRepresentation project, String outputFile) {
            StringBuilder uml = new StringBuilder();
            uml.append("@startuml\n");
            uml.append("skinparam classAttributeIconSize 0\n");
            uml.append("skinparam class {\n");
            uml.append("  BackgroundColor<<interface>> LightGreen\n");
            uml.append("  BackgroundColor<<abstract>> LightBlue\n");
            uml.append("}\n\n");
            
            // Group by package
            Map<String, List<ClassRepresentation>> packageGroups = project.getClasses().stream()
                .collect(Collectors.groupingBy(ClassRepresentation::getPackageName));
            
            packageGroups.forEach((pkg, classes) -> {
                uml.append("package \"").append(pkg).append("\" {\n");
                
                for (ClassRepresentation cls : classes) {
                    // Class declaration
                    if (cls.isInterface()) {
                        uml.append("  interface ");
                    } else if (cls.isAbstract()) {
                        uml.append("  abstract class ");
                    } else if (cls.isEnum()) {
                        uml.append("  enum ");
                    } else {
                        uml.append("  class ");
                    }
                    
                    uml.append(cls.getName());
                    
                    if (cls.isInterface()) {
                        uml.append(" <<interface>>");
                    } else if (cls.isAbstract()) {
                        uml.append(" <<abstract>>");
                    }
                    
                    uml.append(" {\n");
                    
                    // Fields
                    for (FieldRepresentation field : cls.getFields()) {
                        uml.append("    ").append(getVisibilitySymbol(field.getVisibility()));
                        if (field.isStatic()) uml.append("{static} ");
                        uml.append(field.getType()).append(" ").append(field.getName()).append("\n");
                    }
                    
                    // Methods
                    for (MethodRepresentation method : cls.getMethods()) {
                        uml.append("    ").append(getVisibilitySymbol(method.getVisibility()));
                        if (method.isStatic()) uml.append("{static} ");
                        if (method.isAbstract()) uml.append("{abstract} ");
                        uml.append(method.getReturnType()).append(" ").append(method.getName());
                        uml.append("(");
                        uml.append(method.getParameters().stream()
                            .map(p -> p.getType() + " " + p.getName())
                            .collect(Collectors.joining(", ")));
                        uml.append(")\n");
                    }
                    
                    uml.append("  }\n");
                }
                
                uml.append("}\n\n");
            });
            
            // Relationships
            for (ClassRepresentation cls : project.getClasses()) {
                // Inheritance
                for (String extended : cls.getExtendedTypes()) {
                    uml.append(cls.getName()).append(" --|> ").append(extended).append("\n");
                }
                
                // Implementation
                for (String implemented : cls.getImplementedInterfaces()) {
                    uml.append(cls.getName()).append(" ..|> ").append(implemented).append("\n");
                }
            }
            
            uml.append("@enduml\n");
            
            try (FileWriter writer = new FileWriter(outputFile)) {
                writer.write(uml.toString());
            } catch (IOException e) {
                System.err.println("Error writing PlantUML file: " + e.getMessage());
            }
        }
        
        private String getVisibilitySymbol(String visibility) {
            return switch (visibility) {
                case "public" -> "+";
                case "private" -> "-";
                case "protected" -> "#";
                default -> "~";
            };
        }
    }
    
    class CodeLinter {
        List<LintIssue> lintProject(ProjectRepresentation project) {
            List<LintIssue> issues = new ArrayList<>();
            
            for (ClassRepresentation cls : project.getClasses()) {
                // Naming conventions
                if (!cls.getName().matches("[A-Z][a-zA-Z0-9]*")) {
                    issues.add(new LintIssue(
                        "Naming Convention",
                        cls.getName(),
                        "Class name should start with uppercase and use CamelCase"
                    ));
                }
                
                // Field naming
                for (FieldRepresentation field : cls.getFields()) {
                    if (!field.getName().matches("[a-z][a-zA-Z0-9]*")) {
                        issues.add(new LintIssue(
                            "Naming Convention",
                            cls.getName() + "." + field.getName(),
                            "Field name should start with lowercase and use camelCase"
                        ));
                    }
                    
                    // Public fields warning
                    if ("public".equals(field.getVisibility()) && !field.isStatic() && !field.isFinal()) {
                        issues.add(new LintIssue(
                            "Encapsulation",
                            cls.getName() + "." + field.getName(),
                            "Public fields should be avoided; use getters/setters"
                        ));
                    }
                }
                
                // Method naming
                for (MethodRepresentation method : cls.getMethods()) {
                    if (!method.getName().matches("[a-z][a-zA-Z0-9]*")) {
                        issues.add(new LintIssue(
                            "Naming Convention",
                            cls.getName() + "." + method.getName(),
                            "Method name should start with lowercase and use camelCase"
                        ));
                    }
                }
            }
            
            return issues;
        }
    }
    
    // Data classes for analysis results
    record ClassMetrics(String className, int methodCount, int fieldCount, 
                       double complexity, int coupling, double cohesion, 
                       double maintainabilityIndex) {}
    
    record RefactoringSuggestion(String type, String target, String reason, String suggestion) {}
    
    record SecurityIssue(String severity, String type, String location, 
                        String description, String recommendation) {}
    
    record TestCoverageReport(int totalClasses, int testClasses, int classesWithTests,
                             List<String> classesWithoutTests, 
                             Map<String, Integer> testMethodsByAnnotation) {}
    
    record LintIssue(String type, String location, String message) {}
}

