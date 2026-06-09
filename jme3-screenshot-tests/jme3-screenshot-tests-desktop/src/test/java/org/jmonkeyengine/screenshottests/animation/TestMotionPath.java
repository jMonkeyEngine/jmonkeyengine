/*
 * Copyright (c) 2024 jMonkeyEngine
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
package org.jmonkeyengine.screenshottests.animation;

import org.jmonkeyengine.screenshottests.scenarios.animation.ScenarioMotionPath;
import org.jmonkeyengine.screenshottests.testframework.desktop.DesktopRunner;
import org.jmonkeyengine.screenshottests.testframework.desktop.ScreenshotTestDesktopBase;
import org.junit.jupiter.api.Test;

/**
 * Screenshot test for the MotionPath functionality.
 * 
 * <p>This test creates a teapot model that follows a predefined path with several waypoints.
 * The animation is automatically started and screenshots are taken at frames 10 and 60
 * to capture the teapot at different positions along the path.
 *
 * @author Richard Tingle (screenshot test adaptation)
 */
public class TestMotionPath extends ScreenshotTestDesktopBase {

    /**
     * This test creates a scene with a teapot following a motion path.
     */
    @Test
    public void testMotionPath() {
        ScenarioMotionPath.testMotionPath().run(new DesktopRunner());
    }
}