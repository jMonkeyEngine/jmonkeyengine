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
package com.jme3.app;

import com.jme3.system.AppSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IosApplicationLauncherTest {

    @Test
    void configuresLowDensityRgb565Framebuffer() throws Exception {
        AppSettings settings = new AppSettings(true);
        settings.setDisplayScaleMode(AppSettings.DISPLAY_SCALE_DISABLED);
        settings.setGammaCorrection(false);
        settings.setBitsPerPixel(16);
        settings.setAlphaBits(4);
        settings.setDepthBits(16);
        settings.setStencilBits(8);
        settings.setSamples(4);

        TestLauncher launcher = new TestLauncher(settings);
        launcher.configure();

        assertFalse(launcher.highPixelDensity);
        assertArrayEquals(new int[]{5, 6, 5, 4, 16, 8, 4}, launcher.framebufferConfig);
        assertEquals(1, launcher.createCount);
        assertSame(settings, launcher.app.getSettings());
    }

    @Test
    void gammaCorrectionRequiresRgb888AndDpiAwareDrawable() throws Exception {
        AppSettings settings = new AppSettings(true);
        settings.setDisplayScaleMode(AppSettings.DISPLAY_SCALE_DPI_AWARE);
        settings.setGammaCorrection(true);
        settings.setBitsPerPixel(16);

        TestLauncher launcher = new TestLauncher(settings);
        launcher.configure();

        assertTrue(launcher.highPixelDensity);
        assertArrayEquals(new int[]{8, 8, 8, 0, 24, 0, 0}, launcher.framebufferConfig);
    }

    private static final class TestLauncher extends IosApplicationLauncher {
        private final AppSettings applicationSettings;
        private int createCount;
        private boolean highPixelDensity;
        private int[] framebufferConfig;

        private TestLauncher(AppSettings applicationSettings) {
            this.applicationSettings = applicationSettings;
        }

        @Override
        protected Application createApplication() {
            createCount++;
            LegacyApplication application = new LegacyApplication();
            application.setSettings(applicationSettings);
            return application;
        }

        @Override
        protected void configureWindow(boolean highPixelDensity) {
            this.highPixelDensity = highPixelDensity;
        }

        @Override
        protected void configureDefaultFramebuffer(int redBits, int greenBits, int blueBits,
                int alphaBits, int depthBits, int stencilBits, int samples) {
            framebufferConfig = new int[]{
                    redBits, greenBits, blueBits, alphaBits, depthBits, stencilBits, samples
            };
        }
    }
}
