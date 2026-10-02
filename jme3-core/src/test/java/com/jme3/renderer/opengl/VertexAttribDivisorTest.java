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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jme3.renderer.Caps;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.shader.Shader;
import com.jme3.util.BufferUtils;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Verifies that attribute divisors track numeric instance spans at each draw.
 */
public class VertexAttribDivisorTest {
    private static final int ATTRIBUTE_LOCATION = 4;

    @ParameterizedTest
    @CsvSource({
        "1, 2, false, 4", "2, 1, false, 4", "0, 2, false, 4", "2, 0, false, 4",
        "1, 2, true, 4", "2, 1, true, 4", "0, 2, true, 4", "2, 0, true, 4",
        "1, 2, false, 16", "2, 1, false, 16", "0, 2, false, 16", "2, 0, false, 16",
        "1, 2, true, 16", "2, 1, true, 16", "0, 2, true, 16", "2, 0, true, 16"
    })
    public void spanTransitionsUpdateEverySlotAtDraw(
            int firstSpan, int secondSpan, boolean sameBuffer, int components) {
        RecordingGl gl = new RecordingGl();
        GLRenderer renderer = gl.renderer();
        Mesh mesh = triangle();
        VertexBuffer first = instanceBuffer(firstSpan, components);
        VertexBuffer second = sameBuffer ? first : instanceBuffer(secondSpan, components);

        renderer.renderMesh(mesh, 0, 4, new VertexBuffer[] {first});
        if (sameBuffer) {
            second.setInstanceSpan(secondSpan);
        }
        renderer.renderMesh(mesh, 0, 4, new VertexBuffer[] {second});

        assertEquals(2, gl.drawDivisors.size());
        for (int slot = ATTRIBUTE_LOCATION; slot < ATTRIBUTE_LOCATION + components / 4; slot++) {
            assertEquals(firstSpan, gl.drawDivisors.get(0)[slot], "First draw, slot " + slot);
            assertEquals(secondSpan, gl.drawDivisors.get(1)[slot], "Second draw, slot " + slot);
        }
    }

    @ParameterizedTest
    @CsvSource({"true, 4", "false, 4", "true, 16", "false, 16"})
    public void unchangedSpanDoesNotRepeatDivisorCalls(boolean sameBuffer, int components) {
        RecordingGl gl = new RecordingGl();
        GLRenderer renderer = gl.renderer();
        Mesh mesh = triangle();
        VertexBuffer first = instanceBuffer(2, components);
        VertexBuffer second = sameBuffer ? first : instanceBuffer(2, components);

        renderer.renderMesh(mesh, 0, 4, new VertexBuffer[] {first});
        int initialCalls = gl.divisorCalls;
        renderer.renderMesh(mesh, 0, 4, new VertexBuffer[] {second});

        assertEquals(components / 4, initialCalls);
        assertEquals(initialCalls, gl.divisorCalls);
    }

    @Test
    public void clearingMutatedBufferResetsDivisorsBeforeSlotReuse() {
        verifyClearedDivisors(false);
    }

    @Test
    public void clearingCollectedBufferResetsDivisorsBeforeSlotReuse() {
        verifyClearedDivisors(true);
    }

    @Test
    public void shrinkingToSingleSlotResetsUnusedMatrixDivisors() {
        RecordingGl gl = new RecordingGl();
        GLRenderer renderer = gl.renderer();
        Mesh mesh = triangle();

        renderer.renderMesh(mesh, 0, 4, new VertexBuffer[] {instanceBuffer(2, 16)});
        renderer.renderMesh(mesh, 0, 4, new VertexBuffer[] {instanceBuffer(1, 4)});

        assertEquals(1, gl.drawDivisors.get(1)[ATTRIBUTE_LOCATION]);
        for (int slot = ATTRIBUTE_LOCATION + 1; slot < ATTRIBUTE_LOCATION + 4; slot++) {
            assertEquals(0, gl.drawDivisors.get(1)[slot], "Unused matrix slot " + slot);
        }
    }

    @Test
    public void nonInstancedRendererDoesNotCallDivisorExtension() {
        RecordingGl gl = new RecordingGl();
        GLRenderer renderer = gl.renderer();
        renderer.getCaps().remove(Caps.MeshInstancing);
        Mesh mesh = triangle();

        renderer.renderMesh(mesh, 0, 1, new VertexBuffer[] {instanceBuffer(0, 4)});
        renderer.renderMesh(mesh, 0, 1, null);

        assertEquals(0, gl.divisorCalls);
    }

    @Test
    public void invalidatedContextReappliesPositiveDivisor() {
        RecordingGl gl = new RecordingGl();
        GLRenderer renderer = gl.renderer();
        Mesh mesh = triangle();
        VertexBuffer buffer = instanceBuffer(2, 4);

        renderer.renderMesh(mesh, 0, 4, new VertexBuffer[] {buffer});
        renderer.invalidateState();
        renderer.setShader(shader());
        renderer.renderMesh(mesh, 0, 4, new VertexBuffer[] {buffer});

        assertEquals(2, gl.divisorCalls);
        assertEquals(2, gl.drawDivisors.get(1)[ATTRIBUTE_LOCATION]);
    }

    private static void verifyClearedDivisors(boolean clearReference) {
        RecordingGl gl = new RecordingGl();
        GLRenderer renderer = gl.renderer();
        Mesh mesh = triangle();
        VertexBuffer buffer = instanceBuffer(2, 16);
        renderer.renderMesh(mesh, 0, 4, new VertexBuffer[] {buffer});
        if (clearReference) {
            // Deterministically simulate collection of the cached weak reference.
            buffer.getWeakRef().clear();
        } else {
            buffer.setInstanceSpan(0);
        }

        renderer.renderMesh(mesh, 0, 4, null);
        renderer.renderMesh(mesh, 0, 4, new VertexBuffer[] {instanceBuffer(0, 16)});

        for (int slot = ATTRIBUTE_LOCATION; slot < ATTRIBUTE_LOCATION + 4; slot++) {
            assertEquals(0, gl.drawDivisors.get(1)[slot], "Cleared slot " + slot);
            assertEquals(0, gl.drawDivisors.get(2)[slot], "Reused slot " + slot);
        }
        assertEquals(8, gl.divisorCalls);
    }

    private static VertexBuffer instanceBuffer(int span, int components) {
        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Type.InstanceData);
        buffer.setupData(VertexBuffer.Usage.Static, components, VertexBuffer.Format.Float,
                BufferUtils.createFloatBuffer(components * 4));
        buffer.setInstanceSpan(span);
        return buffer;
    }

    private static Mesh triangle() {
        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, new float[] {0, 0, 0, 1, 0, 0, 0, 1, 0});
        return mesh;
    }

    private static Shader shader() {
        Shader shader = new Shader();
        shader.setId(1);
        shader.clearUpdateNeeded();
        shader.getAttribute(VertexBuffer.Type.Position).setLocation(0);
        shader.getAttribute(VertexBuffer.Type.InstanceData).setLocation(ATTRIBUTE_LOCATION);
        return shader;
    }

    private static final class RecordingGl implements InvocationHandler {
        private final int[] divisors = new int[16];
        private final List<int[]> drawDivisors = new ArrayList<>();
        private int nextBufferId = 10;
        private int divisorCalls;

        private <T> T proxy(Class<T> api) {
            return api.cast(Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[] {api}, this));
        }

        private GLRenderer renderer() {
            GLRenderer renderer = new GLRenderer(proxy(GL.class), proxy(GLExt.class), proxy(GLFbo.class));
            renderer.getCaps().add(Caps.MeshInstancing);
            renderer.setShader(shader());
            return renderer;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] arguments) {
            switch (method.getName()) {
                case "glGenBuffers":
                    ((IntBuffer) arguments[0]).put(0, nextBufferId++);
                    break;
                case "glVertexAttribDivisorARB":
                    int slot = (Integer) arguments[0];
                    divisors[slot] = (Integer) arguments[1];
                    divisorCalls++;
                    break;
                case "glDrawArraysInstancedARB":
                case "glDrawArrays":
                    drawDivisors.add(divisors.clone());
                    break;
                default:
                    break;
            }
            if (method.getReturnType() == boolean.class) {
                return false;
            }
            if (method.getReturnType() == int.class) {
                return 0;
            }
            return null;
        }
    }
}
