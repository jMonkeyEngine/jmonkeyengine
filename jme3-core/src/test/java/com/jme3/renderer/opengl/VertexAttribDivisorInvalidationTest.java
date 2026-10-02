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
 * Verifies lazy restoration of attribute divisors after external GL state changes.
 */
public class VertexAttribDivisorInvalidationTest {
    private static final int ATTRIBUTE_LOCATION = 4;

    @ParameterizedTest
    @CsvSource({
        "false, false, 4", "false, true, 4", "true, false, 4", "true, true, 4",
        "false, false, 16", "false, true, 16", "true, false, 16", "true, true, 16"
    })
    public void externalDivisorsAreResetForZeroSpan(
            boolean previouslyUsed, boolean sameBuffer, int components) {
        RecordingGl gl = new RecordingGl();
        GLRenderer renderer = gl.renderer();
        Mesh mesh = triangle();
        VertexBuffer first = instanceBuffer(0, components);
        if (previouslyUsed) {
            renderer.renderMesh(mesh, 0, 4, new VertexBuffer[] {first});
        }
        gl.changeDivisorsExternally(ATTRIBUTE_LOCATION, components / 4, 2);
        gl.changeDivisorsExternally(0, 1, 3);
        int callsBeforeInvalidation = gl.divisorCalls;

        renderer.invalidateState();

        assertEquals(callsBeforeInvalidation, gl.divisorCalls, "Invalidation must remain lazy");
        renderer.setShader(shader());
        VertexBuffer next = sameBuffer ? first : instanceBuffer(0, components);
        renderer.renderMesh(mesh, 0, 4, new VertexBuffer[] {next});

        assertDivisors(gl.lastDraw(), ATTRIBUTE_LOCATION, components / 4, 0);
        assertEquals(0, gl.lastDraw()[0], "Ordinary position attribute");
        int callsAfterDraw = gl.divisorCalls;
        renderer.renderMesh(mesh, 0, 4, new VertexBuffer[] {next});
        assertEquals(callsAfterDraw, gl.divisorCalls, "Valid state must remain cached");
    }

    @ParameterizedTest
    @CsvSource({"false, 4", "true, 4", "false, 16", "true, 16"})
    public void rendererDivisorsAreResetAfterInvalidation(boolean sameBuffer, int components) {
        RecordingGl gl = new RecordingGl();
        GLRenderer renderer = gl.renderer();
        Mesh mesh = triangle();
        VertexBuffer first = instanceBuffer(2, components);
        renderer.renderMesh(mesh, 0, 4, new VertexBuffer[] {first});

        renderer.invalidateState();
        renderer.setShader(shader());
        VertexBuffer next = sameBuffer ? first : instanceBuffer(0, components);
        next.setInstanceSpan(0);
        renderer.renderMesh(mesh, 0, 4, new VertexBuffer[] {next});

        assertDivisors(gl.lastDraw(), ATTRIBUTE_LOCATION, components / 4, 0);
    }

    @ParameterizedTest
    @CsvSource({"1, 4", "2, 4", "1, 16", "2, 16"})
    public void positiveSpansAreReappliedWithoutAnExtraZeroCall(int span, int components) {
        RecordingGl gl = new RecordingGl();
        GLRenderer renderer = gl.renderer();
        Mesh mesh = triangle();
        VertexBuffer buffer = instanceBuffer(span, components);
        renderer.renderMesh(mesh, 0, 4, new VertexBuffer[] {buffer});
        gl.changeDivisorsExternally(ATTRIBUTE_LOCATION, components / 4, 3);
        final int previousTargetCalls = gl.divisorCallsBySlot[ATTRIBUTE_LOCATION];

        renderer.invalidateState();
        renderer.setShader(shader());
        renderer.renderMesh(mesh, 0, 4, new VertexBuffer[] {buffer});

        assertDivisors(gl.lastDraw(), ATTRIBUTE_LOCATION, components / 4, span);
        assertEquals(previousTargetCalls + 1, gl.divisorCallsBySlot[ATTRIBUTE_LOCATION]);
    }

    @ParameterizedTest
    @CsvSource({"4", "16"})
    public void repeatedInvalidationRestoresConsumedSlotsAgain(int components) {
        RecordingGl gl = new RecordingGl();
        GLRenderer renderer = gl.renderer();
        Mesh mesh = triangle();
        VertexBuffer buffer = instanceBuffer(0, components);
        for (int iteration = 0; iteration < 3; iteration++) {
            gl.changeDivisorsExternally(ATTRIBUTE_LOCATION, components / 4, iteration + 1);
            int previousCalls = gl.divisorCalls;
            renderer.invalidateState();
            renderer.invalidateState();
            assertEquals(previousCalls, gl.divisorCalls, "No GL divisor calls during invalidation");
            renderer.setShader(shader());
            renderer.renderMesh(mesh, 0, 4, new VertexBuffer[] {buffer});
            assertDivisors(gl.lastDraw(), ATTRIBUTE_LOCATION, components / 4, 0);
            assertEquals(previousCalls + components / 4 + 1, gl.divisorCalls);
        }
    }

    @Test
    public void unusedMatrixSlotsRemainInvalidUntilConsumed() {
        RecordingGl gl = new RecordingGl();
        GLRenderer renderer = gl.renderer();
        final Mesh mesh = triangle();
        gl.changeDivisorsExternally(ATTRIBUTE_LOCATION, 4, 2);
        renderer.invalidateState();
        renderer.setShader(shader());

        renderer.renderMesh(mesh, 0, 4, new VertexBuffer[] {instanceBuffer(0, 4)});
        assertEquals(0, gl.lastDraw()[ATTRIBUTE_LOCATION]);
        assertDivisors(gl.lastDraw(), ATTRIBUTE_LOCATION + 1, 3, 2);
        renderer.renderMesh(mesh, 0, 4, new VertexBuffer[] {instanceBuffer(0, 16)});
        assertDivisors(gl.lastDraw(), ATTRIBUTE_LOCATION, 4, 0);
    }

    @Test
    public void freshRendererRetainsKnownZeroDefaults() {
        RecordingGl gl = new RecordingGl();
        GLRenderer renderer = gl.renderer();
        Mesh mesh = triangle();
        renderer.renderMesh(mesh, 0, 4, new VertexBuffer[] {instanceBuffer(0, 16)});
        renderer.renderMesh(mesh, 0, 4, null);
        assertEquals(0, gl.divisorCalls);
    }

    @Test
    public void freshNativeContextAfterInvalidationReceivesZeroDefaults() {
        RecordingGl gl = new RecordingGl();
        GLRenderer renderer = gl.renderer();
        renderer.invalidateState();
        renderer.setShader(shader());
        renderer.renderMesh(triangle(), 0, 4, new VertexBuffer[] {instanceBuffer(0, 16)});
        assertDivisors(gl.lastDraw(), ATTRIBUTE_LOCATION, 4, 0);
    }

    @Test
    public void unavailableInstancingNeverCallsDivisorExtension() {
        RecordingGl gl = new RecordingGl();
        GLRenderer renderer = gl.renderer();
        renderer.getCaps().remove(Caps.MeshInstancing);
        Mesh mesh = triangle();
        for (int iteration = 0; iteration < 2; iteration++) {
            renderer.invalidateState();
            renderer.setShader(shader());
            renderer.renderMesh(mesh, 0, 1, new VertexBuffer[] {instanceBuffer(0, 16)});
            renderer.renderMesh(mesh, 0, 1, null);
        }
        assertEquals(0, gl.divisorCalls);
    }

    private static void assertDivisors(int[] divisors, int first, int count, int expected) {
        for (int slot = first; slot < first + count; slot++) {
            assertEquals(expected, divisors[slot], "Attribute slot " + slot);
        }
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
        private final int[] divisorCallsBySlot = new int[16];
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

        private void changeDivisorsExternally(int first, int count, int value) {
            for (int slot = first; slot < first + count; slot++) {
                divisors[slot] = value;
            }
        }

        private int[] lastDraw() {
            return drawDivisors.get(drawDivisors.size() - 1);
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
                    divisorCallsBySlot[slot]++;
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
