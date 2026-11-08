/*
 * Copyright (c) 2026 jMonkeyEngine
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
package org.jmonkeyengine.screenshottests.testframework;

/**
 * Controls where a screenshot test gets its reference images from, and what it does when there
 * isn't one.
 *
 * <p>
 * Reference images used to be committed to {@code src/main/resources} and loaded from the
 * classpath. Because a reference image is really a recording of how one particular machine
 * rasterises a scene, that made the tests sensitive to the hardware CI happened to run on. The
 * replacement is to generate the reference images fresh, on the same runner, from a reference
 * branch ({@link #GENERATE}) and then compare the branch under test against them
 * ({@link #COMPARE}).
 * </p>
 *
 * @author Richard Tingle (aka richtea)
 */
public enum ScreenshotTestMode {

    /**
     * Writes the screenshot of every frame as a reference image and compares nothing against a
     * reference image, so the test cannot fail on how the scene looks. This is the pass that runs
     * against the reference branch.
     *
     * <p>
     * Tests can still fail in this mode, and a failure here must abort the run before the
     * comparison pass starts: an application crash, a timeout or a disagreement between the
     * scenarios of a multi-scenario test all mean the reference images are not trustworthy.
     * </p>
     */
    GENERATE("generate"),

    /**
     * Compares against the reference images produced by an earlier {@link #GENERATE} run. A
     * reference image that is missing means the test did not exist on the reference branch, so it
     * is a new test: it is reported as a warning rather than failed.
     */
    COMPARE("compare");

    private final String configurationValue;

    ScreenshotTestMode(String configurationValue) {
        this.configurationValue = configurationValue;
    }

    /**
     * Returns the value used to select this mode on the command line, e.g. {@code generate}.
     *
     * @return the configuration value for this mode
     */
    public String getConfigurationValue() {
        return configurationValue;
    }

    public static final String HOW_TO_SELECT_A_MODE =
            "A screenshot test run has to say what it is doing, because a screenshot is only"
            + " meaningful next to a reference image taken on the same machine:\n"
            + "  -Pscreenshot.mode=generate  render the reference branch and record what it looks"
            + " like on this machine\n"
            + "  -Pscreenshot.mode=compare   render this branch and compare it against those"
            + " recordings\n"
            + "Reference images are written to and read from build/reference-images unless"
            + " -Pscreenshot.referenceDir=<path> says otherwise. To check a change locally, run"
            + " generate on the branch you are comparing against, then compare on yours.\n"
            + "See jme3-screenshot-tests/README.md";

    public static ScreenshotTestMode fromConfigurationValue(String configurationValue) {
        if (configurationValue == null || configurationValue.trim().isEmpty()) {
            throw new IllegalArgumentException("No screenshot test mode was selected.\n"
                    + HOW_TO_SELECT_A_MODE);
        }

        String normalised = configurationValue.trim();
        for (ScreenshotTestMode mode : values()) {
            if (mode.configurationValue.equalsIgnoreCase(normalised)) {
                return mode;
            }
        }

        throw new IllegalArgumentException("'" + configurationValue
                + "' is not a screenshot test mode.\n" + HOW_TO_SELECT_A_MODE);
    }
}
