package com.jme3.shader.glsl.parser.ast.util;

/**
 * The predicate interface.
 *
 * @author JavaSaBr
 */
public interface BiPredicate<F, S> {

    /**
     * Tests the values.
     *
     * @param first  the first value.
     * @param second the second value.
     * @return true if test was successful.
     */
    boolean test(F first, S second);
}
