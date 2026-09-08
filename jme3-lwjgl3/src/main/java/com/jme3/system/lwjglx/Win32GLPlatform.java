/*
 * Copyright (c) 2009-2023 jMonkeyEngine
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
package com.jme3.system.lwjglx;

import static org.lwjgl.opengl.WGL.wglGetCurrentContext;
import static org.lwjgl.opengl.WGL.wglGetCurrentDC;
import static org.lwjgl.opengl.WGL.wglGetProcAddress;
import static org.lwjgl.opengl.WGL.wglMakeCurrent;
import static org.lwjgl.system.JNI.callI;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.jawt.JAWTFunctions.JAWT_FreeDrawingSurface;

import java.awt.AWTException;
import java.awt.Canvas;

import org.lwjgl.opengl.awt.GLData;
import org.lwjgl.opengl.awt.PlatformWin32GLCanvas;

/**
 * <code>Win32GLPlatform</code> class that implements the {@link com.jme3.system.lwjglx.LwjglxGLPlatform} 
 * interface for the Windows (Win32) platform.
 * 
 * @author wil
 */
final class Win32GLPlatform extends PlatformWin32GLCanvas implements LwjglxGLPlatform {

    @Override
    public long create(Canvas canvas, GLData data, GLData effective) throws AWTException {
        effective.swapInterval = null;
        Integer requestedSwapInterval = data.swapInterval;
        long context;
        try {
            // The dependency treats an unavailable WGL swap-control extension
            // as a fatal context-creation error. Create first, then apply VSync
            // here so jME can fall back to EDT fairness instead.
            data.swapInterval = null;
            context = super.create(canvas, data, effective);
        } finally {
            data.swapInterval = requestedSwapInterval;
        }

        if (requestedSwapInterval == null) {
            return context;
        }

        long previousContext = wglGetCurrentContext(null);
        long previousDc = wglGetCurrentDC();
        try {
            if (!makeCurrent(context)) {
                return context;
            }

            long setAddress = wglGetProcAddress(null, "wglSwapIntervalEXT");
            if (setAddress == NULL || callI(requestedSwapInterval, setAddress) == 0) {
                return context;
            }

            long getAddress = wglGetProcAddress(null, "wglGetSwapIntervalEXT");
            if (getAddress != NULL && callI(getAddress) == requestedSwapInterval) {
                effective.swapInterval = requestedSwapInterval;
            }
        } finally {
            wglMakeCurrent(null, previousDc, previousContext);
        }
        return context;
    }

    /* (non-Javadoc)
     * @see com.jme3.system.lwjglx.LwjglxGLPlatform#dispose()
     */
    @Override
    public void dispose() {
        if (ds != null) {
            super.dispose();
        }
    }

    /* (non-Javadoc)
     * @see com.jme3.system.lwjglx.LwjglxGLPlatform#destroy() 
     */
    @Override
    public void destroy() {
        if (ds != null) {
            JAWT_FreeDrawingSurface(ds, awt.FreeDrawingSurface());
            awt.free();
        }
    }
}
