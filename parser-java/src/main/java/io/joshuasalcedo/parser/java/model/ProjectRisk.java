package io.joshuasalcedo.parser.java.model;

import io.joshuasalcedo.parser.java.parser.RiskLevel; /**
 * Project risk
 */
public class ProjectRisk {
    private final RiskLevel level;
    private final String category;
    private final String description;
    private final String mitigation;
    
    public ProjectRisk(RiskLevel level, String category, String description, String mitigation) {
        this.level = level;
        this.category = category;
        this.description = description;
        this.mitigation = mitigation;
    }
    
    public RiskLevel getLevel() { return level; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getMitigation() { return mitigation; }
}
