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
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.renderer.opengl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jme3.renderer.Caps;
import com.jme3.shader.bufferobject.BufferObject;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;

class IndexedBufferBindingTest {
    @Test
    void contextResetClearsIndexedBufferBindings() throws Exception {
        RecordingGl gl = new RecordingGl();
        GLRenderer renderer = gl.renderer();
        renderer.getCaps().add(Caps.UniformBufferObject);
        renderer.getCaps().add(Caps.ShaderStorageBufferObject);
        BufferObject buffer = new BufferObject();
        buffer.setId(71);
        buffer.clearUpdateNeeded();

        renderer.setUniformBufferObject(3, buffer);
        renderer.setShaderStorageBufferObject(3, buffer);
        assertEquals(2, gl.bufferBaseCalls);
        renderer.invalidateState();

        renderer.setUniformBufferObject(3, buffer);
        renderer.setShaderStorageBufferObject(3, buffer);
        assertEquals(4, gl.bufferBaseCalls);
    }

    @Test
    void uniformAndStorageBuffersUseIndependentIndexedBindingCaches() throws Exception {
        RecordingGl gl = new RecordingGl();
        GLRenderer renderer = gl.renderer();
        renderer.getCaps().add(Caps.UniformBufferObject);
        renderer.getCaps().add(Caps.ShaderStorageBufferObject);
        BufferObject buffer = new BufferObject();
        buffer.setId(72);
        buffer.clearUpdateNeeded();
        renderer.registerNativeObject(buffer);

        renderer.setUniformBufferObject(4, buffer);
        renderer.setShaderStorageBufferObject(4, buffer);
        assertEquals(2, gl.bufferBaseCalls);
        BufferObject other = new BufferObject();
        other.setId(73);
        other.clearUpdateNeeded();
        renderer.setUniformBufferObject(4, other);
        renderer.setShaderStorageBufferObject(4, buffer);
        assertEquals(3, gl.bufferBaseCalls, "changing a UBO must preserve the SSBO cache");
    }

    private static final class RecordingGl implements InvocationHandler {
        int bufferBaseCalls;

        GLRenderer renderer() {
            GL4 gl = (GL4) Proxy.newProxyInstance(GL4.class.getClassLoader(),
                    new Class<?>[]{GL4.class}, this);
            GLExt extension = (GLExt) Proxy.newProxyInstance(GLExt.class.getClassLoader(),
                    new Class<?>[]{GLExt.class}, this);
            return new GLRenderer(gl, extension, null);
        }

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] arguments) {
            if (method.getName().equals("glBindBufferBase")) {
                bufferBaseCalls++;
            }
            return defaultValue(method.getReturnType());
        }

        private static Object defaultValue(Class<?> type) {
            if (!type.isPrimitive() || type == void.class) {
                return null;
            }
            if (type == boolean.class) {
                return false;
            }
            if (type == char.class) {
                return '\0';
            }
            if (type == byte.class) {
                return (byte) 0;
            }
            if (type == short.class) {
                return (short) 0;
            }
            if (type == int.class) {
                return 0;
            }
            if (type == long.class) {
                return 0L;
            }
            if (type == float.class) {
                return 0f;
            }
            if (type == double.class) {
                return 0d;
            }
            throw new AssertionError(type);
        }
    }
}
