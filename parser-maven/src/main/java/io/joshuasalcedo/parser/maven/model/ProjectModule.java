package io.joshuasalcedo.parser.maven.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import io.joshuasalcedo.parser.maven.parser.MavenParser;
import io.joshuasalcedo.parser.maven.parser.DependencyTreeParser;
import org.apache.maven.api.model.Model;

import java.util.*;

public record ProjectModule(
        Model module,
        List<Model> subModules,
        Map<Model, List<ParsedDependency>> moduleDependencies
) {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public ProjectModule(Model module) {
        this(module, 
             MavenParser.subModules(module), 
             buildModuleDependenciesMap(module, MavenParser.subModules(module)));
    }

    private static Map<Model, List<ParsedDependency>> buildModuleDependenciesMap(Model parentModule, List<Model> subModules) {
        Map<Model, List<ParsedDependency>> dependenciesMap = new HashMap<>();
        
        // Add parent module dependencies
        List<ParsedDependency> parentDeps = DependencyTreeParser.getParsedDependencies(parentModule);
        dependenciesMap.put(parentModule, parentDeps);
        
        // Add submodule dependencies
        if (subModules != null) {
            for (Model subModule : subModules) {
                List<ParsedDependency> subModuleDeps = DependencyTreeParser.getParsedDependencies(subModule);
                dependenciesMap.put(subModule, subModuleDeps);
            }
        }
        
        return dependenciesMap;
    }

    public ProjectModule(String module) {
        this(MavenParser.parseModel(module));
    }
}