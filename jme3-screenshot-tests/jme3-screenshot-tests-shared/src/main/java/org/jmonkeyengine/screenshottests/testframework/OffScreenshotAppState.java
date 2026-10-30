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

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.renderer.Renderer;
import com.jme3.system.JmeSystem;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Optional;

public class OffScreenshotAppState extends AbstractAppState{

    private final Texture2D renderTexture;
    private Renderer renderer;
    private final FrameBuffer frameBuffer;
    private Optional<Path> capture = Optional.empty();

    private ByteBuffer outBuf;

    public void takeScreenshot(Path pathToSaveTo) {
        capture = Optional.of(pathToSaveTo);
    }

    public OffScreenshotAppState(Texture2D renderTexture, FrameBuffer frameBuffer) {
        this.renderTexture = renderTexture;
        this.frameBuffer = frameBuffer;
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        renderer = app.getRenderManager().getRenderer();
        outBuf = BufferUtils.createByteBuffer(renderTexture.getImage().getWidth() * renderTexture.getImage().getHeight() * 4);
    }

    @Override
    public void postRender() {
        super.postRender();
        if (capture.isPresent()) {

            renderer.readFrameBuffer(frameBuffer, outBuf);
            try (FileOutputStream fileOutBuf = new FileOutputStream(capture.get().toFile())) {
                JmeSystem.writeImageFile(fileOutBuf, "png",outBuf, renderTexture.getImage().getWidth(), renderTexture.getImage().getHeight());
            }catch (IOException e) {
                throw new RuntimeException(e);
            }
            capture = Optional.empty();
        }
    }
}