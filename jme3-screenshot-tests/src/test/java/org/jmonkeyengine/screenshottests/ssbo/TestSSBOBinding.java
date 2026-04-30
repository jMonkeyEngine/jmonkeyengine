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

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.shader.bufferobject.BufferObject;
import com.jme3.util.BufferUtils;
import org.jmonkeyengine.screenshottests.testframework.ScreenshotTestBase;
import org.jmonkeyengine.screenshottests.testframework.TestType;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.util.stream.Stream;

/**
 * Tests that SSBO binding points are correctly resolved when buffers are
 * set via the material system. Each test variant uses a different combination
 * of layout(binding=N) qualifiers in the fragment shader.
 *
 * <p>Three SSBOs are created, each containing a vec4 color:
 * <ul>
 *   <li>RedBlock: (1, 0, 0, 0)</li>
 *   <li>GreenBlock: (0, 1, 0, 0)</li>
 *   <li>BlueBlock: (0, 0, 1, 0)</li>
 * </ul>
 * The shader reads redColor.r, greenColor.g, blueColor.b and outputs them
 * as a single color. If all bindings are correct, the result is white.
 */
@SuppressWarnings("OptionalGetWithoutIsPresent")
public class TestSSBOBinding extends ScreenshotTestBase {

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
        String imageName = testInfo.getTestClass().get().getName() + "."
                + testInfo.getTestMethod().get().getName() + "_" + testName;

        screenshotTest(new BaseAppState() {
            @Override
            protected void initialize(Application app) {
                SimpleApplication simpleApp = (SimpleApplication) app;

                simpleApp.getCamera().setLocation(new Vector3f(0, 0, 1));
                simpleApp.getCamera().lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
                simpleApp.getViewPort().setBackgroundColor(ColorRGBA.Black);

                Material mat = new Material(simpleApp.getAssetManager(), matDefPath);

                mat.setShaderStorageBufferObject("RedBlock", createColorBuffer(1f, 0f, 0f, 0f));
                mat.setShaderStorageBufferObject("GreenBlock", createColorBuffer(0f, 1f, 0f, 0f));
                mat.setShaderStorageBufferObject("BlueBlock", createColorBuffer(0f, 0f, 1f, 0f));

                Geometry quad = new Geometry("FullScreenQuad", new Quad(2, 2));
                quad.setLocalTranslation(-1, -1, 0);
                quad.setMaterial(mat);
                simpleApp.getRootNode().attachChild(quad);
            }

            @Override
            protected void cleanup(Application app) {}

            @Override
            protected void onEnable() {}

            @Override
            protected void onDisable() {}
        })
        .setBaseImageFileName(imageName)
        .setTestType(testType)
        .setFramesToTakeScreenshotsOn(1)
        .run();
    }

    private static BufferObject createColorBuffer(float r, float g, float b, float a) {
        BufferObject bo = new BufferObject();
        ByteBuffer buf = BufferUtils.createByteBuffer(16); // vec4 = 4 floats
        buf.putFloat(r).putFloat(g).putFloat(b).putFloat(a);
        buf.flip();
        bo.setData(buf);
        return bo;
    }
}
