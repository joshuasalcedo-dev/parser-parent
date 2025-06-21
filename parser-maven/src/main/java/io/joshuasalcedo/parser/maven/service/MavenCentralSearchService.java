package io.joshuasalcedo.parser.maven.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.joshuasalcedo.parser.maven.model.DependencyNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for searching Maven Central repository using their REST API.
 * Provides methods to search for artifacts and enrich dependency information.
 */
public class MavenCentralSearchService {
    
    private static final Logger logger = LoggerFactory.getLogger(MavenCentralSearchService.class);
    private static final String MAVEN_CENTRAL_SEARCH_URL = "https://search.maven.org/solrsearch/select";
    private static final Gson gson = new Gson();
    private final HttpClient httpClient;

    public MavenCentralSearchService() {
        this.httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    /**
     * Search for an artifact by groupId and artifactId
     */
    public DependencyNode searchArtifact(String groupId, String artifactId) {
        try {
            // Build the query with proper URL encoding
            String query = String.format("g:%s+AND+a:%s", 
                URLEncoder.encode(groupId, StandardCharsets.UTF_8),
                URLEncoder.encode(artifactId, StandardCharsets.UTF_8));
            
            String url = String.format("%s?q=%s&rows=1&wt=json", MAVEN_CENTRAL_SEARCH_URL, query);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return parseSearchResponse(response.body(), groupId, artifactId);
            } else {
                logger.warn("Maven Central search failed with status: {} for {}:{}", 
                    response.statusCode(), groupId, artifactId);
            }
        } catch (Exception e) {
            logger.error("Error searching Maven Central for {}:{} - {}", groupId, artifactId, e.getMessage(), e);
        }
        
        return null;
    }

    /**
     * Search for all versions of an artifact
     */
    public List<String> searchAllVersions(String groupId, String artifactId) {
        List<String> versions = new ArrayList<>();
        
        try {
            String query = String.format("g:%s+AND+a:%s", 
                URLEncoder.encode(groupId, StandardCharsets.UTF_8),
                URLEncoder.encode(artifactId, StandardCharsets.UTF_8));
            
            String url = String.format("%s?q=%s&core=gav&rows=50&wt=json", MAVEN_CENTRAL_SEARCH_URL, query);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
                JsonObject responseObj = jsonResponse.getAsJsonObject("response");
                JsonArray docs = responseObj.getAsJsonArray("docs");
                
                for (int i = 0; i < docs.size(); i++) {
                    JsonObject doc = docs.get(i).getAsJsonObject();
                    if (doc.has("v")) {
                        versions.add(doc.get("v").getAsString());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error searching versions for {}:{} - {}", groupId, artifactId, e.getMessage(), e);
        }
        
        return versions;
    }

    /**
     * Basic search by keyword
     */
    public List<DependencyNode> searchByKeyword(String keyword, int maxResults) {
        List<DependencyNode> results = new ArrayList<>();
        
        try {
            String query = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String url = String.format("%s?q=%s&rows=%d&wt=json", MAVEN_CENTRAL_SEARCH_URL, query, maxResults);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
                JsonObject responseObj = jsonResponse.getAsJsonObject("response");
                JsonArray docs = responseObj.getAsJsonArray("docs");
                
                for (int i = 0; i < docs.size(); i++) {
                    JsonObject doc = docs.get(i).getAsJsonObject();
                    DependencyNode node = parseDocumentToNode(doc);
                    if (node != null) {
                        results.add(node);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error searching by keyword '{}' - {}", keyword, e.getMessage(), e);
        }
        
        return results;
    }

    /**
     * Enrich a dependency node with additional information from Maven Central
     */
    public void enrichDependencyNode(DependencyNode node) {
        if (node == null || node.getGroupId() == null || node.getArtifactId() == null) {
            return;
        }

        DependencyNode enriched = searchArtifact(node.getGroupId(), node.getArtifactId());
        if (enriched != null) {
            // Update with enriched information
            node.setDescription(enriched.getDescription());
            node.setLatestVersion(enriched.getLatestVersion());
            node.setDownloadCount(enriched.getDownloadCount());
            node.setHomepage(enriched.getHomepage());
            node.setLicenses(enriched.getLicenses());
        }
    }

    private DependencyNode parseSearchResponse(String responseBody, String expectedGroupId, String expectedArtifactId) {
        try {
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            JsonObject responseObj = jsonResponse.getAsJsonObject("response");
            JsonArray docs = responseObj.getAsJsonArray("docs");
            
            if (docs.size() > 0) {
                JsonObject doc = docs.get(0).getAsJsonObject();
                return parseDocumentToNode(doc);
            }
        } catch (Exception e) {
            logger.error("Error parsing Maven Central response for {}:{} - {}", 
                expectedGroupId, expectedArtifactId, e.getMessage(), e);
        }
        
        return null;
    }

    private DependencyNode parseDocumentToNode(JsonObject doc) {
        try {
            DependencyNode node = new DependencyNode();
            
            // Required fields
            if (doc.has("g")) node.setGroupId(doc.get("g").getAsString());
            if (doc.has("a")) node.setArtifactId(doc.get("a").getAsString());
            if (doc.has("v")) node.setVersion(doc.get("v").getAsString());
            if (doc.has("latestVersion")) node.setLatestVersion(doc.get("latestVersion").getAsString());
            else if (doc.has("v")) node.setLatestVersion(doc.get("v").getAsString()); // fallback
            
            // Optional fields
            if (doc.has("p")) node.setType(doc.get("p").getAsString());
            if (doc.has("repositoryId")) node.setClassifier(doc.get("repositoryId").getAsString());
            
            // Descriptive fields (not always available)
            if (doc.has("text")) {
                JsonArray textArray = doc.getAsJsonArray("text");
                if (!textArray.isEmpty()) {
                    node.setDescription(textArray.get(0).getAsString());
                }
            }
            
            // License information
            if (doc.has("license")) {
                JsonArray licenseArray = doc.getAsJsonArray("license");
                List<String> licenses = new ArrayList<>();
                for (int i = 0; i < licenseArray.size(); i++) {
                    licenses.add(licenseArray.get(i).getAsString());
                }
                node.setLicenses(licenses);
            }
            
            // Download count (if available)
            if (doc.has("popularity")) {
                node.setDownloadCount(doc.get("popularity").getAsLong());
            }
            
            return node;
        } catch (Exception e) {
            logger.error("Error parsing document to node - {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Close the HTTP client
     */
    public void close() {
        // HttpClient doesn't need explicit closing in Java 11+
    }
}