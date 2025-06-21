package io.joshuasalcedo.parser.spring.controller;

import io.joshuasalcedo.parser.java.model.*;
import io.joshuasalcedo.parser.java.parser.JavaProjectAnalyzer;

import io.joshuasalcedo.parser.spring.autoconfigure.ParserAutoConfiguration;
import io.joshuasalcedo.parser.spring.config.ParserProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * REST Controller providing endpoints for Java project analysis.
 *
 * This controller exposes the parser functionality through HTTP endpoints,
 * allowing clients to analyze Java projects and retrieve various metrics and insights.
 */
@RestController
@RequestMapping("${parser.api-path:/api/parser}")
@Tag(name = "Java Project Parser", description = "Endpoints for analyzing Java projects and retrieving code metrics")
public class ParserController {

    private static final Logger logger = LoggerFactory.getLogger(ParserController.class);

    private final JavaProjectAnalyzer analyzer;
    private final ParserProperties properties;
    private final ParserAutoConfiguration autoConfiguration;

    public ParserController(JavaProjectAnalyzer analyzer, ParserProperties properties, ParserAutoConfiguration autoConfiguration) {
        this.analyzer = analyzer;
        this.properties = properties;
        this.autoConfiguration = autoConfiguration;
    }

    /**
     * Get the complete project analysis results.
     * Returns cached results from startup if available, otherwise performs fresh analysis.
     */
    @GetMapping(value = "/analysis", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get complete project analysis",
            description = "Returns comprehensive analysis of the Java project including metrics, dependencies, and code quality information. " +
                    "If analysis was performed on startup, returns cached results for better performance."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Analysis completed successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProjectAnalysisResult.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Analysis failed due to internal error",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid project path or configuration",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<ProjectAnalysisResult> getProjectAnalysis(
            @Parameter(description = "Project path to analyze. If not provided, uses configured default path")
            @RequestParam(required = false) String projectPath
    ) {
        try {
            // Use cached result if available and no specific path requested
            if (projectPath == null && autoConfiguration.hasCachedResult()) {
                logger.debug("Returning cached analysis result");
                return ResponseEntity.ok(autoConfiguration.getCachedAnalysisResult());
            }

            // Perform fresh analysis
            String pathToAnalyze = projectPath != null ? projectPath : determineProjectPath();
            logger.info("Performing fresh analysis for path: {}", pathToAnalyze);

            ProjectAnalysisResult result = analyzer.analyzeProject(pathToAnalyze);
            return ResponseEntity.ok(result);

        } catch (IOException e) {
            logger.error("Failed to analyze project", e);
            return ResponseEntity.internalServerError().build();
        } catch (IllegalArgumentException e) {
            logger.error("Invalid project configuration", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get project summary with basic statistics.
     */
    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get project summary",
            description = "Returns a quick summary of the project with basic statistics like class count, method count, and package distribution."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Summary retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProjectSummary.class)
                    )
            ),
            @ApiResponse(responseCode = "500", description = "Failed to retrieve summary")
    })
    public ResponseEntity<ProjectSummary> getProjectSummary(
            @Parameter(description = "Project path to analyze")
            @RequestParam(required = false) String projectPath
    ) {
        try {
            String pathToAnalyze = projectPath != null ? projectPath : determineProjectPath();
            ProjectSummary summary = analyzer.getProjectSummary(pathToAnalyze);
            return ResponseEntity.ok(summary);

        } catch (IOException e) {
            logger.error("Failed to get project summary", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get dependency analysis results.
     */
    @GetMapping(value = "/dependencies", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get dependency analysis",
            description = "Returns detailed dependency analysis including coupling metrics, circular dependencies, and dependency graphs."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Dependency analysis completed",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProjectDependencyResult.class)
                    )
            ),
            @ApiResponse(responseCode = "500", description = "Dependency analysis failed")
    })
    public ResponseEntity<ProjectDependencyResult> getDependencyAnalysis(
            @Parameter(description = "Project path to analyze")
            @RequestParam(required = false) String projectPath
    ) {
        try {
            String pathToAnalyze = projectPath != null ? projectPath : determineProjectPath();
            ProjectDependencyResult result = analyzer.analyzeDependencies(pathToAnalyze);
            return ResponseEntity.ok(result);

        } catch (IOException e) {
            logger.error("Failed to analyze dependencies", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get code metrics analysis.
     */
    @GetMapping(value = "/metrics", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get code metrics",
            description = "Returns comprehensive code metrics including complexity, maintainability index, code duplication, and quality scores."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Metrics analysis completed",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MetricsResult.class)
                    )
            ),
            @ApiResponse(responseCode = "500", description = "Metrics analysis failed")
    })
    public ResponseEntity<MetricsResult> getMetricsAnalysis(
            @Parameter(description = "Project path to analyze")
            @RequestParam(required = false) String projectPath
    ) {
        try {
            String pathToAnalyze = projectPath != null ? projectPath : determineProjectPath();
            MetricsResult result = analyzer.analyzeMetrics(pathToAnalyze);
            return ResponseEntity.ok(result);

        } catch (IOException e) {
            logger.error("Failed to analyze metrics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get project health assessment.
     */
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get project health assessment",
            description = "Returns overall health assessment of the project including health score, recommendations, and identified risks."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Health assessment completed",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProjectHealthAssessment.class)
                    )
            ),
            @ApiResponse(responseCode = "500", description = "Health assessment failed")
    })
    public ResponseEntity<ProjectHealthAssessment> getProjectHealth(
            @Parameter(description = "Project path to analyze")
            @RequestParam(required = false) String projectPath
    ) {
        try {
            String pathToAnalyze = projectPath != null ? projectPath : determineProjectPath();
            ProjectHealthAssessment health = analyzer.assessProjectHealth(pathToAnalyze);
            return ResponseEntity.ok(health);

        } catch (IOException e) {
            logger.error("Failed to assess project health", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Search for code patterns in the project.
     */
    @PostMapping(value = "/patterns", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Search for code patterns",
            description = "Searches for specific code patterns in the project based on provided pattern definitions."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Pattern search completed",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PatternSearchResult.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid pattern definitions"),
            @ApiResponse(responseCode = "500", description = "Pattern search failed")
    })
    public ResponseEntity<PatternSearchResult> searchPatterns(
            @Parameter(description = "List of code patterns to search for")
            @RequestBody List<CodePattern> patterns,
            @Parameter(description = "Project path to analyze")
            @RequestParam(required = false) String projectPath
    ) {
        try {
            if (patterns == null || patterns.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            String pathToAnalyze = projectPath != null ? projectPath : determineProjectPath();
            PatternSearchResult result = analyzer.findPatterns(pathToAnalyze, patterns);
            return ResponseEntity.ok(result);

        } catch (IOException e) {
            logger.error("Failed to search patterns", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Clear cached analysis results.
     */
    @DeleteMapping("/cache")
    @Operation(
            summary = "Clear analysis cache",
            description = "Clears cached analysis results, forcing fresh analysis on next request."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cache cleared successfully"),
            @ApiResponse(responseCode = "500", description = "Failed to clear cache")
    })
    public ResponseEntity<Map<String, String>> clearCache() {
        try {
            autoConfiguration.clearCache();
            logger.info("Analysis cache cleared");
            return ResponseEntity.ok(Map.of("message", "Cache cleared successfully"));

        } catch (Exception e) {
            logger.error("Failed to clear cache", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get parser configuration and status.
     */
    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get parser status",
            description = "Returns current parser configuration and status information."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Status retrieved successfully",
            content = @Content(mediaType = "application/json")
    )
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "configuredProjectPath", properties.getProjectPath() != null ? properties.getProjectPath() : "default",
                "autoAnalyzeOnStartup", properties.isAutoAnalyzeOnStartup(),
                "parallelAnalysis", properties.isParallelAnalysis(),
                "analyzeDependencies", properties.isAnalyzeDependencies(),
                "analyzeMetrics", properties.isAnalyzeMetrics(),
                "generateGraphs", properties.isGenerateGraphs(),
                "hasCachedResults", autoConfiguration.hasCachedResult(),
                "apiPath", properties.getApiPath()
        ));
    }

    private String determineProjectPath() {
        if (properties.getProjectPath() != null && !properties.getProjectPath().trim().isEmpty()) {
            return properties.getProjectPath();
        }

        // Default to project root directory (look for pom.xml or build.gradle)
        String currentDir = System.getProperty("user.dir");
        String projectRoot = findProjectRoot(currentDir);
        return projectRoot != null ? projectRoot : currentDir;
    }

    private String findProjectRoot(String startDir) {
        java.io.File dir = new java.io.File(startDir);
        while (dir != null && dir.exists()) {
            // Check for Maven project
            if (new java.io.File(dir, "pom.xml").exists()) {
                return dir.getAbsolutePath();
            }
            // Check for Gradle project
            if (new java.io.File(dir, "build.gradle").exists() || new java.io.File(dir, "build.gradle.kts").exists()) {
                return dir.getAbsolutePath();
            }
            // Check for multi-module project indicators
            if (new java.io.File(dir, "settings.gradle").exists() || new java.io.File(dir, "settings.gradle.kts").exists()) {
                return dir.getAbsolutePath();
            }
            dir = dir.getParentFile();
        }
        return null;
    }
}