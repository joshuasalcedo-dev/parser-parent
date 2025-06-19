package io.joshuasalcedo.parser.java.result;

import java.util.Set; /**
 * Method usage statistics
 */
public record MethodUsage(
    String methodId,
    int callCount,
    Set<String> callers,
    String usageLevel
) {
    
    public boolean isUnused() {
        return callCount == 0;
    }
    
    public boolean isHotspot() {
        return callCount > 50 || callers.size() > 10;
    }
    
    public double getReuseFactor() {
        return callers.isEmpty() ? 0.0 : (double) callCount / callers.size();
    }
}
