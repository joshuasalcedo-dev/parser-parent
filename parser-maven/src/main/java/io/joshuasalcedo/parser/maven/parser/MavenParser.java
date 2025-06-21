package io.joshuasalcedo.parser.maven.parser;

import org.apache.maven.api.model.Model;
import org.apache.maven.model.v4.MavenStaxReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for parsing Maven POM files and working with Maven project models.
 * This class provides functionality to read and parse Maven POM files, find project
 * directories, and extract information about project modules.
 * <p>
 * The class uses Apache Maven's model API to represent Maven projects and their
 * structure. It provides methods to navigate through multi-module Maven projects
 * and automatically sets POM file locations and project directories for proper
 * module resolution.
 * </p>
 * 
 * <h3>Key Features:</h3>
 * <ul>
 *   <li>Parses Maven POM files into Model objects with proper file location metadata</li>
 *   <li>Automatically resolves and parses submodules in multi-module projects</li>
 *   <li>Handles fallback to current working directory when POM file location is unavailable</li>
 *   <li>Provides robust error handling and logging for troubleshooting</li>
 * </ul>
 */
public class MavenParser {

    private static final Logger logger = LoggerFactory.getLogger(MavenParser.class);
    private static final MavenStaxReader reader = new MavenStaxReader();

    /**
     * Private constructor to prevent direct instantiation.
     * Use {@link #create()} method to obtain an instance.
     */
    private MavenParser() {}

    /**
     * Factory method to create a new instance of MavenParser.
     *
     * @return a new instance of MavenParser
     */
    public static MavenParser create() {
        return new MavenParser();
    }

    /**
     * Retrieves and parses all submodules of the given Maven project model.
     * This method reads the modules defined in the parent POM file and attempts
     * to parse each module's POM file into a Model object.
     * <p>
     * The method intelligently handles POM file location resolution:
     * <ul>
     *   <li>If the parent model has a POM file location, uses its directory as the base</li>
     *   <li>Falls back to the current working directory if POM file location is null</li>
     *   <li>Resolves each module path relative to the base directory</li>
     *   <li>Looks for pom.xml in each module directory</li>
     * </ul>
     * </p>
     *
     * @param model the parent Maven project model containing module definitions
     * @return a list of parsed Model objects representing the submodules, or an empty list
     *         if the parent model is null, has no modules, or if modules cannot be parsed
     */
    public static List<Model> subModules(Model model) {
        List<Model> subModules = new ArrayList<>();
        Path parentDir = null;
        if (model == null) {
            logger.warn("Cannot get SubModules. The parent Model is null");
            return subModules;
        }

        if (model.getModules().isEmpty()) {
            logger.debug("MODULE DOES NOT HAVE SUBMODULES. RETURNING AN EMPTY ARRAY");
            logger.warn("USING DEPRECIATED METHOD model.getModules(). Should use model.getProjects()");
            return subModules;
        }

        if (model.getPomFile() == null) {
            logger.debug("Parent model's POM file is null. Cannot determine base directory for submodules");
            parentDir = Path.of(System.getProperty("user.dir"));
        }else {
            parentDir = model.getPomFile();
        }

        List<String> subModuleNames = model.getModules();
        // Get the parent directory of the POM file

        System.out.println("GETTING THE PARENT DIRECTORY : " + parentDir.toAbsolutePath().toString());

        for (String moduleName : subModuleNames) {
            try {
                // Resolve the module path relative to the parent POM's directory
                Path modulePath = parentDir.resolve(moduleName);
                Path modulePomPath = modulePath.resolve("pom.xml");

                if (!Files.exists(modulePomPath)) {
                    logger.error("Cannot find pom.xml for module: {} at path: {}", moduleName, modulePomPath);
                    continue;
                }

                Model subModule = parseModel(modulePomPath.toFile());
                if (subModule != null) {
                    subModules.add(subModule);
                } else {
                    logger.error("Failed to parse module: {} at path: {}", moduleName, modulePomPath);
                }

            } catch (Exception e) {
                logger.error("Error processing submodule: {} - {}", moduleName, e.getMessage(), e);
            }
        }

        return subModules;
    }

    /**
     * Parses a Maven POM file from a file path string into a Model object.
     * This method delegates to parseModel(File) after converting the string path to a File.
     * The parsed Model will have its POM file location and project directory properly set.
     *
     * @param xml the file path string representing the Maven POM file to parse
     * @return the parsed Model object with file location metadata set, or null if the file cannot be parsed
     */
    public static Model parseModel(String xml) {
        if (xml == null) {
            logger.error("Cannot parse model: File path is null");
            return null;
        }

        return parseModel(new File(xml));
    }


    /**
     * Parses a Maven POM file into a Model object.
     * This method reads the XML content of the specified file and converts it
     * into a Maven Model object using the MavenStaxReader.
     *
     * @param xml the File object representing the Maven POM file to parse
     * @return the parsed Model object, or null if the file is null, doesn't exist,
     *         is not a file, is not readable, or if an error occurs during parsing
     */
    private static Model parseModel(File xml) {
        if (xml == null) {
            logger.error("Cannot parse model: File is null");
            return null;
        }

        if (!xml.exists()) {
            logger.error("Cannot parse model: File does not exist - {}", xml.getAbsolutePath());
            return null;
        }

        if (!xml.isFile()) {
            logger.error("Cannot parse model: Path is not a file - {}", xml.getAbsolutePath());
            return null;
        }

        if (!xml.canRead()) {
            logger.error("Cannot parse model: File is not readable - {}", xml.getAbsolutePath());
            return null;
        }

        try (FileReader fileReader = new FileReader(xml)) {
            Model model = reader.read(fileReader);

            if (model == null) {
                logger.error("Parsed model is null for file: {}", xml.getAbsolutePath());
                return null;
            }

            logger.debug("Successfully parsed model from: {}", xml.getAbsolutePath());
            return model;

        } catch (IOException e) {
            logger.error("IO error reading pom.xml file: {} - {}", xml.getAbsolutePath(), e.getMessage(), e);
        } catch (XMLStreamException e) {
            logger.error("XML parsing error for pom.xml file: {} - {}", xml.getAbsolutePath(), e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error parsing pom.xml file: {} - {}", xml.getAbsolutePath(), e.getMessage(), e);
        }

        return null;
    }
}