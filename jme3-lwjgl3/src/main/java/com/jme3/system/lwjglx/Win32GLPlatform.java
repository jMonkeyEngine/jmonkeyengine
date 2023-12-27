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

import static org.lwjgl.system.jawt.JAWTFunctions.*;

import org.lwjgl.opengl.awt.PlatformWin32GLCanvas;
import org.lwjgl.system.jawt.JAWTDrawingSurfaceInfo;
import org.lwjgl.system.jawt.JAWTWin32DrawingSurfaceInfo;


/**
 * Class <code>Win32GLPlatform</code>; overrides the following methods: <code>swapBuffers()</code> 
 * ,<code>makeCurrent(long context)</code> and <code>delayBeforeSwapNV(float seconds)</code>. 
 * So that the canvas can be removed and added back from its parent component.
 * <p>
 * Only for windows.
 * 
 * @author wil
 */
public class Win32GLPlatform extends PlatformWin32GLCanvas implements LwjglxGLPlatform {
    
    /**
     * (non-JavaDoc)
     * @see org.lwjgl.opengl.awt.PlatformGLCanvas#swapBuffers() 
     * @return boolean
     */
    @Override
    public boolean swapBuffers() {
        // Get the drawing surface info
        JAWTDrawingSurfaceInfo dsi = JAWT_DrawingSurface_GetDrawingSurfaceInfo(ds, ds.GetDrawingSurfaceInfo());
        if (dsi == null) {
            throw new IllegalStateException("JAWT_DrawingSurface_GetDrawingSurfaceInfo() failed");
        }
        
        try {
            // Get the platform-specific drawing info
            JAWTWin32DrawingSurfaceInfo dsiWin = JAWTWin32DrawingSurfaceInfo.create(dsi.platformInfo());
            hwnd = dsiWin.hwnd();            
            return super.swapBuffers();
        } finally {
            // Free the drawing surface info
            JAWT_DrawingSurface_FreeDrawingSurfaceInfo(dsi, ds.FreeDrawingSurfaceInfo());
        }
    }

    /**
     * (non-JavaDoc)
     * @see org.lwjgl.opengl.awt.PlatformGLCanvas#makeCurrent(long) 
     * 
     * @param context long
     * @return boolean
     */
    @Override
    public boolean makeCurrent(long context) {
        // Get the drawing surface info
        JAWTDrawingSurfaceInfo dsi = JAWT_DrawingSurface_GetDrawingSurfaceInfo(ds, ds.GetDrawingSurfaceInfo());
        if (dsi == null) {
            throw new IllegalStateException("JAWT_DrawingSurface_GetDrawingSurfaceInfo() failed");
        }
        
        try {
            // Get the platform-specific drawing info
            JAWTWin32DrawingSurfaceInfo dsiWin = JAWTWin32DrawingSurfaceInfo.create(dsi.platformInfo());
            hwnd = dsiWin.hwnd();
            return super.makeCurrent(context);
        } finally {
            // Free the drawing surface info
            JAWT_DrawingSurface_FreeDrawingSurfaceInfo(dsi, ds.FreeDrawingSurfaceInfo());
        }
    }

    /**
     * (non-JavaDoc)
     * @see org.lwjgl.opengl.awt.PlatformGLCanvas#delayBeforeSwapNV(float) 
     * 
     * @param seconds float
     * @return boolean
     */
    @Override
    public boolean delayBeforeSwapNV(float seconds) {
        // Get the drawing surface info
        JAWTDrawingSurfaceInfo dsi = JAWT_DrawingSurface_GetDrawingSurfaceInfo(ds, ds.GetDrawingSurfaceInfo());
        if (dsi == null) {
            throw new IllegalStateException("JAWT_DrawingSurface_GetDrawingSurfaceInfo() failed");
        }
        
        try {
            // Get the platform-specific drawing info
            JAWTWin32DrawingSurfaceInfo dsiWin = JAWTWin32DrawingSurfaceInfo.create(dsi.platformInfo());
            hwnd = dsiWin.hwnd();
            return super.delayBeforeSwapNV(seconds);
        } finally {
            // Free the drawing surface info
            JAWT_DrawingSurface_FreeDrawingSurfaceInfo(dsi, ds.FreeDrawingSurfaceInfo());
        }
    }
    
    /**
     * (non-JavaDoc)
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
