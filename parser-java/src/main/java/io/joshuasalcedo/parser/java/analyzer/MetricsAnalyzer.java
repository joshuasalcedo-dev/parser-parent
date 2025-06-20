package io.joshuasalcedo.parser.java.analyzer;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.StaticJavaParser;
import io.joshuasalcedo.parser.java.model.*;

import java.io.File;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Advanced metrics analyzer that calculates code metrics, detects duplicates,
 * and analyzes usage patterns
 */
public class MetricsAnalyzer extends AbstractAnalyzer<MetricsResult> {

    private final Map<String, CompilationUnit> compilationUnits = new HashMap<>();
    private final Map<String, Integer> methodCallCounts = new ConcurrentHashMap<>();
    private final Map<String, Integer> classUsageCounts = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> methodCallers = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> classReferences = new ConcurrentHashMap<>();
    private final Map<String, String> methodBodies = new HashMap<>();
    private final Map<String, Integer> methodComplexity = new HashMap<>();
    private final Map<String, Integer> methodLineCount = new HashMap<>();

    public MetricsAnalyzer(ProjectRepresentation project) {
        super(project);
        parseCompilationUnits();
    }

    private void parseCompilationUnits() {
        for (ClassRepresentation cls : project.getClasses()) {
            try {
                File file = new File(cls.getFilePath());
                if (file.exists()) {
                    CompilationUnit cu = StaticJavaParser.parse(file);
                    compilationUnits.put(cls.getFullyQualifiedName(), cu);
                }
            } catch (Exception e) {
                System.err.println("Failed to parse: " + cls.getFilePath());
            }
        }
    }

    @Override
    public MetricsResult analyze() {
        // First pass: collect method bodies and basic metrics
        collectMethodInformation();

        // Second pass: analyze usage patterns
        analyzeUsagePatterns();

        // Calculate various metrics
        List<ClassMetrics> classMetrics = calculateClassMetrics();
        List<CodeDuplication> duplicates = detectDuplicateCode();
        List<MethodUsage> methodUsages = analyzeMethodUsage();
        Map<String, Integer> mostUsedClasses = getMostUsedClasses();
        Set<String> unusedMethods = detectUnusedMethods();
        Set<String> unusedClasses = detectUnusedClasses();
        Map<String, Double> classComplexity = calculateClassComplexity();
        Map<String, Integer> linesOfCode = calculateLinesOfCode();

        // Calculate project-level metrics
        ProjectMetrics projectMetrics = calculateProjectMetrics(
                classMetrics, duplicates, methodUsages, unusedMethods, unusedClasses
        );

        return new MetricsResult(
                classMetrics,
                duplicates,
                methodUsages,
                mostUsedClasses,
                unusedMethods,
                unusedClasses,
                classComplexity,
                linesOfCode,
                projectMetrics
        );
    }

    private void collectMethodInformation() {
        for (Map.Entry<String, CompilationUnit> entry : compilationUnits.entrySet()) {
            String className = entry.getKey();
            CompilationUnit cu = entry.getValue();

            cu.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(MethodDeclaration method, Void arg) {
                    String methodId = className + "." + method.getNameAsString() + getMethodSignature(method);

                    // Store method body for duplicate detection
                    method.getBody().ifPresent(body -> {
                        methodBodies.put(methodId, normalizeMethodBody(body.toString()));
                    });

                    // Calculate cyclomatic complexity
                    int complexity = calculateCyclomaticComplexity(method);
                    methodComplexity.put(methodId, complexity);

                    // Count lines of code
                    int lines = countMethodLines(method);
                    methodLineCount.put(methodId, lines);

                    super.visit(method, arg);
                }

                @Override
                public void visit(ConstructorDeclaration constructor, Void arg) {
                    String constructorId = className + ".<init>" + getConstructorSignature(constructor);

                    // Calculate complexity for constructors too
                    int complexity = calculateCyclomaticComplexity(constructor);
                    methodComplexity.put(constructorId, complexity);

                    int lines = countConstructorLines(constructor);
                    methodLineCount.put(constructorId, lines);

                    super.visit(constructor, arg);
                }
            }, null);
        }
    }

    private void analyzeUsagePatterns() {
        for (Map.Entry<String, CompilationUnit> entry : compilationUnits.entrySet()) {
            String currentClass = entry.getKey();
            CompilationUnit cu = entry.getValue();

            cu.accept(new UsageAnalysisVisitor(currentClass), null);
        }
    }

    private class UsageAnalysisVisitor extends VoidVisitorAdapter<Void> {
        private final String currentClass;

        public UsageAnalysisVisitor(String currentClass) {
            this.currentClass = currentClass;
        }

        @Override
        public void visit(MethodCallExpr methodCall, Void arg) {
            String methodName = methodCall.getNameAsString();

            // Try to resolve the method call
            if (methodCall.getScope().isPresent()) {
                Expression scope = methodCall.getScope().get();
                String targetType = resolveType(scope);
                if (targetType != null) {
                    String methodId = targetType + "." + methodName;

                    // Increment call count
                    methodCallCounts.merge(methodId, 1, Integer::sum);

                    // Track who calls this method
                    methodCallers.computeIfAbsent(methodId, k -> new HashSet<>()).add(currentClass);

                    // Track class usage
                    classUsageCounts.merge(targetType, 1, Integer::sum);
                    classReferences.computeIfAbsent(targetType, k -> new HashSet<>()).add(currentClass);
                }
            }

            super.visit(methodCall, arg);
        }

        @Override
        public void visit(ObjectCreationExpr objectCreation, Void arg) {
            String className = objectCreation.getType().asString();
            String resolvedClass = resolveClassName(className);

            if (resolvedClass != null) {
                classUsageCounts.merge(resolvedClass, 1, Integer::sum);
                classReferences.computeIfAbsent(resolvedClass, k -> new HashSet<>()).add(currentClass);

                // Constructor call
                String constructorId = resolvedClass + ".<init>";
                methodCallCounts.merge(constructorId, 1, Integer::sum);
                methodCallers.computeIfAbsent(constructorId, k -> new HashSet<>()).add(currentClass);
            }

            super.visit(objectCreation, arg);
        }

        @Override
        public void visit(FieldAccessExpr fieldAccess, Void arg) {
            if (fieldAccess.getScope() != null) {
                Expression scope = fieldAccess.getScope();
                String targetType = resolveType(scope);
                if (targetType != null) {
                    classUsageCounts.merge(targetType, 1, Integer::sum);
                    classReferences.computeIfAbsent(targetType, k -> new HashSet<>()).add(currentClass);
                }
            }

            super.visit(fieldAccess, arg);
        }

        private String resolveType(Expression expr) {
            // Simplified type resolution - in real implementation, use JavaSymbolSolver
            if (expr instanceof NameExpr) {
                String name = ((NameExpr) expr).getNameAsString();
                // Check if it's a known class
                return project.getClasses().stream()
                        .map(ClassRepresentation::getName)
                        .filter(className -> className.equals(name))
                        .findFirst()
                        .orElse(null);
            } else if (expr instanceof ThisExpr) {
                return currentClass;
            }
            // Add more resolution logic as needed
            return null;
        }

        private String resolveClassName(String simpleName) {
            // Try to find the full class name
            return project.getClasses().stream()
                    .map(ClassRepresentation::getFullyQualifiedName)
                    .filter(fqn -> fqn.endsWith("." + simpleName) || fqn.equals(simpleName))
                    .findFirst()
                    .orElse(null);
        }
    }

    private List<ClassMetrics> calculateClassMetrics() {
        return project.getClasses().stream()
                .map(this::calculateMetricsForClass)
                .collect(Collectors.toList());
    }

    private ClassMetrics calculateMetricsForClass(ClassRepresentation cls) {
        String className = cls.getFullyQualifiedName();

        // Basic counts
        int methodCount = cls.getMethods().size();
        int fieldCount = cls.getFields().size();

        // Complexity metrics
        double totalComplexity = cls.getMethods().stream()
                .mapToInt(m -> methodComplexity.getOrDefault(
                        className + "." + m.getName() + getMethodSignature(m), 1))
                .sum();

        // Coupling metrics
        int afferentCoupling = classReferences.getOrDefault(className, Set.of()).size();
        int efferentCoupling = countEfferentCoupling(cls);

        // Cohesion metric (simplified LCOM)
        double cohesion = calculateLackOfCohesion(cls);

        // Lines of code
        int loc = methodLineCount.entrySet().stream()
                .filter(e -> e.getKey().startsWith(className + "."))
                .mapToInt(Map.Entry::getValue)
                .sum();

        // Usage metrics
        int usageCount = classUsageCounts.getOrDefault(className, 0);

        // Calculate maintainability index
        double maintainability = calculateMaintainabilityIndex(
                totalComplexity, loc, methodCount
        );

        return new ClassMetrics(
                className,
                methodCount,
                fieldCount,
                totalComplexity,
                afferentCoupling,
                efferentCoupling,
                cohesion,
                maintainability,
                loc,
                usageCount
        );
    }

    private List<CodeDuplication> detectDuplicateCode() {
        List<CodeDuplication> duplicates = new ArrayList<>();
        Map<String, List<String>> hashToMethods = new HashMap<>();

        // Group methods by their normalized body hash
        for (Map.Entry<String, String> entry : methodBodies.entrySet()) {
            String methodId = entry.getKey();
            String body = entry.getValue();

            if (body.length() > 50) { // Only consider methods with substantial code
                String hash = hashMethodBody(body);
                hashToMethods.computeIfAbsent(hash, k -> new ArrayList<>()).add(methodId);
            }
        }

        // Find duplicates
        for (Map.Entry<String, List<String>> entry : hashToMethods.entrySet()) {
            List<String> methods = entry.getValue();
            if (methods.size() > 1) {
                String sampleBody = methodBodies.get(methods.get(0));
                int lineCount = sampleBody.split("\n").length;

                duplicates.add(new CodeDuplication(
                        methods,
                        lineCount,
                        calculateDuplicationImpact(methods, lineCount)
                ));
            }
        }

        // Also detect similar code (not exact duplicates)
        List<CodeDuplication> similarCode = detectSimilarCode();
        duplicates.addAll(similarCode);

        return duplicates;
    }

    private List<CodeDuplication> detectSimilarCode() {
        List<CodeDuplication> similar = new ArrayList<>();
        List<Map.Entry<String, String>> methodList = new ArrayList<>(methodBodies.entrySet());

        for (int i = 0; i < methodList.size(); i++) {
            for (int j = i + 1; j < methodList.size(); j++) {
                String method1 = methodList.get(i).getKey();
                String method2 = methodList.get(j).getKey();
                String body1 = methodList.get(i).getValue();
                String body2 = methodList.get(j).getValue();

                double similarity = calculateSimilarity(body1, body2);

                if (similarity > 0.8 && similarity < 1.0) { // 80% similar but not identical
                    int avgLines = (body1.split("\n").length + body2.split("\n").length) / 2;

                    similar.add(new CodeDuplication(
                            List.of(method1, method2),
                            avgLines,
                            similarity,
                            true // flag as similar, not exact
                    ));
                }
            }
        }

        return similar;
    }

    private List<MethodUsage> analyzeMethodUsage() {
        return methodCallCounts.entrySet().stream()
                .map(entry -> {
                    String methodId = entry.getKey();
                    int callCount = entry.getValue();
                    Set<String> callers = methodCallers.getOrDefault(methodId, Set.of());

                    return new MethodUsage(
                            methodId,
                            callCount,
                            callers,
                            classifyUsageLevel(callCount)
                    );
                })
                .sorted((a, b) -> Integer.compare(b.callCount(), a.callCount()))
                .collect(Collectors.toList());
    }

    private Map<String, Integer> getMostUsedClasses() {
        return classUsageCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(20)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    private Set<String> detectUnusedMethods() {
        Set<String> allMethods = new HashSet<>();

        // Collect all methods
        for (ClassRepresentation cls : project.getClasses()) {
            String className = cls.getFullyQualifiedName();

            for (MethodRepresentation method : cls.getMethods()) {
                String methodId = className + "." + method.getName();
                allMethods.add(methodId);
            }
        }

        // Find methods that are never called
        Set<String> unusedMethods = allMethods.stream()
                .filter(method -> !methodCallCounts.containsKey(method) ||
                        methodCallCounts.get(method) == 0)
                .filter(this::isNotEntryPoint)
                .collect(Collectors.toSet());

        return unusedMethods;
    }

    private Set<String> detectUnusedClasses() {
        return project.getClasses().stream()
                .map(ClassRepresentation::getFullyQualifiedName)
                .filter(className -> !classUsageCounts.containsKey(className) ||
                        classUsageCounts.get(className) == 0)
                .filter(this::isNotEntryPointClass)
                .collect(Collectors.toSet());
    }

    private Map<String, Double> calculateClassComplexity() {
        Map<String, Double> complexity = new HashMap<>();

        for (ClassRepresentation cls : project.getClasses()) {
            String className = cls.getFullyQualifiedName();

            double totalComplexity = cls.getMethods().stream()
                    .mapToDouble(m -> methodComplexity.getOrDefault(
                            className + "." + m.getName() + getMethodSignature(m), 1))
                    .sum();

            // Weight by number of methods
            double avgComplexity = cls.getMethods().isEmpty() ? 0 :
                    totalComplexity / cls.getMethods().size();

            complexity.put(className, avgComplexity);
        }

        return complexity;
    }

    private Map<String, Integer> calculateLinesOfCode() {
        Map<String, Integer> loc = new HashMap<>();

        for (Map.Entry<String, CompilationUnit> entry : compilationUnits.entrySet()) {
            String className = entry.getKey();
            CompilationUnit cu = entry.getValue();

            int lines = cu.getRange()
                    .map(range -> range.end.line - range.begin.line + 1)
                    .orElse(0);

            loc.put(className, lines);
        }

        return loc;
    }

    // Helper methods

    private String getMethodSignature(MethodDeclaration method) {
        return "(" + method.getParameters().stream()
                .map(p -> p.getType().asString())
                .collect(Collectors.joining(",")) + ")";
    }

    private String getMethodSignature(MethodRepresentation method) {
        return "(" + method.getParameters().stream()
                .map(ParameterRepresentation::getType)
                .collect(Collectors.joining(",")) + ")";
    }

    private String getConstructorSignature(ConstructorDeclaration constructor) {
        return "(" + constructor.getParameters().stream()
                .map(p -> p.getType().asString())
                .collect(Collectors.joining(",")) + ")";
    }

    private String normalizeMethodBody(String body) {
        return body.replaceAll("\\s+", " ")
                .replaceAll("//.*", "")
                .replaceAll("/\\*.*?\\*/", "")
                .trim();
    }

    private String hashMethodBody(String body) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(body.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return body.hashCode() + "";
        }
    }

    private int calculateCyclomaticComplexity(MethodDeclaration method) {
        final int[] complexity = {1}; // Base complexity - use array to make it effectively final

        method.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(IfStmt n, Void arg) {
                complexity[0]++;
                super.visit(n, arg);
            }

            @Override
            public void visit(WhileStmt n, Void arg) {
                complexity[0]++;
                super.visit(n, arg);
            }

            @Override
            public void visit(ForStmt n, Void arg) {
                complexity[0]++;
                super.visit(n, arg);
            }

            @Override
            public void visit(ForEachStmt n, Void arg) {
                complexity[0]++;
                super.visit(n, arg);
            }

            @Override
            public void visit(SwitchEntry n, Void arg) {
                if (!n.getLabels().isEmpty()) {
                    complexity[0]++;
                }
                super.visit(n, arg);
            }

            @Override
            public void visit(CatchClause n, Void arg) {
                complexity[0]++;
                super.visit(n, arg);
            }

            @Override
            public void visit(ConditionalExpr n, Void arg) {
                complexity[0]++;
                super.visit(n, arg);
            }
        }, null);

        return complexity[0];
    }

    private int calculateCyclomaticComplexity(ConstructorDeclaration constructor) {
        final int[] complexity = {1}; // Base complexity

        constructor.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(IfStmt n, Void arg) {
                complexity[0]++;
                super.visit(n, arg);
            }

            @Override
            public void visit(WhileStmt n, Void arg) {
                complexity[0]++;
                super.visit(n, arg);
            }

            @Override
            public void visit(ForStmt n, Void arg) {
                complexity[0]++;
                super.visit(n, arg);
            }

            @Override
            public void visit(ForEachStmt n, Void arg) {
                complexity[0]++;
                super.visit(n, arg);
            }

            @Override
            public void visit(SwitchEntry n, Void arg) {
                if (!n.getLabels().isEmpty()) {
                    complexity[0]++;
                }
                super.visit(n, arg);
            }

            @Override
            public void visit(CatchClause n, Void arg) {
                complexity[0]++;
                super.visit(n, arg);
            }

            @Override
            public void visit(ConditionalExpr n, Void arg) {
                complexity[0]++;
                super.visit(n, arg);
            }
        }, null);

        return complexity[0];
    }

    private int countMethodLines(MethodDeclaration method) {
        return method.getRange()
                .map(range -> range.end.line - range.begin.line + 1)
                .orElse(0);
    }

    private int countConstructorLines(ConstructorDeclaration constructor) {
        return constructor.getRange()
                .map(range -> range.end.line - range.begin.line + 1)
                .orElse(0);
    }

    private int countEfferentCoupling(ClassRepresentation cls) {
        Set<String> dependencies = new HashSet<>();

        // Field types
        cls.getFields().forEach(field -> {
            String type = extractClassName(field.getType());
            if (isProjectClass(type)) {
                dependencies.add(type);
            }
        });

        // Method return types and parameters
        cls.getMethods().forEach(method -> {
            String returnType = extractClassName(method.getReturnType());
            if (isProjectClass(returnType)) {
                dependencies.add(returnType);
            }

            method.getParameters().forEach(param -> {
                String paramType = extractClassName(param.getType());
                if (isProjectClass(paramType)) {
                    dependencies.add(paramType);
                }
            });
        });

        return dependencies.size();
    }

    private double calculateLackOfCohesion(ClassRepresentation cls) {
        if (cls.getMethods().size() <= 1 || cls.getFields().isEmpty()) {
            return 0.0; // Perfect cohesion
        }

        // Simplified LCOM calculation
        int methodPairs = 0;
        int disjointPairs = 0;

        List<MethodRepresentation> methods = new ArrayList<>(cls.getMethods());
        for (int i = 0; i < methods.size(); i++) {
            for (int j = i + 1; j < methods.size(); j++) {
                methodPairs++;
                // In a real implementation, check if methods access common fields
                // For now, use a simplified metric
            }
        }

        return methodPairs == 0 ? 0.0 : (double) disjointPairs / methodPairs;
    }

    private double calculateMaintainabilityIndex(double complexity, int loc, int methodCount) {
        if (loc == 0) return 100.0;

        // Simplified maintainability index calculation
        double mi = 171 - 5.2 * Math.log(complexity) - 0.23 * complexity - 16.2 * Math.log(loc);

        // Factor in method count
        if (methodCount > 20) {
            mi -= (methodCount - 20) * 0.5;
        }

        return Math.max(0, Math.min(100, mi));
    }

    private double calculateDuplicationImpact(List<String> methods, int lineCount) {
        // Impact = number of duplicates * lines of code * complexity factor
        double avgComplexity = methods.stream()
                .mapToInt(m -> methodComplexity.getOrDefault(m, 1))
                .average()
                .orElse(1.0);

        return methods.size() * lineCount * (1 + avgComplexity / 10);
    }

    private double calculateSimilarity(String code1, String code2) {
        // Levenshtein distance-based similarity
        int distance = levenshteinDistance(code1, code2);
        int maxLength = Math.max(code1.length(), code2.length());

        return maxLength == 0 ? 1.0 : 1.0 - (double) distance / maxLength;
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        dp[i - 1][j] + 1,
                        Math.min(dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
                );
            }
        }

        return dp[s1.length()][s2.length()];
    }

    private String classifyUsageLevel(int callCount) {
        if (callCount == 0) return "Unused";
        if (callCount == 1) return "Rarely Used";
        if (callCount < 5) return "Occasionally Used";
        if (callCount < 20) return "Frequently Used";
        return "Heavily Used";
    }

    private boolean isNotEntryPoint(String methodId) {
        // Check if method is an entry point
        return !methodId.endsWith(".main") &&
                !methodId.contains("Test") &&
                !methodId.contains("@PostConstruct") &&
                !methodId.contains("@EventListener");
    }

    private boolean isNotEntryPointClass(String className) {
        ClassRepresentation cls = project.getClasses().stream()
                .filter(c -> c.getFullyQualifiedName().equals(className))
                .findFirst()
                .orElse(null);

        if (cls == null) return true;

        // Check for main method
        boolean hasMain = cls.getMethods().stream()
                .anyMatch(m -> "main".equals(m.getName()) && m.isStatic());

        // Check for test class
        boolean isTest = className.endsWith("Test") || className.endsWith("Tests");

        // Check for Spring annotations
        boolean isSpringBean = cls.getAnnotations().stream()
                .anyMatch(a -> Set.of("Component", "Service", "Repository",
                                "Controller", "RestController", "Configuration")
                        .contains(a));

        return !hasMain && !isTest && !isSpringBean;
    }

    private String extractClassName(String type) {
        // Remove generics and array notation
        return type.replaceAll("<.*>", "")
                .replaceAll("\\[\\]", "")
                .trim();
    }

    private boolean isProjectClass(String className) {
        return project.getClasses().stream()
                .anyMatch(c -> c.getFullyQualifiedName().equals(className) ||
                        c.getName().equals(className));
    }

    private ProjectMetrics calculateProjectMetrics(
            List<ClassMetrics> classMetrics,
            List<CodeDuplication> duplicates,
            List<MethodUsage> methodUsages,
            Set<String> unusedMethods,
            Set<String> unusedClasses) {

        int totalClasses = project.getClasses().size();
        int totalMethods = classMetrics.stream().mapToInt(ClassMetrics::methodCount).sum();
        int totalLOC = classMetrics.stream().mapToInt(ClassMetrics::linesOfCode).sum();

        double avgComplexity = classMetrics.stream()
                .mapToDouble(ClassMetrics::complexity)
                .average()
                .orElse(0.0);

        double avgMaintainability = classMetrics.stream()
                .mapToDouble(ClassMetrics::maintainabilityIndex)
                .average()
                .orElse(0.0);

        int duplicatedLines = duplicates.stream()
                .mapToInt(d -> d.lineCount() * (d.methods().size() - 1))
                .sum();

        double duplicationRatio = totalLOC > 0 ? (double) duplicatedLines / totalLOC : 0.0;

        double codeReuse = methodUsages.stream()
                .filter(m -> m.callCount() > 1)
                .count() / (double) totalMethods;

        return new ProjectMetrics(
                totalClasses,
                totalMethods,
                totalLOC,
                avgComplexity,
                avgMaintainability,
                duplicationRatio,
                codeReuse,
                unusedMethods.size(),
                unusedClasses.size(),
                calculateTechnicalDebtScore(classMetrics, duplicates, unusedMethods, unusedClasses)
        );
    }

    private double calculateTechnicalDebtScore(
            List<ClassMetrics> classMetrics,
            List<CodeDuplication> duplicates,
            Set<String> unusedMethods,
            Set<String> unusedClasses) {

        double score = 0.0;

        // High complexity classes
        score += classMetrics.stream()
                .filter(c -> c.complexity() > 20)
                .count() * 10;

        // Low maintainability
        score += classMetrics.stream()
                .filter(c -> c.maintainabilityIndex() < 50)
                .count() * 5;

        // Code duplication
        score += duplicates.size() * 3;

        // Unused code
        score += unusedMethods.size() * 1;
        score += unusedClasses.size() * 2;

        return score;
    }

    @Override
    public String getAnalyzerName() {
        return "Advanced Metrics Analyzer";
    }

    @Override
    public void printResults(MetricsResult results) {
        System.out.println("\n=== Advanced Metrics Analysis Results ===");

        // Project overview
        ProjectMetrics pm = results.projectMetrics();
        System.out.println("\nProject Overview:");
        System.out.println("  Total Classes: " + pm.totalClasses());
        System.out.println("  Total Methods: " + pm.totalMethods());
        System.out.println("  Total Lines of Code: " + pm.totalLinesOfCode());
        System.out.println("  Average Complexity: " + String.format("%.2f", pm.averageComplexity()));
        System.out.println("  Average Maintainability: " + String.format("%.2f", pm.averageMaintainability()));
        System.out.println("  Code Duplication: " + String.format("%.1f%%", pm.duplicationRatio() * 100));
        System.out.println("  Code Reuse: " + String.format("%.1f%%", pm.codeReuse() * 100));
        System.out.println("  Technical Debt Score: " + String.format("%.0f", pm.technicalDebtScore()));

        // Most complex classes
        System.out.println("\nMost Complex Classes:");
        results.classMetrics().stream()
                .sorted((a, b) -> Double.compare(b.complexity(), a.complexity()))
                .limit(10)
                .forEach(c -> System.out.printf("  %s: %.1f complexity, %d methods%n",
                        c.className(), c.complexity(), c.methodCount()));

        // Most used classes
        System.out.println("\nMost Used Classes:");
        results.mostUsedClasses().entrySet().stream()
                .limit(10)
                .forEach(e -> System.out.printf("  %s: %d references%n", e.getKey(), e.getValue()));

        // Most used methods
        System.out.println("\nMost Used Methods:");
        results.methodUsages().stream()
                .limit(10)
                .forEach(m -> System.out.printf("  %s: %d calls from %d classes%n",
                        m.methodId(), m.callCount(), m.callers().size()));

        // Code duplicates
        if (!results.codeDuplicates().isEmpty()) {
            System.out.println("\n⚠️  Code Duplicates Found:");
            results.codeDuplicates().stream()
                    .sorted((a, b) -> Double.compare(b.impact(), a.impact()))
                    .limit(10)
                    .forEach(d -> {
                        System.out.printf("  %d duplicate methods with %d lines each (impact: %.1f)%n",
                                d.methods().size(), d.lineCount(), d.impact());
                        d.methods().forEach(m -> System.out.println("    - " + m));
                    });
        }

        // Unused code
        if (!results.unusedMethods().isEmpty()) {
            System.out.println("\n⚠️  Unused Methods: " + results.unusedMethods().size());
            results.unusedMethods().stream()
                    .limit(10)
                    .forEach(m -> System.out.println("  - " + m));
            if (results.unusedMethods().size() > 10) {
                System.out.println("  ... and " + (results.unusedMethods().size() - 10) + " more");
            }
        }

        if (!results.unusedClasses().isEmpty()) {
            System.out.println("\n⚠️  Unused Classes: " + results.unusedClasses().size());
            results.unusedClasses().forEach(c -> System.out.println("  - " + c));
        }
    }

    @Override
    public void exportResults(MetricsResult results, String outputPath) {
        // Export detailed metrics to JSON/CSV
        // Implementation would go here
    }
}