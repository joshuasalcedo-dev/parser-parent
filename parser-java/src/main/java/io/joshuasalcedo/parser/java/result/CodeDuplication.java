package io.joshuasalcedo.parser.java.result;

import java.util.List; /**
 * Code duplication information
 */
public record CodeDuplication(
    List<String> methods,
    int lineCount,
    double impact,
    boolean isSimilar  // true if similar but not exact duplicate
) {
    
    public CodeDuplication(List<String> methods, int lineCount, double impact) {
        this(methods, lineCount, impact, false);
    }
    
    public int getDuplicateCount() {
        return methods.size() - 1; // Subtract 1 for the original
    }
    
    public int getTotalDuplicatedLines() {
        return lineCount * getDuplicateCount();
    }
    
    public String getDuplicationType() {
        return isSimilar ? "Similar Code" : "Exact Duplicate";
    }
}
