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

import org.lwjgl.system.Platform;
import static org.lwjgl.system.Platform.*;

/**
 * Class <code>wjglxDefaultGLPlatform</code> used to create a drawing platform.
 * @author wil
 */
public final class LwjglxDefaultGLPlatform {
    
    /**
     * Returns a drawing platform based on the platform it is running on.
     * @return LwjglxGLPlatform
     * @throws UnsupportedOperationException throws exception if platform is not supported
     */
    public static LwjglxGLPlatform createLwjglxGLPlatform() throws UnsupportedOperationException {
        switch (Platform.get()) {
            case WINDOWS:
                return new Win32GLPlatform();
            //case FREEBSD:  -> In future versions of lwjgl3 (possibly)
            case LINUX:
                return new X11GLPlatform();
            case MACOSX:
                return new MacOSXGLPlatform();
            default:
                throw new UnsupportedOperationException("Platform " + Platform.get() + " not yet supported");
        }
    }
    
    /**
     * private constructor.
     */
    private LwjglxDefaultGLPlatform() {}
}
