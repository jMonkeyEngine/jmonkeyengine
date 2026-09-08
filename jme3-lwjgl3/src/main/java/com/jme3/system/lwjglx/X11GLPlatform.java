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

import static org.lwjgl.opengl.GLX.glXDestroyContext;
import static org.lwjgl.opengl.GLX11.glXQueryExtensionsString;
import static org.lwjgl.opengl.GLX14.glXGetProcAddress;
import static org.lwjgl.opengl.GLXEXTSwapControl.glXSwapIntervalEXT;
import static org.lwjgl.opengl.GLXSGISwapControl.glXSwapIntervalSGI;
import static org.lwjgl.system.JNI.callI;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.jawt.JAWTFunctions.JAWT_FreeDrawingSurface;

import java.awt.AWTException;
import java.awt.Canvas;

import org.lwjgl.opengl.awt.GLData;
import org.lwjgl.opengl.awt.PlatformLinuxGLCanvas;
import org.lwjgl.system.linux.X11;

/**
 * <code>X11GLPlatform</code> class that implements the {@link com.jme3.system.lwjglx.LwjglxGLPlatform} 
 * interface for the Linux (Based) platform.
 * 
 * @author wil
 */
final class X11GLPlatform extends PlatformLinuxGLCanvas implements LwjglxGLPlatform {

    @Override
    public long create(Canvas canvas, GLData data, GLData effective) throws AWTException {
        effective.swapInterval = null;
        long context = super.create(canvas, data, effective);
        if (data.swapInterval == null) {
            return context;
        }

        try {
            boolean locked = false;
            boolean current = false;
            try {
                lock();
                locked = true;
                current = makeCurrent(context);
                if (current && applySwapInterval(data.swapInterval)) {
                    effective.swapInterval = data.swapInterval;
                }
            } finally {
                if (current) {
                    makeCurrent(NULL);
                }
                if (locked) {
                    unlock();
                }
            }
        } catch (AWTException | RuntimeException exception) {
            try {
                deleteContext(context);
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
        return context;
    }

    private boolean applySwapInterval(int interval) {
        int screen = X11.XDefaultScreen(display);
        String extensionString = glXQueryExtensionsString(display, screen);
        switch (X11SwapIntervalSelector.select(extensionString, interval)) {
            case EXT:
                glXSwapIntervalEXT(display, drawable, interval);
                return true;
            case MESA:
                long address = glXGetProcAddress("glXSwapIntervalMESA");
                return address != NULL && callI(interval, address) == 0;
            case SGI:
                return glXSwapIntervalSGI(interval) == 0;
            default:
                return false;
        }
    }

    /**
     * Returns a pointer to the {@code Display*} of the current X11 window using
     * AWT.
     *
     * @return long
     */
    public long getDisplay() {
        return display;
    }

    /**
     * Delete the previously created context.
     *
     * @param context long
     * @return boolean
     */
    @Override
    public boolean deleteContext(long context) {
        if (context == NULL || display == NULL) {
            return false;
        }

        glXDestroyContext(display, context);
        return true;
    }
    
    /**
     * (non-Javadoc)
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
