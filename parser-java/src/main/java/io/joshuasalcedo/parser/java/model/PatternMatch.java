package io.joshuasalcedo.parser.java.model;

/**
 * Pattern match result
 */
public class PatternMatch {
    private final CodePattern pattern;
    private final String elementName;
    private final String filePath;
    private final String elementType;
    private final String elementSimpleName;
    
    public PatternMatch(CodePattern pattern, String elementName, String filePath,
                       String elementType, String elementSimpleName) {
        this.pattern = pattern;
        this.elementName = elementName;
        this.filePath = filePath;
        this.elementType = elementType;
        this.elementSimpleName = elementSimpleName;
    }
    
    public CodePattern getPattern() { return pattern; }
    public String getElementName() { return elementName; }
    public String getFilePath() { return filePath; }
    public String getElementType() { return elementType; }
    public String getElementSimpleName() { return elementSimpleName; }
}
