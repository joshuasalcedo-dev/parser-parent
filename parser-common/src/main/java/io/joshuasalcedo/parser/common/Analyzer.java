package io.joshuasalcedo.parser.common;

@FunctionalInterface
public interface Analyzer<T,R> {

//    THE RETURN TYPE SHOULD BE THE DATA MODEL AFTER THE ANALYSIS.
//    THE R,  IS THE DATAMODEL WE WANT TO ANALYZE, THE IMPLEMENTATION CAN BE ANYTHING
    T analyze(R r);
}
