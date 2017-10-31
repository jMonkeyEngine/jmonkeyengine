package com.jme3.shader.glsl.parser.ast.util;

/**
 * The predicate interface.
 *
 * @author JavaSaBr
 */
public interface CharPredicate {

    /**
     * Tests the value.
     *
     * @param value the value.
     * @return true if test was successful.
     */
    boolean test(char value);
}
