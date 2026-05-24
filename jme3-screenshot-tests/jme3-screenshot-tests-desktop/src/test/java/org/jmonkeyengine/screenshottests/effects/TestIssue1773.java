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
package org.jmonkeyengine.screenshottests.effects;

import org.jmonkeyengine.screenshottests.scenarios.effects.ScenarioIssue1773;
import org.jmonkeyengine.screenshottests.testframework.desktop.DesktopRunner;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Richard Tingle (aka richtea)
 */
public class TestIssue1773 {

    /**
     * Test case for Issue 1773 (Wrong particle position when using
     * 'EmitterMeshVertexShape' or 'EmitterMeshFaceShape' and worldSpace
     * flag equal to true)
     *
     * If the test succeeds, the particles will be generated from the vertices
     * (for EmitterMeshVertexShape) or from the faces (for EmitterMeshFaceShape)
     * of the torus mesh. If the test fails, the particles will appear in the
     * center of the torus when worldSpace flag is set to true.
     *
     */
    @ParameterizedTest(name = "Test Issue 1773 (emit in worldSpace = {0})")
    @ValueSource(booleans = {true, false})
    public void testIssue1773(boolean worldSpace, TestInfo testInfo){

        String imageName = testInfo.getTestClass().get().getName() + "." + testInfo.getTestMethod().get().getName() + (worldSpace ? "_worldSpace" : "_localSpace");

        ScenarioIssue1773.testIssue1773(worldSpace)
                .setBaseImageFileName(imageName)
                .run(new DesktopRunner());
    }
}
