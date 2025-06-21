package io.joshuasalcedo.parser.spring.autoconfigure;

import io.joshuasalcedo.parser.java.model.ProjectRepresentation;
import io.joshuasalcedo.parser.java.parser.AnalyzerConfig;
import io.joshuasalcedo.parser.java.parser.JavaProjectAnalyzer;
import io.joshuasalcedo.parser.java.model.ProjectAnalysisResult;
import io.joshuasalcedo.parser.maven.model.ProjectModule;
import io.joshuasalcedo.parser.spring.config.CommandLineRunnerImpl;
import io.joshuasalcedo.parser.spring.config.ParserConfig;
import io.joshuasalcedo.parser.spring.config.ParserProperties;
import io.joshuasalcedo.parser.spring.controller.ParserController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;

import java.io.File;
import java.io.IOException;

/**
 * Auto-configuration for the Java Parser Spring Boot Starter.
 * 
 * This configuration automatically sets up the parser components when the starter is included.
 * It listens for the ApplicationStartedEvent to trigger initial project analysis if enabled.
 */
@AutoConfiguration
@ConditionalOnClass(JavaProjectAnalyzer.class)
@EnableConfigurationProperties(ParserProperties.class)
@Import({ParserConfig.class})
public class ParserAutoConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(ParserAutoConfiguration.class);


    @Bean
    CommandLineRunner commandLineRunner(ProjectRepresentation projectRepresentation, ProjectModule projectModule) {
        return new CommandLineRunnerImpl(projectRepresentation, projectModule);
    }
    private final ParserProperties properties;
    private volatile ProjectAnalysisResult cachedAnalysisResult;
    
    public ParserAutoConfiguration(ParserProperties properties) {
        this.properties = properties;
        logger.info("🚀 Java Parser Auto-Configuration initialized!");
        logger.info("📊 Auto-analyze on startup: {}", properties.isAutoAnalyzeOnStartup());
        logger.info("🌐 API path: {}", properties.getApiPath());
    }
    
    /**
     * Creates the JavaProjectAnalyzer bean with configuration from properties.
     */
    @Bean
    public JavaProjectAnalyzer javaProjectAnalyzer() {
        logger.info("🔧 Creating JavaProjectAnalyzer bean...");
        AnalyzerConfig config = new AnalyzerConfig()
            .withParallelAnalysis(properties.isParallelAnalysis())
            .withThreadPoolSize(properties.getThreadPoolSize())
            .withAnalyzeDependencies(properties.isAnalyzeDependencies())
            .withAnalyzeMetrics(properties.isAnalyzeMetrics())
            .withGenerateGraphs(properties.isGenerateGraphs());
            
        JavaProjectAnalyzer analyzer = new JavaProjectAnalyzer(config);
        logger.info("✅ JavaProjectAnalyzer bean created successfully");
        return analyzer;
    }
    
    /**
     * Creates the REST controller for parser endpoints.
     */
    @Bean
    public ParserController parserController(JavaProjectAnalyzer analyzer) {
        logger.info("🎮 Creating ParserController bean...");
        ParserController controller = new ParserController(analyzer, properties, this);
        logger.info("✅ ParserController created - API available at: {}", properties.getApiPath());
        return controller;
    }
    
    
    /**
     * Event listener that triggers project analysis when the application starts.
     * This runs after the application context is fully initialized.
     */
    @EventListener
    @Order(1000) // Run after most other startup tasks
    public void onApplicationStarted(ApplicationStartedEvent event) {
        if (!properties.isAutoAnalyzeOnStartup()) {
            logger.info("Auto-analysis on startup is disabled");
            return;
        }
        
        String projectPath = determineProjectPath();
        logger.info("Starting automatic project analysis for path: {}", projectPath);
        
        try {
            long startTime = System.currentTimeMillis();
            
            try (JavaProjectAnalyzer analyzer = javaProjectAnalyzer()) {
                cachedAnalysisResult = analyzer.analyzeProject(projectPath);
                
                long duration = System.currentTimeMillis() - startTime;
                logger.info("Project analysis completed in {}ms. Found {} classes, {} methods", 
                    duration,
                    cachedAnalysisResult.getProject().getClasses().size(),
                    cachedAnalysisResult.getProject().getClasses().stream()
                        .mapToInt(cls -> cls.getMethods().size())
                        .sum());
                        
                logger.info("🌐 Parser API endpoints available at: {}", properties.getApiPath());
                logger.info("📚 OpenAPI docs available at: /swagger-ui.html");
            }
            
        } catch (Exception e) {
            logger.error("Failed to analyze project on startup", e);
            // Don't fail application startup due to analysis errors
        }
    }
    
    /**
     * Determines the project path to analyze.
     * Uses configured path or falls back to current working directory.
     */
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
        File dir = new File(startDir);
        while (dir != null && dir.exists()) {
            // Check for Maven project
            if (new File(dir, "pom.xml").exists()) {
                return dir.getAbsolutePath();
            }
            // Check for Gradle project
            if (new File(dir, "build.gradle").exists() || new File(dir, "build.gradle.kts").exists()) {
                return dir.getAbsolutePath();
            }
            // Check for multi-module project indicators
            if (new File(dir, "settings.gradle").exists() || new File(dir, "settings.gradle.kts").exists()) {
                return dir.getAbsolutePath();
            }
            dir = dir.getParentFile();
        }
        return null;
    }
    
    /**
     * Gets the cached analysis result from startup, if available.
     */
    public ProjectAnalysisResult getCachedAnalysisResult() {
        return cachedAnalysisResult;
    }
    
    /**
     * Clears the cached analysis result.
     */
    public void clearCache() {
        this.cachedAnalysisResult = null;
    }
    
    /**
     * Checks if there is a cached analysis result available.
     */
    public boolean hasCachedResult() {
        return cachedAnalysisResult != null;
    }
}