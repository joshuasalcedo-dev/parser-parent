package io.joshuasalcedo.parser.maven.parser;

import io.joshuasalcedo.parser.maven.model.DependencyNode;
import io.joshuasalcedo.parser.maven.model.ParsedDependency;
import io.joshuasalcedo.parser.maven.service.MavenCentralSearchService;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.Model;
import org.apache.maven.artifact.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for extracting dependency information from Maven models.
 * This provides a simpler alternative to the full Maven dependency resolution
 * by parsing the declared dependencies directly from the POM and enriching them
 * with Maven Central search data.
 */
public class DependencyTreeParser {
    
    private static final Logger logger = LoggerFactory.getLogger(DependencyTreeParser.class);


    /**
     * Creates a DependencyNode tree from the direct dependencies declared in a Maven model.
     * Optionally enriches the dependencies with Maven Central search data.
     * 
     * @param model the Maven model to extract dependencies from
     * @param enrichWithMavenCentral whether to enrich dependencies with Maven Central data
     * @return DependencyNode representing the project with its direct dependencies as children,
     *         or null if model is null
     */
    public static DependencyNode getDirectDependencies(Model model, boolean enrichWithMavenCentral) {
        if (model == null) {
            return null;
        }

        // Create root node for the project itself
        DependencyNode rootNode = new DependencyNode(
            model.getGroupId() != null ? model.getGroupId() : (model.getParent() != null ? model.getParent().getGroupId() : "unknown"),
            model.getArtifactId(),
            model.getVersion() != null ? model.getVersion() : (model.getParent() != null ? model.getParent().getVersion() : "unknown")
        );
        
        rootNode.setType(model.getPackaging() != null ? model.getPackaging() : "jar");

        MavenCentralSearchService searchService = enrichWithMavenCentral ? new MavenCentralSearchService() : null;

        // Add direct dependencies as children
        List<Dependency> dependencies = model.getDependencies();
        for (Dependency dep : dependencies) {
            DependencyNode childNode = new DependencyNode(
                dep.getGroupId(),
                dep.getArtifactId(),
                dep.getVersion() != null ? dep.getVersion() : "managed"
            );
            
            // Set basic properties
            childNode.setScope(dep.getScope() != null ? dep.getScope() : "compile");
            childNode.setType(dep.getType() != null ? dep.getType() : "jar");
            childNode.setClassifier(dep.getClassifier());
            
            // Set optional flag
            if (dep.getOptional() != null && !dep.getOptional().isEmpty() && !dep.getOptional().isBlank()) {
                childNode.setOptional(Boolean.parseBoolean(dep.getOptional()));
            }

            // Enrich with Maven Central data if requested
            if (enrichWithMavenCentral && searchService != null) {
                try {
                    logger.info("Enriching dependency: {}:{}", dep.getGroupId(), dep.getArtifactId());
                    searchService.enrichDependencyNode(childNode);
                } catch (Exception e) {
                    logger.warn("Failed to enrich dependency {}:{} - {}", dep.getGroupId(), dep.getArtifactId(), e.getMessage());
                }
            }

            rootNode.addChild(childNode);
        }
        
        if (searchService != null) {
            searchService.close();
        }
        
        return rootNode;
    }

    /**
     * Formats a DependencyNode tree as a string for debugging/display purposes.
     * 
     * @param node the root DependencyNode
     * @return formatted string representation of the dependency tree
     */
    public static String formatDependencyTree(DependencyNode node) {
        if (node == null) {
            return "No dependencies found";
        }

        StringBuilder sb = new StringBuilder();
        formatDependencyNode(node, sb, "", true);
        return sb.toString();
    }

    private static void formatDependencyNode(DependencyNode node, StringBuilder sb, String prefix, boolean isRoot) {
        if (isRoot) {
            sb.append(node.getGroupId())
              .append(":")
              .append(node.getArtifactId())
              .append(":")
              .append(node.getVersion());
            
            if (node.getDescription() != null && !node.getDescription().isEmpty()) {
                sb.append(" - ").append(node.getDescription());
            }
            
            sb.append("\n");
        } else {
            sb.append(prefix)
              .append("├── ")
              .append(node.getGroupId())
              .append(":")
              .append(node.getArtifactId())
              .append(":")
              .append(node.getVersion());

            if (!node.getScope().equals("compile")) {
                sb.append(" (").append(node.getScope()).append(")");
            }

            if (node.isOptional()) {
                sb.append(" (optional)");
            }

            if (node.isOutdated()) {
                sb.append(" [latest: ").append(node.getLatestVersion()).append("]");
            }

            if (node.getDescription() != null && !node.getDescription().isEmpty()) {
                sb.append(" - ").append(node.getDescription());
            }

            sb.append("\n");
        }

        List<DependencyNode> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            boolean isLast = (i == children.size() - 1);
            String newPrefix = prefix + (isLast ? "    " : "│   ");
            formatDependencyNode(children.get(i), sb, newPrefix, false);
        }
    }

    /**
     * Search Maven Central for artifacts matching a keyword
     * 
     * @param keyword the search keyword
     * @param maxResults maximum number of results to return
     * @return list of DependencyNode objects matching the search
     */
    public static List<DependencyNode> searchMavenCentral(String keyword, int maxResults) {
        MavenCentralSearchService searchService = new MavenCentralSearchService();
        try {
            return searchService.searchByKeyword(keyword, maxResults);
        } finally {
            searchService.close();
        }
    }

    /**
     * Creates a list of ParsedDependency objects from the direct dependencies declared in a Maven model.
     * 
     * @param model the Maven model to extract dependencies from
     * @return List of ParsedDependency objects representing the project's dependencies
     */
    public static List<ParsedDependency> getParsedDependencies(Model model) {
        List<ParsedDependency> parsedDependencies = new ArrayList<>();
        
        if (model == null || model.getDependencies() == null) {
            return parsedDependencies;
        }

        MavenCentralSearchService searchService = new MavenCentralSearchService();
        
        try {
            for (Dependency dep : model.getDependencies()) {
                // Create temporary DependencyNode to use existing enrichment logic
                DependencyNode tempNode = new DependencyNode(
                    dep.getGroupId(),
                    dep.getArtifactId(),
                    dep.getVersion() != null ? dep.getVersion() : "managed"
                );
                
                tempNode.setScope(dep.getScope() != null ? dep.getScope() : "compile");
                tempNode.setType(dep.getType() != null ? dep.getType() : "jar");
                tempNode.setOptional(dep.getOptional() != null && Boolean.parseBoolean(dep.getOptional()));
                
                // Enrich with Maven Central data
                try {
                    searchService.enrichDependencyNode(tempNode);
                } catch (Exception e) {
                    logger.warn("Failed to enrich dependency {}:{} - {}", dep.getGroupId(), dep.getArtifactId(), e.getMessage());
                }
                
                // Convert to ParsedDependency
                ParsedDependency parsedDep = new ParsedDependency(
                    tempNode.getGroupId(),
                    tempNode.getArtifactId(),
                    tempNode.getVersion(),
                    tempNode.getScope(),
                    tempNode.getType(),
                    tempNode.isOptional(),
                    tempNode.getDescription(),
                    tempNode.getArtifactId(), // Using artifactId as name
                    tempNode.getLatestVersion(),
                    tempNode.isOutdated(),
                    tempNode.getHomepage()
                );
                
                parsedDependencies.add(parsedDep);
            }
        } finally {
            searchService.close();
        }
        
        return parsedDependencies;
    }
}