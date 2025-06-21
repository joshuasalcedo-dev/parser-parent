package io.joshuasalcedo.parser.maven.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom dependency node model for representing Maven dependencies with enhanced information
 * from Maven Central search results.
 */
@Deprecated
public class DependencyNode {
    private String groupId;
    private String artifactId;
    private String version;
    private String scope;
    private String type;
    private String classifier;
    private boolean optional;
    private String description;
    private String latestVersion;
    private long downloadCount;
    private String homepage;
    private List<String> licenses;
    private List<DependencyNode> children;
    private transient DependencyNode parent;

    public DependencyNode() {
        this.children = new ArrayList<>();
        this.licenses = new ArrayList<>();
        this.optional = false;
        this.scope = "compile";
        this.type = "jar";
    }

    public DependencyNode(String groupId, String artifactId, String version) {
        this();
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    // Getters and setters
    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public void setLatestVersion(String latestVersion) {
        this.latestVersion = latestVersion;
    }

    public long getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(long downloadCount) {
        this.downloadCount = downloadCount;
    }

    public String getHomepage() {
        return homepage;
    }

    public void setHomepage(String homepage) {
        this.homepage = homepage;
    }

    public List<String> getLicenses() {
        return licenses;
    }

    public void setLicenses(List<String> licenses) {
        this.licenses = licenses != null ? licenses : new ArrayList<>();
    }

    public List<DependencyNode> getChildren() {
        return children;
    }

    public void setChildren(List<DependencyNode> children) {
        this.children = children != null ? children : new ArrayList<>();
    }

    public DependencyNode getParent() {
        return parent;
    }

    public void setParent(DependencyNode parent) {
        this.parent = parent;
    }

    public void addChild(DependencyNode child) {
        this.children.add(child);
        child.setParent(this);
    }

    /**
     * Gets the coordinate string for this dependency (groupId:artifactId:version)
     */
    public String getCoordinate() {
        return groupId + ":" + artifactId + ":" + version;
    }

    /**
     * Gets the full coordinate string including type and classifier if present
     */
    public String getFullCoordinate() {
        StringBuilder sb = new StringBuilder();
        sb.append(groupId).append(":").append(artifactId).append(":").append(type);
        if (classifier != null && !classifier.isEmpty()) {
            sb.append(":").append(classifier);
        }
        sb.append(":").append(version);
        return sb.toString();
    }

    /**
     * Checks if this dependency is outdated compared to the latest version
     */
    public boolean isOutdated() {
        return latestVersion != null && !latestVersion.equals(version);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getFullCoordinate());
        if (scope != null && !scope.equals("compile")) {
            sb.append(" (").append(scope).append(")");
        }
        if (optional) {
            sb.append(" (optional)");
        }
        if (isOutdated()) {
            sb.append(" [latest: ").append(latestVersion).append("]");
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        DependencyNode that = (DependencyNode) obj;
        return groupId.equals(that.groupId) && 
               artifactId.equals(that.artifactId) && 
               version.equals(that.version);
    }

    @Override
    public int hashCode() {
        return getCoordinate().hashCode();
    }
}