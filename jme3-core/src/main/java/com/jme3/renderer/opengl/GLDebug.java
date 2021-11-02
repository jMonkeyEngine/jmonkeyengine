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
package com.jme3.renderer.opengl;

import com.jme3.renderer.RendererException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * This class uses Reflection to intercept method calls to the Proxy Object ({@link #createProxy(GL, Object, Class[])}
 * and extends them with the Error Checking in {@link #checkError()}.<br>
 * This means we don't have to generate a class with overrides for every possible method just to add a
 * {@link #checkError()} call.<br>
 * Note that we should not call {@link #checkError()} for {@link GL#glGetError()}, it doesn't make sense.<br>
 * Note that this class is general purpose and as such every class instance (every object) can be guarded as long as
 * the passed gl instance is valid.
 *
 * @author MeFisto94
 */
public class GLDebug implements InvocationHandler {
    protected Object obj;
    protected GL gl;
    protected Method methodGlGetError;

    private GLDebug(GL gl, Object obj) throws NoSuchMethodException {
        this.gl = gl;
        this.obj = obj;
        methodGlGetError = GL.class.getMethod("glGetError");
        /* The NoSuchMethodException shouldn't be thrown, but since we're in a constructor and cannot fail safe
         * otherwise, we throw it. */
    }

    protected String decodeError(int err) {
        String errMsg;
        switch (err) {
            case GL.GL_NO_ERROR:
                errMsg = "No Error";
                break;
            case GL.GL_INVALID_ENUM:
                errMsg = "Invalid enum argument";
                break;
            case GL.GL_INVALID_OPERATION:
                errMsg = "Invalid operation";
                break;
            case GL.GL_INVALID_VALUE:
                errMsg = "Invalid numeric argument";
                break;
            case GL.GL_OUT_OF_MEMORY:
                errMsg = "Out of memory";
                break;
            case GLFbo.GL_INVALID_FRAMEBUFFER_OPERATION_EXT:
                errMsg = "Framebuffer is not complete";
                break;
            case GL2.GL_STACK_OVERFLOW:
                errMsg = "Internal stack overflow";
                break;
            case GL2.GL_STACK_UNDERFLOW:
                errMsg = "Internal stack underflow";
                break;
            default:
                errMsg = "Unknown";
                break;
        }
        return errMsg + " (Error Code: " + err + ")";
    }

    protected void checkError() {
        int err = gl.glGetError();
        if (err != 0) {
            throw new RendererException("An OpenGL error occurred - " + decodeError(err));
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = method.invoke(obj, args);

        if (method.equals(methodGlGetError)) {
            return result;
        }

        checkError();
        return result;
    }

    /**
     * Creates a debug-proxied object, which will call {@link GL#glGetError()} after every method invocation and throw
     * a {@link com.jme3.renderer.RendererException} if there was a GL Error.
     *
     * @param gl The GL Context, required to call {@link GL#glGetError()}
     * @param obj The object which methods will be proxied
     * @param implementedInterfaces The interfaces/class this object implements
     * @return The Proxy object (or null if an error occurred)
     */
    public static Object createProxy(GL gl, Object obj, Class<?>... implementedInterfaces) {
        try {
            return Proxy.newProxyInstance(
                    GLDebug.class.getClassLoader(),
                    implementedInterfaces,
                    new GLDebug(gl, obj)
            );
        } catch (NoSuchMethodException nsme) {
            throw new IllegalArgumentException ("Could not initialize the proxy because the glGetError method wasn't " +
                    "found!", nsme);
        }
    }
}
