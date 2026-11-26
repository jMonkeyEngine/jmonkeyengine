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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.renderer.Caps;
import com.jme3.renderer.RenderContext;
import com.jme3.scene.VertexBuffer;
import com.jme3.shader.Shader;
import com.jme3.shader.bufferobject.BufferObject;
import com.jme3.util.BufferUtils;
import com.jme3.util.NativeObjectManager;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class NativeResourceDeletionTest {
    @Test
    void shaderDeleteUnregistersBeforeItsIdIsReused() throws Exception {
        RecordingGl gl = new RecordingGl();
        GLRenderer renderer = gl.renderer();
        Shader shader = new Shader();
        shader.setId(11);
        renderer.registerNativeObject(shader);

        renderer.deleteShader(shader);
        shader.setId(12);
        renderer.registerNativeObject(shader);
        manager(renderer).deleteAllObjects(renderer);

        assertEquals(Arrays.asList(11, 12), gl.deletedPrograms);
    }

    @Test
    void vertexBufferDeleteUnregistersBeforeItsIdIsReused() throws Exception {
        RecordingGl gl = new RecordingGl();
        GLRenderer renderer = gl.renderer();
        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Type.Position);
        buffer.setId(21);
        renderer.registerNativeObject(buffer);

        renderer.deleteBuffer(buffer);
        buffer.setId(22);
        renderer.registerNativeObject(buffer);
        manager(renderer).deleteAllObjects(renderer);

        assertEquals(Arrays.asList(21, 22), gl.deletedBuffers);
    }

    @Test
    void bufferObjectDeleteUnregistersBeforeItsIdIsReused() throws Exception {
        RecordingGl gl = new RecordingGl();
        GLRenderer renderer = gl.renderer();
        BufferObject buffer = new BufferObject();
        buffer.setId(31);
        renderer.registerNativeObject(buffer);

        renderer.deleteBuffer(buffer);
        buffer.setId(32);
        renderer.registerNativeObject(buffer);
        manager(renderer).deleteAllObjects(renderer);

        assertEquals(Arrays.asList(31, 32), gl.deletedBuffers);
    }

    @Test
    void fenceDeleteUnregistersBeforeCleanup() throws Exception {
        RecordingGl gl = new RecordingGl();
        GLRenderer renderer = gl.renderer();
        GLFence first = new GLFence(41L, null);
        renderer.registerNativeObject(first);

        renderer.deleteFence(first);
        GLFence second = new GLFence(42L, null);
        renderer.registerNativeObject(second);
        manager(renderer).deleteAllObjects(renderer);

        assertEquals(Arrays.asList(first.getFenceId(), second.getFenceId()), gl.deletedFences);
    }

    @Test
    void deletingVertexBufferInvalidatesItsAttributeAndBindingCaches() throws Exception {
        RecordingGl gl = new RecordingGl();
        GLRenderer renderer = gl.renderer();
        RenderContext context = context(renderer);
        Shader shader = new Shader();
        context.boundShader = shader;
        context.boundShaderProgram = 50;

        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Type.Position);
        buffer.setupData(VertexBuffer.Usage.Static, 3, VertexBuffer.Format.Float,
                BufferUtils.createFloatBuffer(3));
        buffer.setId(51);
        buffer.clearUpdateNeeded();
        renderer.registerNativeObject(buffer);
        renderer.setVertexAttrib(buffer);
        assertEquals(1, gl.attributePointerCalls);

        renderer.deleteBuffer(buffer);
        assertNull(context.boundAttribs[0]);
        assertEquals(0, context.boundArrayVBO);

        buffer.setId(52);
        buffer.clearUpdateNeeded();
        renderer.setVertexAttrib(buffer);
        assertEquals(2, gl.attributePointerCalls);
    }

    @Test
    void deletingBufferObjectInvalidatesBothIndexedBindingCaches() throws Exception {
        RecordingGl gl = new RecordingGl();
        GLRenderer renderer = gl.renderer();
        renderer.getCaps().add(Caps.UniformBufferObject);
        renderer.getCaps().add(Caps.ShaderStorageBufferObject);
        final RenderContext context = context(renderer);
        BufferObject buffer = new BufferObject();
        buffer.setId(61);
        buffer.clearUpdateNeeded();
        renderer.registerNativeObject(buffer);

        renderer.setUniformBufferObject(2, buffer);
        renderer.setShaderStorageBufferObject(2, buffer);
        assertEquals(2, gl.bufferBaseCalls);
        renderer.deleteBuffer(buffer);
        assertNull(context.boundBO[2]);
        assertNull(context.boundSsbo[2]);

        buffer.setId(62);
        buffer.clearUpdateNeeded();
        renderer.setUniformBufferObject(2, buffer);
        renderer.setShaderStorageBufferObject(2, buffer);
        assertEquals(4, gl.bufferBaseCalls);
    }

    @Test
    void deletingInstancedVertexBufferResetsItsDivisorBeforeClearingCache() throws Exception {
        RecordingGl gl = new RecordingGl();
        GLRenderer renderer = gl.renderer();
        renderer.getCaps().add(Caps.MeshInstancing);
        RenderContext context = context(renderer);
        gl.context = context;
        context.boundShader = new Shader();
        context.boundShaderProgram = 75;

        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Type.Position);
        buffer.setupData(VertexBuffer.Usage.Static, 3, VertexBuffer.Format.Float,
                BufferUtils.createFloatBuffer(3));
        buffer.setInstanced(true);
        buffer.setId(76);
        buffer.clearUpdateNeeded();
        renderer.registerNativeObject(buffer);
        renderer.setVertexAttrib(buffer);
        assertEquals(Arrays.asList(1), gl.attributeDivisors);

        renderer.deleteBuffer(buffer);

        assertEquals(Arrays.asList(1, 0), gl.attributeDivisors);
        assertTrue(gl.divisorResetSawCachedSlot,
                "the cached slot must be present when its divisor is reset");
        assertNull(context.boundAttribs[0]);
    }

    @Test
    void deletingInterleavedBackingBufferInvalidatesLogicalAttributeCache() throws Exception {
        RecordingGl gl = new RecordingGl();
        GLRenderer renderer = gl.renderer();
        RenderContext context = context(renderer);
        context.boundShader = new Shader();
        context.boundShaderProgram = 77;

        VertexBuffer logical = new VertexBuffer(VertexBuffer.Type.Position);
        logical.setupData(VertexBuffer.Usage.Static, 3, VertexBuffer.Format.Float,
                BufferUtils.createFloatBuffer(3));
        logical.setId(78);
        logical.clearUpdateNeeded();
        VertexBuffer backing = new VertexBuffer(VertexBuffer.Type.InterleavedData);
        backing.setId(79);
        backing.clearUpdateNeeded();
        renderer.registerNativeObject(backing);

        renderer.setVertexAttrib(logical, backing);
        assertEquals(Arrays.asList(79), gl.attributePointerBuffers);

        renderer.deleteBuffer(backing);
        backing.setId(80);
        backing.clearUpdateNeeded();
        renderer.setVertexAttrib(logical, backing);

        assertEquals(Arrays.asList(79, 80), gl.attributePointerBuffers);
    }

    @Test
    void managerDeletionResetsDivisorAfterAttributeReferenceIsCleared() throws Exception {
        RecordingGl gl = new RecordingGl();
        GLRenderer renderer = gl.renderer();
        renderer.getCaps().add(Caps.MeshInstancing);
        RenderContext context = context(renderer);
        gl.context = context;
        context.boundShader = new Shader();
        context.boundShaderProgram = 83;

        VertexBuffer instanced = new VertexBuffer(VertexBuffer.Type.Position);
        instanced.setupData(VertexBuffer.Usage.Static, 3, VertexBuffer.Format.Float,
                BufferUtils.createFloatBuffer(3));
        instanced.setInstanced(true);
        instanced.setId(84);
        instanced.clearUpdateNeeded();
        renderer.registerNativeObject(instanced);
        renderer.setVertexAttrib(instanced);
        assertEquals(Arrays.asList(1), gl.attributeDivisors);

        context.boundAttribs[0].clear();
        manager(renderer).deleteAllObjects(renderer);

        assertEquals(Arrays.asList(1, 0), gl.attributeDivisors);
        assertTrue(gl.divisorResetSawCachedSlot,
                "the slot must be reset before its weak-reference entry is cleared");
        assertNull(context.boundAttribs[0]);

        VertexBuffer perVertex = new VertexBuffer(VertexBuffer.Type.Position);
        perVertex.setupData(VertexBuffer.Usage.Static, 3, VertexBuffer.Format.Float,
                BufferUtils.createFloatBuffer(3));
        perVertex.setId(85);
        perVertex.clearUpdateNeeded();
        renderer.setVertexAttrib(perVertex);

        assertEquals(Arrays.asList(1, 0), gl.attributeDivisors,
                "the non-instanced binding must inherit divisor zero");
        assertEquals(Arrays.asList(84, 85), gl.attributePointerBuffers);
    }

    @Test
    void deletingCurrentlyBoundShaderClearsTheProgramCache() throws Exception {
        RecordingGl gl = new RecordingGl();
        GLRenderer renderer = gl.renderer();
        RenderContext context = context(renderer);
        Shader shader = new Shader();
        shader.setId(81);
        renderer.registerNativeObject(shader);
        context.boundShaderProgram = 81;
        context.boundShader = shader;

        renderer.deleteShader(shader);

        assertEquals(0, context.boundShaderProgram);
        assertNull(context.boundShader);
        assertEquals(Arrays.asList(0), gl.usedPrograms);
    }

    private static NativeObjectManager manager(GLRenderer renderer) throws Exception {
        Field field = GLRenderer.class.getDeclaredField("objManager");
        field.setAccessible(true);
        return (NativeObjectManager) field.get(renderer);
    }

    private static RenderContext context(GLRenderer renderer) throws Exception {
        Field field = GLRenderer.class.getDeclaredField("context");
        field.setAccessible(true);
        return (RenderContext) field.get(renderer);
    }

    private static final class RecordingGl implements InvocationHandler {
        final List<Integer> deletedPrograms = new ArrayList<>();
        final List<Integer> deletedBuffers = new ArrayList<>();
        final List<Long> deletedFences = new ArrayList<>();
        final List<Integer> usedPrograms = new ArrayList<>();
        final List<Integer> attributeDivisors = new ArrayList<>();
        final List<Integer> attributePointerBuffers = new ArrayList<>();
        int attributePointerCalls;
        int bufferBaseCalls;
        int boundArrayBuffer;
        RenderContext context;
        boolean divisorResetSawCachedSlot;

        GLRenderer renderer() {
            GL4 gl = (GL4) Proxy.newProxyInstance(GL4.class.getClassLoader(),
                    new Class<?>[]{GL4.class}, this);
            GLExt extension = (GLExt) Proxy.newProxyInstance(GLExt.class.getClassLoader(),
                    new Class<?>[]{GLExt.class}, this);
            return new GLRenderer(gl, extension, null);
        }

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] arguments) {
            String name = method.getName();
            if (name.equals("glDeleteProgram")) {
                deletedPrograms.add((Integer) arguments[0]);
            } else if (name.equals("glDeleteBuffers")) {
                IntBuffer ids = (IntBuffer) arguments[0];
                for (int index = ids.position(); index < ids.limit(); index++) {
                    deletedBuffers.add(ids.get(index));
                }
            } else if (name.equals("glDeleteSync")) {
                deletedFences.add(((GLFence) arguments[0]).getFenceId());
            } else if (name.equals("glUseProgram")) {
                usedPrograms.add((Integer) arguments[0]);
            } else if (name.equals("glBindBuffer") && (Integer) arguments[0] == GL.GL_ARRAY_BUFFER) {
                boundArrayBuffer = (Integer) arguments[1];
            } else if (name.equals("glVertexAttribPointer")) {
                attributePointerCalls++;
                attributePointerBuffers.add(boundArrayBuffer);
            } else if (name.equals("glBindBufferBase")) {
                bufferBaseCalls++;
            } else if (name.equals("glVertexAttribDivisorARB")) {
                int divisor = (Integer) arguments[1];
                attributeDivisors.add(divisor);
                if (divisor == 0 && context != null) {
                    int index = (Integer) arguments[0];
                    divisorResetSawCachedSlot |= context.boundAttribs[index] != null;
                }
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
