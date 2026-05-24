/*
 * Copyright (c) 2025 jMonkeyEngine
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
package org.jmonkeyengine.screenshottests.model.shape;

import com.jme3.math.Vector3f;
import org.jmonkeyengine.screenshottests.scenarios.model.shape.ScenarioBillboard;
import org.jmonkeyengine.screenshottests.testframework.desktop.DesktopRunner;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

/**
 * Screenshot test for the Billboard test.
 * 
 * <p>This test creates three different billboard alignments (Screen, Camera, AxialY)
 * with different colored quads. Each billboard is positioned at a different x-coordinate
 * and has a blue Z-axis arrow attached to it. Screenshots are taken from three different angles:
 * front, above, and right.
 * 
 * @author Richard Tingle (screenshot test adaptation)
 */
@SuppressWarnings("OptionalGetWithoutIsPresent")
public class TestBillboard {

    private static Stream<Arguments> testParameters() {
        return Stream.of(
                Arguments.of("fromFront", new Vector3f(0, 1, 15)),
                Arguments.of("fromAbove", new Vector3f(0, 15, 6)),
                Arguments.of("fromRight", new Vector3f(-15, 10, 5))
        );
    }

    /**
     * A billboard test with the specified camera parameters.
     *
     * @param cameraPosition The position of the camera
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("testParameters")
    public void testBillboard(String testName, Vector3f cameraPosition, TestInfo testInfo) {
        String imageName = testInfo.getTestClass().get().getName() + "." + testInfo.getTestMethod().get().getName() + "_" + testName;

        ScenarioBillboard.testBillboard(cameraPosition)
                .setBaseImageFileName(imageName)
                .run(new DesktopRunner());
    }
}
