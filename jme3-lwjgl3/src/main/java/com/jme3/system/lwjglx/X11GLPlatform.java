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

import org.lwjgl.opengl.awt.PlatformLinuxGLCanvas;
import static org.lwjgl.system.jawt.JAWTFunctions.*;

/**
 * Class <code>X11GLPlatform</code>; overrides the following methods: <code>swapBuffers()</code> 
 * and <code>makeCurrent(long context)</code>. So that the canvas can be removed and 
 * added back from its parent component.
 * 
 * <p>
 * Works only for <b>Linux</b> based platforms
 * 
 * @author wil
 */
final class X11GLPlatform extends PlatformLinuxGLCanvas implements LwjglxGLPlatform {

    public long getDisplay() {
        return display;
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
