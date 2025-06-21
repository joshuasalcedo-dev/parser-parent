package io.joshuasalcedo.parser.java.parser;


import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import io.joshuasalcedo.parser.java.model.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core parser for Java projects using JavaParser library.
 * This class is responsible for parsing Java source files and creating
 * the AST representation models.
 */
public class JavaProjectParser {
    
    private final Map<String, ClassRepresentation> classMap = new HashMap<>();
    private final JavaParser javaParser;
    
    public JavaProjectParser() {
        ParserConfiguration config = new ParserConfiguration();
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        this.javaParser = new JavaParser(config);
    }
    
    /**
     * Parse a Java project from the given root path
     * @param rootPath Path to the project root directory
     * @return ProjectRepresentation containing all parsed classes
     */
    public ProjectRepresentation parseProject(String rootPath) {
        File rootDir = new File(rootPath);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            throw new IllegalArgumentException("Invalid project root directory: " + rootPath);
        }
        
        ProjectRepresentation project = new ProjectRepresentation(rootDir.getName(), rootPath);
        
        // Parse all Java files in src/main/java and src/test/java (Maven structure)
        List<String> sourcePaths = Arrays.asList(
            "src/main/java",
            "src/test/java",
            "src",  // Also check root src for non-Maven projects
            "."     // And current directory
        );
        
        for (String sourcePath : sourcePaths) {
            Path sourceDir = Paths.get(rootPath, sourcePath).normalize();
            if (Files.exists(sourceDir) && Files.isDirectory(sourceDir)) {
                parseDirectory(sourceDir, project);
            }
        }
        
        // If still no classes found, try to parse all .java files in the entire project
        if (project.getClasses().isEmpty()) {
            parseDirectory(rootDir.toPath(), project);
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
        } else if (type.getClass().getSimpleName().equals("RecordDeclaration")) {
            // Handle records - JavaParser may have RecordDeclaration but we avoid hard dependency
            classRep.setRecord(true);
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
        if (declaration instanceof NodeWithModifiers) {
            NodeWithModifiers<?> nodeWithModifiers = (NodeWithModifiers<?>) declaration;
            if (nodeWithModifiers.hasModifier(Modifier.Keyword.PUBLIC)) return "public";
            if (nodeWithModifiers.hasModifier(Modifier.Keyword.PROTECTED)) return "protected";
            if (nodeWithModifiers.hasModifier(Modifier.Keyword.PRIVATE)) return "private";
        }
        return "package-private";
    }
    
    /**
     * Calculate statistics for the project
     */
    public ProjectStatistics calculateStatistics(ProjectRepresentation project) {
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
            
            // Calculate dependencies
            int dependencies = clazz.getExtendedTypes().size() + clazz.getImplementedInterfaces().size();
            
            // Create complexity entry
            classComplexities.add(new ClassComplexity(
                clazz.getFullyQualifiedName(),
                clazz.getMethods().size(),
                clazz.getFields().size(),
                dependencies
            ));
            
            // Estimate lines of code
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
            classComplexities.stream().limit(10).collect(Collectors.toList())
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
    
    /**
     * Get the parsed class map
     */
    public Map<String, ClassRepresentation> getClassMap() {
        return classMap;
    }
}