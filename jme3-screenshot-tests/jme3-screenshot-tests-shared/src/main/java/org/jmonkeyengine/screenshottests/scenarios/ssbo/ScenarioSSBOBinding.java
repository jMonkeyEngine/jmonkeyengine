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
package org.jmonkeyengine.screenshottests.scenarios.ssbo;

import static org.jmonkeyengine.screenshottests.testframework.ScreenshotTestBase.screenshotTest;

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
import java.nio.ByteBuffer;
import org.jmonkeyengine.screenshottests.testframework.ScreenshotTest;

/**
 * Tests SSBO binding point resolution with different layout(binding=N) combinations.
 */
public final class ScenarioSSBOBinding {

    private ScenarioSSBOBinding() {
    }

    public static ScreenshotTest testSSBOBinding(String matDefPath) {
        return screenshotTest(new BaseAppState() {
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
            protected void cleanup(Application app) {
            }

            @Override
            protected void onEnable() {
            }

            @Override
            protected void onDisable() {
            }
        }).setFramesToTakeScreenshotsOn(1);
    }

    private static BufferObject createColorBuffer(float r, float g, float b, float a) {
        BufferObject bo = new BufferObject();
        ByteBuffer buf = BufferUtils.createByteBuffer(16);
        buf.putFloat(r).putFloat(g).putFloat(b).putFloat(a);
        buf.flip();
        bo.setData(buf);
        return bo;
    }
}
