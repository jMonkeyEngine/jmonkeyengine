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
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.renderer;

import com.jme3.system.NullRenderer;
import com.jme3.texture.FrameBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RenderManagerViewPortSizeTest {

    @Test
    void viewsCreatedBeforeFirstReshapeFallBackToCameraSize() {
        RenderManager renderManager = new RenderManager(new NullRenderer());

        assertTargetSize(renderManager.createPreView("pre", new Camera(320, 240)), 320, 240);
        assertTargetSize(renderManager.createMainView("main", new Camera(400, 300)), 400, 300);
        assertTargetSize(renderManager.createPostView("post", new Camera(500, 400)), 500, 400);
    }

    @Test
    void viewsCreatedAfterReshapeInheritDefaultFramebufferSize() {
        RenderManager renderManager = new RenderManager(new NullRenderer());
        renderManager.notifyReshape(320, 240, 640, 480);

        Camera preCamera = new Camera(320, 240);
        Camera mainCamera = new Camera(320, 240);
        Camera postCamera = new Camera(320, 240);

        assertTargetSize(renderManager.createPreView("pre", preCamera), 640, 480);
        assertTargetSize(renderManager.createMainView("main", mainCamera), 640, 480);
        assertTargetSize(renderManager.createPostView("post", postCamera), 640, 480);
        assertEquals(320, mainCamera.getWidth());
        assertEquals(240, mainCamera.getHeight());
    }

    @Test
    void explicitFramebufferOverridesInheritedDefaultFramebufferSize() {
        RenderManager renderManager = new RenderManager(new NullRenderer());
        renderManager.notifyReshape(320, 240, 640, 480);
        ViewPort viewPort = renderManager.createMainView("main", new Camera(320, 240));

        viewPort.setOutputFrameBuffer(new FrameBuffer(256, 128, 1));

        assertTargetSize(viewPort, 256, 128);
    }

    @Test
    void laterReshapeUpdatesExistingViewsAndNewViewDefaults() {
        RenderManager renderManager = new RenderManager(new NullRenderer());
        renderManager.notifyReshape(320, 240, 640, 480);
        ViewPort existingView = renderManager.createMainView("existing", new Camera(320, 240));

        renderManager.notifyReshape(320, 240, 800, 600);
        ViewPort lateView = renderManager.createMainView("late", new Camera(320, 240));

        assertTargetSize(existingView, 800, 600);
        assertTargetSize(lateView, 800, 600);
    }

    private static void assertTargetSize(ViewPort viewPort, int expectedWidth, int expectedHeight) {
        assertEquals(expectedWidth, viewPort.getRenderTargetWidth());
        assertEquals(expectedHeight, viewPort.getRenderTargetHeight());
    }
}
