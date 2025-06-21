package io.joshuasalcedo.parser.spring.config;

import io.joshuasalcedo.parser.java.model.ProjectRepresentation;
import io.joshuasalcedo.parser.java.model.ClassRepresentation;
import io.joshuasalcedo.parser.maven.model.ProjectModule;
import io.joshuasalcedo.parser.maven.model.ParsedDependency;
import org.apache.maven.api.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public final class CommandLineRunnerImpl implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CommandLineRunnerImpl.class);

    // ANSI color codes
    private static final String RESET = "\033[0m";
    private static final String BOLD = "\033[1m";
    private static final String DIM = "\033[2m";

    // Colors
    private static final String RED = "\033[0;31m";
    private static final String GREEN = "\033[0;32m";
    private static final String YELLOW = "\033[0;33m";
    private static final String BLUE = "\033[0;34m";
    private static final String PURPLE = "\033[0;35m";
    private static final String CYAN = "\033[0;36m";
    private static final String WHITE = "\033[0;37m";

    // Bright colors
    private static final String BRIGHT_RED = "\033[0;91m";
    private static final String BRIGHT_GREEN = "\033[0;92m";
    private static final String BRIGHT_YELLOW = "\033[0;93m";
    private static final String BRIGHT_BLUE = "\033[0;94m";
    private static final String BRIGHT_PURPLE = "\033[0;95m";
    private static final String BRIGHT_CYAN = "\033[0;96m";

    // Background colors
    private static final String BG_BLACK = "\033[40m";
    private static final String BG_BLUE = "\033[44m";

    // Box drawing characters
    private static final String HORIZONTAL = "━";
    private static final String VERTICAL = "┃";
    private static final String TOP_LEFT = "┏";
    private static final String TOP_RIGHT = "┓";
    private static final String BOTTOM_LEFT = "┗";
    private static final String BOTTOM_RIGHT = "┛";
    private static final String TREE_BRANCH = "├─";
    private static final String TREE_LAST = "└─";
    private static final String TREE_VERTICAL = "│ ";

    private final ProjectRepresentation projectRepresentation;
    private final ProjectModule projectModule;

    public CommandLineRunnerImpl(ProjectRepresentation projectRepresentation, ProjectModule projectModule) {
        this.projectRepresentation = projectRepresentation;
        this.projectModule = projectModule;
    }

    @Override
    public void run(String... args) throws Exception {
        printHeader();
        printJavaProjectStructure();
        printMavenModuleStructure();
        printFooter();
    }

    private void printHeader() {
        log.info("");
        String title = " PROJECT ANALYSIS REPORT ";
        int width = 80;
        int padding = (width - title.length() - 2) / 2;

        StringBuilder topLine = new StringBuilder();
        topLine.append(BRIGHT_CYAN).append(TOP_LEFT);
        for (int i = 0; i < width; i++) {
            topLine.append(HORIZONTAL);
        }
        topLine.append(TOP_RIGHT).append(RESET);
        log.info(topLine.toString());

        StringBuilder titleLine = new StringBuilder();
        titleLine.append(BRIGHT_CYAN).append(VERTICAL).append(RESET);
        titleLine.append(" ".repeat(padding));
        titleLine.append(BOLD).append(BRIGHT_YELLOW).append(title).append(RESET);
        titleLine.append(" ".repeat(width - padding - title.length()));
        titleLine.append(BRIGHT_CYAN).append(VERTICAL).append(RESET);
        log.info(titleLine.toString());

        StringBuilder bottomLine = new StringBuilder();
        bottomLine.append(BRIGHT_CYAN).append(BOTTOM_LEFT);
        for (int i = 0; i < width; i++) {
            bottomLine.append(HORIZONTAL);
        }
        bottomLine.append(BOTTOM_RIGHT).append(RESET);
        log.info(bottomLine.toString());
        log.info("");
    }

    private void printJavaProjectStructure() {
        log.info(BOLD + BRIGHT_BLUE + "📁 JAVA PROJECT STRUCTURE" + RESET);
        log.info(DIM + "═".repeat(50) + RESET);

        if (projectRepresentation != null) {
            log.info(BRIGHT_GREEN + "  Project Name: " + RESET + BOLD + projectRepresentation.getName() + RESET);
            log.info(BRIGHT_GREEN + "  Root Path: " + RESET + projectRepresentation.getRootPath());
            log.info(BRIGHT_GREEN + "  Total Classes: " + RESET + BRIGHT_YELLOW + projectRepresentation.getClasses().size() + RESET);
            log.info("");

            List<ClassRepresentation> classes = projectRepresentation.getClasses();
            for (int i = 0; i < classes.size(); i++) {
                boolean isLast = (i == classes.size() - 1);
                printClassRepresentation(classes.get(i), "", isLast);
            }
        } else {
            log.info(RED + "  No Java project representation available" + RESET);
        }
        log.info("");
    }

    private void printClassRepresentation(ClassRepresentation classRep, String indent, boolean isLast) {
        String branch = isLast ? TREE_LAST : TREE_BRANCH;
        String extension = isLast ? "  " : TREE_VERTICAL;

        // Class header
        StringBuilder classHeader = new StringBuilder();
        classHeader.append(indent).append(DIM).append(branch).append(RESET);
        classHeader.append(BRIGHT_PURPLE).append(" class ").append(RESET);
        classHeader.append(BOLD).append(classRep.getName()).append(RESET);
        log.info(classHeader.toString());

        String newIndent = indent + extension;

        // Package
        if (classRep.getPackageName() != null && !classRep.getPackageName().isEmpty()) {
            log.info(newIndent + DIM + "├─" + RESET + CYAN + " package: " + RESET + classRep.getPackageName());
        }

        // Class type info
        String typeInfo = buildTypeInfo(classRep);
        if (!typeInfo.isEmpty()) {
            log.info(newIndent + DIM + "├─" + RESET + YELLOW + " type: " + RESET + typeInfo);
        }

        // Methods
        if (!classRep.getMethods().isEmpty()) {
            log.info(newIndent + DIM + "└─" + RESET + GREEN + " methods (" + classRep.getMethods().size() + "):" + RESET);
            classRep.getMethods().forEach(method -> {
                log.info(newIndent + "   " + DIM + "•" + RESET + " " + method.getName() + "()");
            });
        }
    }

    private String buildTypeInfo(ClassRepresentation classRep) {
        StringBuilder info = new StringBuilder();
        if (classRep.isInterface()) info.append("interface ");
        if (classRep.isEnum()) info.append("enum ");
        if (classRep.isAbstract()) info.append("abstract ");
        if (classRep.isPackagePrivate() )info.append("package ");
        if (info.length() == 0) info.append("class");
        return info.toString().trim();
    }

    private void printMavenModuleStructure() {
        log.info(BOLD + BRIGHT_BLUE + "📦 MAVEN MODULE STRUCTURE" + RESET);
        log.info(DIM + "═".repeat(50) + RESET);

        if (projectModule != null && projectModule.module() != null) {
            Model module = projectModule.module();

            // Main module info
            log.info(BRIGHT_GREEN + "  Module: " + RESET + BOLD + 
                   module.getArtifactId() + RESET + " " + DIM + "v" + module.getVersion() + RESET);
            log.info(BRIGHT_GREEN + "  Group ID: " + RESET + module.getGroupId());
            log.info(BRIGHT_GREEN + "  Packaging: " + RESET + BRIGHT_CYAN + module.getPackaging() + RESET);

            // Submodules
            if (projectModule.subModules() != null && !projectModule.subModules().isEmpty()) {
                log.info("");
                log.info(BRIGHT_YELLOW + "  📂 Submodules (" + projectModule.subModules().size() + "):" + RESET);
                for (int i = 0; i < projectModule.subModules().size(); i++) {
                    Model subModule = projectModule.subModules().get(i);
                    boolean isLast = (i == projectModule.subModules().size() - 1);
                    String branch = isLast ? "  └─" : "  ├─";
                    log.info(DIM + branch + RESET + " " + BRIGHT_PURPLE + subModule.getArtifactId() + RESET + 
                           " " + DIM + "(" + subModule.getPackaging() + ")" + RESET);
                }
            }

            // Dependencies
            printDependencies(projectModule.moduleDependencies());

        } else {
            log.info(RED + "  No Maven module information available" + RESET);
        }
        log.info("");
    }

    private void printDependencies(Map<Model, List<ParsedDependency>> dependencies) {
        if (dependencies == null || dependencies.isEmpty()) {
            return;
        }

        log.info("");
        log.info(BRIGHT_YELLOW + "  📚 Dependencies by Module:" + RESET);

        for (Map.Entry<Model, List<ParsedDependency>> entry : dependencies.entrySet()) {
            Model module = entry.getKey();
            List<ParsedDependency> deps = entry.getValue();

            if (deps != null && !deps.isEmpty()) {
                log.info("");
                log.info("  " + BRIGHT_CYAN + module.getArtifactId() + RESET + " " + 
                       DIM + "(" + deps.size() + " dependencies)" + RESET);

                // Group dependencies by scope
                Map<String, Integer> scopeCount = new HashMap<>();
                deps.forEach(dep -> {
                    String scope = dep.scope() != null ? dep.scope() : "compile";
                    scopeCount.merge(scope, 1, Integer::sum);
                });

                scopeCount.forEach((scope, count) -> {
                    String color = getScopeColor(scope);
                    log.info("    " + color + "◆ " + scope + ": " + count + RESET);
                });
            }
        }
    }

    private String getScopeColor(String scope) {
        return switch (scope.toLowerCase()) {
            case "compile" -> GREEN;
            case "test" -> YELLOW;
            case "provided" -> CYAN;
            case "runtime" -> BLUE;
            case "system" -> RED;
            default -> WHITE;
        };
    }

    private void printFooter() {
        log.info(DIM + "━".repeat(80) + RESET);
        log.info(DIM + "Analysis completed at: " + new java.util.Date() + RESET);
        log.info("");
    }
}
