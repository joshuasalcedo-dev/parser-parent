package io.joshuasalcedo.parser.java.model;



/**
 * Code pattern for searching
 */
public abstract class CodePattern {
    private final String name;
    private final String description;
    
    public CodePattern(String name, String description) {
        this.name = name;
        this.description = description;
    }
    
    public String getName() { return name; }
    public String getDescription() { return description; }
    
    public abstract boolean matches(ClassRepresentation cls);
    public abstract boolean matchesMethod(MethodRepresentation method);
    
    // Predefined patterns
    public static class SingletonPattern extends CodePattern {
        public SingletonPattern() {
            super("Singleton", "Classes implementing singleton pattern");
        }
        
        @Override
        public boolean matches(ClassRepresentation cls) {
            // Check for private constructor and static instance
            boolean hasPrivateConstructor = cls.getConstructors().stream()
                .anyMatch(c -> "private".equals(c.getVisibility()));
            boolean hasStaticInstance = cls.getFields().stream()
                .anyMatch(f -> f.isStatic() && f.getType().equals(cls.getName()));
            return hasPrivateConstructor && hasStaticInstance;
        }
        
        @Override
        public boolean matchesMethod(MethodRepresentation method) {
            return false;
        }
    }
    
    public static class GetterSetterPattern extends CodePattern {
        public GetterSetterPattern() {
            super("GetterSetter", "Getter and setter methods");
        }
        
        @Override
        public boolean matches(ClassRepresentation cls) {
            return false;
        }
        
        @Override
        public boolean matchesMethod(MethodRepresentation method) {
            String name = method.getName();
            return (name.startsWith("get") || name.startsWith("set") || name.startsWith("is")) &&
                   method.getParameters().size() <= 1;
        }
    }
    
    public static class TestClassPattern extends CodePattern {
        public TestClassPattern() {
            super("TestClass", "Test classes");
        }
        
        @Override
        public boolean matches(ClassRepresentation cls) {
            return cls.getName().endsWith("Test") || 
                   cls.getName().endsWith("Tests") ||
                   cls.getAnnotations().stream().anyMatch(a -> a.contains("Test"));
        }
        
        @Override
        public boolean matchesMethod(MethodRepresentation method) {
            return method.getAnnotations().stream().anyMatch(a -> a.equals("Test"));
        }
    }
}

