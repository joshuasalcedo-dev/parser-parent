package io.joshuasalcedo.parser.java.result;

/**
 * Detailed metrics for a single class
 */
public record ClassMetrics(
    String className,
    int methodCount,
    int fieldCount,
    double complexity,
    int afferentCoupling,  // Classes that depend on this
    int efferentCoupling,  // Classes this depends on
    double cohesion,
    double maintainabilityIndex,
    int linesOfCode,
    int usageCount
) {
    
    public double getInstability() {
        int totalCoupling = afferentCoupling + efferentCoupling;
        return totalCoupling == 0 ? 0.0 : (double) efferentCoupling / totalCoupling;
    }
    
    public String getComplexityLevel() {
        if (complexity < 10) return "Simple";
        if (complexity < 20) return "Moderate";
        if (complexity < 50) return "Complex";
        return "Very Complex";
    }
    
    public String getMaintainabilityLevel() {
        if (maintainabilityIndex >= 80) return "Highly Maintainable";
        if (maintainabilityIndex >= 60) return "Maintainable";
        if (maintainabilityIndex >= 40) return "Moderately Maintainable";
        if (maintainabilityIndex >= 20) return "Difficult to Maintain";
        return "Very Difficult to Maintain";
    }
}
