package io.joshuasalcedo.parser.maven;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import io.joshuasalcedo.parser.maven.model.ProjectModule;
import io.joshuasalcedo.parser.maven.parser.DependencyTreeParser;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class MavenTest {

    public static void main(String[] args) throws Exception {
        System.out.println("Current Directory: " + System.getProperty("user.dir"));

        // Create ProjectModule with basic parsing
        ProjectModule projectModule = new ProjectModule("pom.xml");
        System.out.println("=== Project Info ===");

        // Use the custom serialization method to avoid circular reference issues
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        // Save the JSON to a file
        try (FileWriter writer = new FileWriter("project_info.json")) {
            gson.toJson(projectModule, writer);

            gson.toJson(projectModule, System.out);

            for(int i = 0; i < projectModule.subModules().size(); i++){
                FileWriter depWriter = new FileWriter(projectModule.subModules()
                        .get(i).getArtifactId() + "-dependencies.json");
                gson.toJson(projectModule.subModules().get(i), depWriter);
            }


            System.out.println("JSON data saved to project_info.json");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}