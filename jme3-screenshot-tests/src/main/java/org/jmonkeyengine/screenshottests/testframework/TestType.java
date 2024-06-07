package org.jmonkeyengine.screenshottests.testframework;

public enum TestType{
    /**
     * Normal test, if it fails it will fail the step
     */
    MUST_PASS,
    /**
     * Test is likely to fail because it is testing something non-deterministic (e.g. uses random numbers).
     * This will be marked as a warning in the report but the test will not fail.
     */
    NON_DETERMINISTIC,
    /**
     * Test is known to fail, this will be marked as a warning in the report but the test will not fail.
     * It will be marked as a warning whether it passes or fails, this is because if it has started to pass again it should
     * be returned to a normal test with type MUST_PASS.
     */
    KNOWN_TO_FAIL,
}
