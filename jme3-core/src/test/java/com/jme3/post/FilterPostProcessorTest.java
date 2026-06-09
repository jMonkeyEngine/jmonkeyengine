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
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package com.jme3.post;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.system.NullRenderer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FilterPostProcessorTest {

    @Test
    void initializesFromViewportRenderTargetSize() {
        RenderManager renderManager = new RenderManager(new NullRenderer());
        Camera camera = new Camera(320, 240);
        ViewPort viewPort = renderManager.createMainView("main", camera);
        renderManager.notifyReshape(320, 240, 640, 480);

        RecordingFilter filter = new RecordingFilter();
        FilterPostProcessor processor = new FilterPostProcessor(null);
        processor.addFilter(filter);
        viewPort.addProcessor(processor);

        renderManager.notifyReshape(320, 240, 640, 480);

        assertEquals(640, filter.width);
        assertEquals(480, filter.height);

        renderManager.notifyReshape(320, 240, 800, 600);

        assertEquals(800, filter.width);
        assertEquals(600, filter.height);
        assertEquals(1, filter.cleanupCount);
    }

    private static class RecordingFilter extends Filter {
        private int width;
        private int height;
        private int cleanupCount;

        @Override
        protected void initFilter(AssetManager manager, RenderManager renderManager, ViewPort vp, int w, int h) {
            width = w;
            height = h;
        }

        @Override
        protected Material getMaterial() {
            return null;
        }

        @Override
        protected void cleanUpFilter(Renderer r) {
            cleanupCount++;
        }
    }
}
