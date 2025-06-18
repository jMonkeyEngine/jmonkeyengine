/*
 * Copyright (c) 2014-2025 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.app.state;

import com.jme3.app.Application;
import com.jme3.math.Matrix3f;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.util.SafeArrayList;

import java.util.Arrays;
import java.util.logging.Logger;

import static java.lang.Float.NEGATIVE_INFINITY;
import static java.lang.Float.NaN;
import static java.lang.Float.POSITIVE_INFINITY;

/**
 * An AppState that periodically checks the values of various JME math constants
 * (e.g., `Vector3f.ZERO`, `Quaternion.IDENTITY`) against their known good values.
 * This is useful for detecting accidental modifications or "drift" of these
 * supposedly immutable constants during application runtime.
 * <p>
 * The state can be configured to report discrepancies using asserts,
 * throwing runtime exceptions, or logging severe messages.
 * The set of constants to check is configurable.
 *
 * @author Paul Speed
 */
public class ConstantVerifierState extends BaseAppState {

    private static final Logger log = Logger.getLogger(ConstantVerifierState.class.getName());

    // Note: I've used actual constructed objects for the good values
    //       instead of clone just to better catch cases where the values
    //       might have been corrupted even before the app state was touched. -pspeed
    private static final Checker[] DEFAULT_CHECKS = new Checker[] {
            new Checker(Vector3f.ZERO, new Vector3f(0, 0, 0)),
            new Checker(Vector3f.NAN, new Vector3f(NaN, NaN, NaN)),
            new Checker(Vector3f.UNIT_X, new Vector3f(1, 0, 0)),
            new Checker(Vector3f.UNIT_Y, new Vector3f(0, 1, 0)),
            new Checker(Vector3f.UNIT_Z, new Vector3f(0, 0, 1)),
            new Checker(Vector3f.UNIT_XYZ, new Vector3f(1, 1, 1)),
            new Checker(Vector3f.POSITIVE_INFINITY,
                    new Vector3f(POSITIVE_INFINITY, POSITIVE_INFINITY, POSITIVE_INFINITY)),
            new Checker(Vector3f.NEGATIVE_INFINITY,
                    new Vector3f(NEGATIVE_INFINITY, NEGATIVE_INFINITY, NEGATIVE_INFINITY)),
            new Checker(Quaternion.IDENTITY, new Quaternion()),
            new Checker(Quaternion.DIRECTION_Z,
                    new Quaternion().fromAxes(Vector3f.UNIT_X, Vector3f.UNIT_Y, Vector3f.UNIT_Z)),
            new Checker(Quaternion.ZERO, new Quaternion(0, 0, 0, 0)),
            new Checker(Vector2f.ZERO, new Vector2f(0f, 0f)),
            new Checker(Vector2f.NAN, new Vector2f(NaN, NaN)),
            new Checker(Vector2f.UNIT_X, new Vector2f(1, 0)),
            new Checker(Vector2f.UNIT_Y, new Vector2f(0, 1)),
            new Checker(Vector2f.UNIT_XY, new Vector2f(1f, 1f)),
            new Checker(Vector2f.POSITIVE_INFINITY,
                    new Vector2f(POSITIVE_INFINITY, POSITIVE_INFINITY)),
            new Checker(Vector2f.NEGATIVE_INFINITY,
                    new Vector2f(NEGATIVE_INFINITY, NEGATIVE_INFINITY)),
            new Checker(Vector4f.ZERO, new Vector4f(0, 0, 0, 0)),
            new Checker(Vector4f.NAN, new Vector4f(NaN, NaN, NaN, NaN)),
            new Checker(Vector4f.UNIT_X, new Vector4f(1, 0, 0, 0)),
            new Checker(Vector4f.UNIT_Y, new Vector4f(0, 1, 0, 0)),
            new Checker(Vector4f.UNIT_Z, new Vector4f(0, 0, 1, 0)),
            new Checker(Vector4f.UNIT_W, new Vector4f(0, 0, 0, 1)),
            new Checker(Vector4f.UNIT_XYZW, new Vector4f(1, 1, 1, 1)),
            new Checker(Vector4f.POSITIVE_INFINITY,
                    new Vector4f(POSITIVE_INFINITY, POSITIVE_INFINITY, POSITIVE_INFINITY, POSITIVE_INFINITY)),
            new Checker(Vector4f.NEGATIVE_INFINITY,
                    new Vector4f(NEGATIVE_INFINITY, NEGATIVE_INFINITY, NEGATIVE_INFINITY, NEGATIVE_INFINITY)),
            new Checker(Matrix3f.ZERO, new Matrix3f(0, 0, 0, 0, 0, 0, 0, 0, 0)),
            new Checker(Matrix3f.IDENTITY, new Matrix3f()),
            new Checker(Matrix4f.ZERO, new Matrix4f(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)),
            new Checker(Matrix4f.IDENTITY, new Matrix4f())
        };

    /**
     * Defines how constant value discrepancies should be reported.
     */
    public enum ErrorType {
        /** Causes an `assert` failure if the constant has changed. Requires assertions to be enabled. */
        Assert,
        /** Throws a `RuntimeException` if the constant has changed. */
        Exception,
        /** Logs a severe message if the constant has changed. */
        Log
    }

    private final SafeArrayList<Checker> checkers = new SafeArrayList<>(Checker.class);
    private ErrorType errorType;

    /**
     * Creates a verifier app state that will check all of the default
     * JME math constants using `ErrorType.Assert`.
     */
    public ConstantVerifierState() {
        this(ErrorType.Assert);
    }

    /**
     * Creates a verifier app state that will check all of the default
     * JME math constants using the specified error reporting mechanism.
     *
     * @param errorType The mechanism to use when a constant's value drifts.
     */
    public ConstantVerifierState(ErrorType errorType) {
        this(errorType, DEFAULT_CHECKS);
    }

    /**
     *  Creates a verifier app state that will check all of the specified
     *  checks and report errors using the specified error type.
     *
     * @param errorType the mechanism to use
     * @param checkers which checks to perform
     */
    private ConstantVerifierState(ErrorType errorType, Checker... checkers) {
        this.errorType = errorType;
        this.checkers.addAll(Arrays.asList(checkers));
    }

    /**
     * Adds a new constant and its expected good value to the list of items to be checked.
     * The `constant` and `goodValue` should be instances of the same class.
     *
     * @param constant The JME constant object to monitor for drift (e.g., `Vector3f.ZERO`).
     * @param goodValue An independent instance representing the expected correct value of the constant.
     * This instance should match the initial value of `constant`.
     */
    public void addChecker(Object constant, Object goodValue) {
        checkers.add(new Checker(constant, goodValue));
    }

    /**
     * Sets the error reporting mechanism to be used when a constant's value drifts.
     *
     * @param errorType The desired error reporting type.
     */
    public void setErrorType(ErrorType errorType) {
        this.errorType = errorType;
    }

    /**
     * Returns the currently configured error reporting mechanism.
     *
     * @return The current `ErrorType`.
     */
    public ErrorType getErrorType() {
        return errorType;
    }

    @Override
    protected void initialize(Application app) {
    }

    @Override
    protected void cleanup(Application app) {
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
    }

    @Override
    public void postRender() {
        // Check as late in the frame as possible.  Subclasses can check earlier
        // if they like.
        checkValues();
    }

    /**
     * Iterates through all registered checkers and verifies the current values
     * of the constants against their known good values.
     * Reports any discrepancies based on the configured `ErrorType`.
     */
    protected void checkValues() {
        for (Checker checker : checkers.getArray()) {
            switch (errorType) {
                default: // Fall through to Assert if somehow null
                case Assert:
                    assert checker.isValid() : checker.toString();
                    break;
                case Exception:
                    if (!checker.isValid()) {
                        throw new RuntimeException("JME Constant has changed, " + checker.toString());
                    }
                    break;
                case Log:
                    if (!checker.isValid()) {
                        log.severe("JME Constant has changed, " + checker.toString());
                    }
                    break;
            }
        }
    }

    /**
     *  Checks the specified 'constant' value against its known good
     *  value.  These should obviously be different instances for this to
     *  mean anything.
     */
    private static class Checker {

        private final Object constant;
        private final Object goodValue;

        public Checker(Object constant, Object goodValue) {
            if (constant == null) {
                throw new IllegalArgumentException("Constant cannot be null");
            }
            if (!constant.equals(goodValue)) {
                throw new IllegalArgumentException(
                        "Constant value: " + constant + " does not match value: " + goodValue);
            }
            this.constant = constant;
            this.goodValue = goodValue;
        }

        public boolean isValid() {
            return constant.equals(goodValue);
        }

        @Override
        public String toString() {
            return "Constant:" + constant + ", correct value:" + goodValue + ", type:" + goodValue.getClass();
        }
    }
}
