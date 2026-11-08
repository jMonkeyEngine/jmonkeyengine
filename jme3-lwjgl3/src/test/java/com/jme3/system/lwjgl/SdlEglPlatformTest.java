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
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.system.lwjgl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.lwjgl.egl.EXTPlatformWayland.EGL_PLATFORM_WAYLAND_EXT;
import static org.lwjgl.egl.EXTPlatformX11.EGL_PLATFORM_X11_EXT;
import static org.lwjgl.sdl.SDLVideo.SDL_GL_EGL_PLATFORM;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;
import java.lang.reflect.Method;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.system.Platform;
import org.mockito.MockedStatic;

class SdlEglPlatformTest {
    @ParameterizedTest
    @CsvSource({"true, true, x11", "true, false, wayland", "false, true, x11", "false, false, wayland"})
    void matchesEglPlatformToTheSelectedVideoBackend(
            boolean preferX11, boolean waylandSession, String selectedDriver) throws Exception {
        assumeTrue(Platform.get() == Platform.LINUX);
        AppSettings settings = new AppSettings(true);
        settings.setRenderer(AppSettings.LWJGL_OPENGL33);
        settings.setX11PlatformPreferred(preferX11);
        LwjglWindow window = new LwjglDisplay();
        Method configure = LwjglWindow.class.getDeclaredMethod("configureGLAttributes", AppSettings.class);
        configure.setAccessible(true);
        try (MockedStatic<SDLVideo> video = mockStatic(SDLVideo.class);
                MockedStatic<JmeSystem> system = mockStatic(JmeSystem.class)) {
            int[] requestedPlatform = {0};
            video.when(SDLVideo::SDL_GetCurrentVideoDriver).thenReturn(selectedDriver);
            video.when(() -> SDLVideo.SDL_GL_SetAttribute(eq(SDL_GL_EGL_PLATFORM), anyInt()))
                    .thenAnswer(invocation -> {
                        requestedPlatform[0] = invocation.getArgument(1);
                        return true;
                    });
            system.when(JmeSystem::isWaylandSession).thenReturn(waylandSession);
            configure.invoke(window, settings);
            int backendPlatform = "x11".equals(selectedDriver)
                    ? EGL_PLATFORM_X11_EXT : EGL_PLATFORM_WAYLAND_EXT;
            // Bundled SDL supplies its video backend's platform when the attribute remains zero.
            int effectivePlatform = requestedPlatform[0] == 0 ? backendPlatform : requestedPlatform[0];
            assertEquals(backendPlatform, effectivePlatform,
                    "The EGL platform must match SDL's selected video backend");
        }
    }
}
