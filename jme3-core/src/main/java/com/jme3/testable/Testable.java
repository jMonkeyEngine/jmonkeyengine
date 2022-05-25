package com.jme3.testable;

/**
 * A testable interface.
 *
 * @author pavl_g.
 */
public interface Testable {
    /**
     * Launch the test.
     *
     * @param userData user data object.
     */
    void launch(Object userData);

    /**
     * Tests whether the test is still active.
     *
     * @return true if the test is active, false otherwise.
     */
    boolean isActive();
}