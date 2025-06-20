package io.joshuasalcedo.parser.maven.model;

public record ParsedDependency(
        String groupId,
        String artifactId,
        String version,
        String scope,
        String type,
        boolean optional,
        String description,
        String name,
        String latestVersion,
        boolean isOutdated,
        String url

//   "groupId": "org.apache.maven",
//        "artifactId": "maven-core",
//        "version": "4.0.0-rc-3",
//        "scope": "compile",
//        "type": "jar",
//        "optional": false,
//        "description": "org.apache.maven",
//        "latestVersion": "4.0.0-rc-3",
//        "isOutdated": false,
) {
}
