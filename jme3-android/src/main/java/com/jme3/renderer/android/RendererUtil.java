/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
package com.jme3.renderer.android;

import android.opengl.GLES20;
import android.opengl.GLU;
import com.jme3.renderer.RendererException;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGL11;

/**
 * Utility class used by the OGLESShaderRenderer and sister
 * classes.
 *
 * @author Kirill Vainer
 */
public class RendererUtil {

    /**
     * When set to true, every OpenGL call will check for errors and throw an
     * exception if there is one, if false, no error checking is performed.
     */
    public static boolean ENABLE_ERROR_CHECKING = true;

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private RendererUtil() {
    }

    /**
     * Checks for an OpenGL error and throws a {@link RendererException} if
     * there is one. Ignores the value of
     * {@link RendererUtil#ENABLE_ERROR_CHECKING}.
     */
    public static void checkGLErrorForced() {
        int error = GLES20.glGetError();
        if (error != 0) {
            String message = GLU.gluErrorString(error);
            if (message == null) {
                throw new RendererException("An unknown OpenGL error has occurred.");
            } else {
                throw new RendererException("An OpenGL error has occurred: " + message);
            }
        }
    }

    /**
     * Checks for an EGL error and throws a {@link RendererException} if there
     * is one. Ignores the value of {@link RendererUtil#ENABLE_ERROR_CHECKING}.
     * 
     * @param egl (not null)
     */
    public static void checkEGLError(EGL10 egl) {
        int error = egl.eglGetError();
        if (error != EGL10.EGL_SUCCESS) {
            String errorMessage;
            switch (error) {
                case EGL10.EGL_NOT_INITIALIZED:
                    errorMessage = "EGL is not initialized, or could not be "
                            + "initialized, for the specified EGL display connection. ";
                    break;
                case EGL10.EGL_BAD_ACCESS:
                    errorMessage = "EGL cannot access a requested resource "
                            + "(for example a context is bound in another thread). ";
                    break;
                case EGL10.EGL_BAD_ALLOC:
                    errorMessage = "EGL failed to allocate resources for the requested operation.";
                    break;
                case EGL10.EGL_BAD_ATTRIBUTE:
                    errorMessage = "An unrecognized attribute or attribute "
                            + "value was passed in the attribute list. ";
                    break;
                case EGL10.EGL_BAD_CONTEXT:
                    errorMessage = "An EGLContext argument does not name a valid EGL rendering context. ";
                    break;
                case EGL10.EGL_BAD_CONFIG:
                    errorMessage = "An EGLConfig argument does not name a valid EGL frame buffer configuration. ";
                    break;
                case EGL10.EGL_BAD_CURRENT_SURFACE:
                    errorMessage = "The current surface of the calling thread "
                            + "is a window, pixel buffer or pixmap that is no longer valid. ";
                    break;
                case EGL10.EGL_BAD_DISPLAY:
                    errorMessage = "An EGLDisplay argument does not name a valid EGL display connection. ";
                    break;
                case EGL10.EGL_BAD_SURFACE:
                    errorMessage = "An EGLSurface argument does not name a "
                            + "valid surface (window, pixel buffer or pixmap) configured for GL rendering. ";
                    break;
                case EGL10.EGL_BAD_MATCH:
                    errorMessage = "Arguments are inconsistent (for example, a "
                            + "valid context requires buffers not supplied by a valid surface). ";
                    break;
                case EGL10.EGL_BAD_PARAMETER:
                    errorMessage = "One or more argument values are invalid.";
                    break;
                case EGL10.EGL_BAD_NATIVE_PIXMAP:
                    errorMessage = "A NativePixmapType argument does not refer to a valid native pixmap. ";
                    break;
                case EGL10.EGL_BAD_NATIVE_WINDOW:
                    errorMessage = "A NativeWindowType argument does not refer to a valid native window. ";
                    break;
                case EGL11.EGL_CONTEXT_LOST:
                    errorMessage = "A power management event has occurred. "
                            + "The application must destroy all contexts and reinitialise "
                            + "OpenGL ES state and objects to continue rendering. ";
                    break;
                default:
                    errorMessage = "Unknown";
            }
            
            throw new RendererException("EGL error 0x" + Integer.toHexString(error) + ": " + errorMessage);
        }
    }

    /**
     * Checks for an OpenGL error and throws a {@link RendererException} if
     * there is one. Does nothing if {@link RendererUtil#ENABLE_ERROR_CHECKING}
     * is set to
     * <code>false</code>.
     */
    public static void checkGLError() {
        if (!ENABLE_ERROR_CHECKING) {
            return;
        }
        int error = GLES20.glGetError();
        if (error != 0) {
            String message = GLU.gluErrorString(error);
            if (message == null) {
                throw new RendererException("An unknown OpenGL error has occurred.");
            } else {
                throw new RendererException("An OpenGL error has occurred: " + message);
            }
        }
    }
}
