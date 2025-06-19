package io.joshuasalcedo.parser.java.util;

import java.util.Set;

public class JavaParseUtil {
    public static  boolean isPrimitiveType(String type) {
        return Set.of("int", "long", "double", "float", "boolean", "char", "byte", "short", "void")
                .contains(type);
    }
}
