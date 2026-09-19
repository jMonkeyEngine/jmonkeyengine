/*
 * Copyright (c) 2009-2026 jMonkeyEngine
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
 * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.shadow;

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.renderer.Camera;
import com.jme3.renderer.Caps;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.system.NullRenderer;
import com.jme3.texture.Texture.ShadowCompareMode;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowCompareModeFallbackTest {

    @Test
    void fallsBackToSoftwareWhenHardwareComparisonIsUnavailable() {
        DirectionalLightShadowRenderer shadows = newShadowRenderer();

        shadows.initialize(renderManager(EnumSet.noneOf(Caps.class)), viewPort());

        assertSoftwareMode(shadows);
    }

    @Test
    void retainsHardwareModeWhenComparisonIsSupported() {
        DirectionalLightShadowRenderer shadows = newShadowRenderer();

        shadows.initialize(renderManager(EnumSet.of(Caps.TextureShadowCompare)), viewPort());

        assertEquals(CompareMode.Hardware, shadows.getShadowCompareMode());
        assertEquals(ShadowCompareMode.LessOrEqual, shadows.shadowMaps[0].getShadowCompareMode());
        assertTrue((Boolean) shadows.postshadowMat.getParam("HardwareShadows").getValue());
    }

    @Test
    void rejectsUnsupportedHardwareModeAfterInitialization() {
        DirectionalLightShadowRenderer shadows = newShadowRenderer();
        shadows.initialize(renderManager(EnumSet.noneOf(Caps.class)), viewPort());

        shadows.setShadowCompareMode(CompareMode.Hardware);

        assertSoftwareMode(shadows);
    }

    @SuppressWarnings("deprecation")
    @Test
    void legacyPssmRendererAlsoFallsBackToSoftware() {
        AssetManager assetManager = new DesktopAssetManager(true);
        PssmShadowRenderer shadows = new PssmShadowRenderer(assetManager, 128, 1);

        shadows.initialize(renderManager(EnumSet.noneOf(Caps.class)), viewPort());

        assertEquals(PssmShadowRenderer.CompareMode.Software, shadows.compareMode);
        assertEquals(ShadowCompareMode.Off, shadows.shadowMaps[0].getShadowCompareMode());
        assertFalse((Boolean) shadows.postshadowMat.getParam("HardwareShadows").getValue());
    }

    private static DirectionalLightShadowRenderer newShadowRenderer() {
        AssetManager assetManager = new DesktopAssetManager(true);
        return new DirectionalLightShadowRenderer(assetManager, 128, 1);
    }

    private static RenderManager renderManager(EnumSet<Caps> caps) {
        return new RenderManager(new NullRenderer() {
            @Override
            public EnumSet<Caps> getCaps() {
                return caps;
            }
        });
    }

    private static ViewPort viewPort() {
        return new ViewPort("test", new Camera(1, 1));
    }

    private static void assertSoftwareMode(DirectionalLightShadowRenderer shadows) {
        assertEquals(CompareMode.Software, shadows.getShadowCompareMode());
        assertEquals(ShadowCompareMode.Off, shadows.shadowMaps[0].getShadowCompareMode());
        assertFalse((Boolean) shadows.postshadowMat.getParam("HardwareShadows").getValue());
    }
}
