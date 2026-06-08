/*
 * Copyright (c) 2026 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
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
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jmonkeyengine.screenshottests.texture;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;

import org.jmonkeyengine.screenshottests.testframework.TestResolution;
import org.jmonkeyengine.screenshottests.testframework.desktop.DesktopRunner;
import org.jmonkeyengine.screenshottests.testframework.desktop.ScreenshotTestDesktopBase;
import org.junit.jupiter.api.Test;

/**
 * Screenshot test for single-channel PNG loading.
 */
public class TestSingleChannelTexture extends ScreenshotTestDesktopBase {

    private static final int TEXTURE_SIZE = 251;

    @Test
    public void testSingleChannelTexture() {
        screenshotTest(new BaseAppState() {
            @Override
            protected void initialize(Application app) {
                SimpleApplication simpleApplication = (SimpleApplication) app;

                attachPicture(simpleApplication, "single-channel-r16",
                        "Textures/singleChannel/R16.png", 0f);
                attachPicture(simpleApplication, "single-channel-r8",
                        "Textures/singleChannel/R8.png", TEXTURE_SIZE);
            }

            private void attachPicture(SimpleApplication app, String name, String texturePath, float x) {
                Texture texture = app.getAssetManager().loadTexture(texturePath);
                Picture picture = new Picture(name);
                picture.setTexture(app.getAssetManager(), (Texture2D) texture, false);
                picture.setPosition(x, 0f);
                picture.setWidth(TEXTURE_SIZE);
                picture.setHeight(TEXTURE_SIZE);
                app.getGuiNode().attachChild(picture);
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
        })
        .setTestResolution(new TestResolution(TEXTURE_SIZE * 2, TEXTURE_SIZE))
        .setFramesToTakeScreenshotsOn(1)
        .run(new DesktopRunner());
    }
}
