package io.joshuasalcedo.parser.java.model;

import java.util.Map; /**
 * Lightweight project summary
 */
public class ProjectSummary {
    private final String projectName;
    private final String projectPath;
    private final int totalClasses;
    private final int totalInterfaces;
    private final int totalEnums;
    private final int totalMethods;
    private final int totalLinesOfCode;
    private final Map<String, Integer> packageDistribution;
    
    public ProjectSummary(String projectName, String projectPath, 
                         int totalClasses, int totalInterfaces, int totalEnums,
                         int totalMethods, int totalLinesOfCode,
                         Map<String, Integer> packageDistribution) {
        this.projectName = projectName;
        this.projectPath = projectPath;
        this.totalClasses = totalClasses;
        this.totalInterfaces = totalInterfaces;
        this.totalEnums = totalEnums;
        this.totalMethods = totalMethods;
        this.totalLinesOfCode = totalLinesOfCode;
        this.packageDistribution = packageDistribution;
    }
    
    // Getters
    public String getProjectName() { return projectName; }
    public String getProjectPath() { return projectPath; }
    public int getTotalClasses() { return totalClasses; }
    public int getTotalInterfaces() { return totalInterfaces; }
    public int getTotalEnums() { return totalEnums; }
    public int getTotalMethods() { return totalMethods; }
    public int getTotalLinesOfCode() { return totalLinesOfCode; }
    public Map<String, Integer> getPackageDistribution() { return packageDistribution; }
}
