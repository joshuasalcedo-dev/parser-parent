package io.joshuasalcedo.parser.spring.config;

import io.joshuasalcedo.parser.java.model.ProjectRepresentation;
import io.joshuasalcedo.parser.java.parser.JavaProjectAnalyzer;
import io.joshuasalcedo.parser.java.parser.JavaProjectParser;
import io.joshuasalcedo.parser.maven.model.ProjectModule;
import io.joshuasalcedo.parser.maven.parser.MavenParser;
import org.apache.maven.api.Project;
import org.jline.utils.Colors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.awt.*;


@Configuration
public class ParserConfig {
    private static final Logger logger = LoggerFactory.getLogger(ParserConfig.class);

    @Bean
    public ProjectRepresentation getProjectRepresentation() {
        logger.debug("getJavaProjectParser Bean at {}", Thread.currentThread().getContextClassLoader());
        logger.debug("Current Directory {}", System.getProperty("user.dir"));
        ProjectRepresentation projectRepresentation = new JavaProjectParser().parseProject(System.getProperty("user.dir"));
        logger.info("ProjectRepresentation Loaded");
        logger.info("Project name: \033[1;4;32m{}\033[0m", projectRepresentation.getName());
        return  projectRepresentation;
    }


    @Bean
    public ProjectModule getProjectModule() {
        logger.debug("getProjectModule Bean at {}", Thread.currentThread().getContextClassLoader());
        logger.debug("Current Directory {}", System.getProperty("user.dir"));
        ProjectModule projectModule = new ProjectModule("pom.xml");
        logger.info("Project Module Loaded");
        logger.info("Project name: \033[1;4;32m{}\033[0m", projectModule.module().getName());
        return projectModule;
    }





}
