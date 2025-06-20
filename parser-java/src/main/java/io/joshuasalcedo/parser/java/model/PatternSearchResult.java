package io.joshuasalcedo.parser.java.model;

import java.util.Collections;
import java.util.List;
import java.util.Map; /**
 * Pattern search result
 */
public class PatternSearchResult {
    private final Map<CodePattern, List<PatternMatch>> matches;
    
    public PatternSearchResult(Map<CodePattern, List<PatternMatch>> matches) {
        this.matches = matches;
    }
    
    public Map<CodePattern, List<PatternMatch>> getMatches() { return matches; }
    
    public int getTotalMatches() {
        return matches.values().stream().mapToInt(List::size).sum();
    }
    
    public List<PatternMatch> getMatchesForPattern(CodePattern pattern) {
        return matches.getOrDefault(pattern, Collections.emptyList());
    }
}
