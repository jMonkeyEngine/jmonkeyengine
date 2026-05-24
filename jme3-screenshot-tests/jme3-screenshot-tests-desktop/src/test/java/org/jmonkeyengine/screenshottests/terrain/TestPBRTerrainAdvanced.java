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
package org.jmonkeyengine.screenshottests.terrain;

import org.jmonkeyengine.screenshottests.scenarios.terrain.ScenarioPBRTerrainAdvanced;
import org.jmonkeyengine.screenshottests.testframework.desktop.DesktopRunner;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

/**
 * This test uses 'AdvancedPBRTerrain.j3md' to create a terrain Material with
 * more textures than 'PBRTerrain.j3md' can handle.

 * Upon running the app, the user should see a mountainous, terrain-based
 * landscape with some grassy areas, some snowy areas, and some tiled roads and
 * gravel paths weaving between the valleys. Snow should be slightly
 * shiny/reflective, and marble texture should be even shinier. If you would
 * like to know what each texture is supposed to look like, you can find the
 * textures used for this test case located in jme3-testdata.

 * The MetallicRoughness map stores:
 * <ul>
 * <li> AmbientOcclusion in the Red channel </li>
 * <li> Roughness in the Green channel </li>
 * <li> Metallic in the Blue channel </li>
 * <li> EmissiveIntensity in the Alpha channel </li>
 * </ul>

 * The shaders are still subject to the GLSL max limit of 16 textures, however
 * each TextureArray counts as a single texture, and each TextureArray can store
 * multiple images. For more information on texture arrays see:
 * https://www.khronos.org/opengl/wiki/Array_Texture

 * Uses assets from CC0Textures.com, licensed under CC0 1.0 Universal. For more
 * information on the textures this test case uses, view the license.txt file
 * located in the jme3-testdata directory where these textures are located:
 * jme3-testdata/src/main/resources/Textures/Terrain/PBR

 * @author yaRnMcDonuts - original test
 * @author Richard Tingle (aka richtea) - screenshot test adaptation
 */
@SuppressWarnings("FieldCanBeLocal")
public class TestPBRTerrainAdvanced {

    private static Stream<Arguments> testParameters() {
        return Stream.of(
            Arguments.of("FinalRender", 0),
            Arguments.of("AmbientOcclusion", 4),
            Arguments.of("Emissive", 5)
        );
    }

    /**
     * Test advanced PBR terrain with different debug modes
     * 
     * @param testName The name of the test (used for screenshot filename)
     * @param debugMode The debug mode to use
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("testParameters")
    public void testPBRTerrainAdvanced(String testName, int debugMode, TestInfo testInfo) {
        if(!testInfo.getTestClass().isPresent() || !testInfo.getTestMethod().isPresent()) {
            throw new RuntimeException("Test preconditions not met");
        }

        String imageName = testInfo.getTestClass().get().getName() + "." + testInfo.getTestMethod().get().getName() + "_" + testName;

        ScenarioPBRTerrainAdvanced.testPBRTerrainAdvanced(debugMode)
                .setBaseImageFileName(imageName)
                .run(new DesktopRunner());
    }
}