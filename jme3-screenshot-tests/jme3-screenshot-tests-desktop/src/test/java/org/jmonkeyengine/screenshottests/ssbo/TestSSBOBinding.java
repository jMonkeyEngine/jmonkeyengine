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
package org.jmonkeyengine.screenshottests.ssbo;

import java.util.stream.Stream;
import org.jmonkeyengine.screenshottests.scenarios.ssbo.ScenarioSSBOBinding;
import org.jmonkeyengine.screenshottests.testframework.TestType;
import org.jmonkeyengine.screenshottests.testframework.desktop.DesktopRunner;
import org.jmonkeyengine.screenshottests.testframework.desktop.ScreenshotTestDesktopBase;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests that SSBO binding points are correctly resolved when buffers are set via the material system.
 */
@SuppressWarnings("OptionalGetWithoutIsPresent")
public class TestSSBOBinding extends ScreenshotTestDesktopBase {

    private static Stream<Arguments> testParameters() {
        return Stream.of(
                Arguments.of("NoBindings", "TestSSBOBinding/SSBONoBindings.j3md", TestType.MUST_PASS),
                Arguments.of("ExplicitBindings", "TestSSBOBinding/SSBOExplicitBindings.j3md", TestType.MUST_PASS),
                Arguments.of("MixedBindings", "TestSSBOBinding/SSBOMixedBindings.j3md", TestType.MUST_PASS),
                Arguments.of("Collision", "TestSSBOBinding/SSBOCollision.j3md", TestType.MUST_PASS)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testParameters")
    public void testSSBOBinding(String testName, String matDefPath, TestType testType, TestInfo testInfo) {
        String imageName = testInfo.getTestClass().get().getName()
                + "." + testInfo.getTestMethod().get().getName() + "_" + testName;

        ScenarioSSBOBinding.testSSBOBinding(matDefPath)
                .setBaseImageFileName(imageName)
                .setTestType(testType)
                .run(new DesktopRunner());
    }
}
