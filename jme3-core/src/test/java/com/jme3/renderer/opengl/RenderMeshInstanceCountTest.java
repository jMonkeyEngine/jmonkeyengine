/*
 * Copyright (c) 2009-2026 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.renderer.opengl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jme3.renderer.Caps;
import com.jme3.renderer.RendererException;
import com.jme3.renderer.Statistics;
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

public class RenderMeshInstanceCountTest {

    @Test
    public void testMeshOwnedInstancesUseTheSubmittedCountForStatsAndDraw() {
        RecordingGl gl = new RecordingGl();
        GLRenderer renderer = gl.renderer();
        Mesh mesh = triangle();
        VertexBuffer instances = instanceBuffer(4);
        mesh.setBuffer(instances);

        renderer.renderMesh(mesh, 0, 1, null);

        assertEquals(List.of(4), gl.instancedDrawCounts);
        assertStatistics(renderer, 12, 4, 1);
    }

    @Test
    public void testCallerInstanceCountIsUsedForStatsAndDraw() {
        RecordingGl gl = new RecordingGl();
        GLRenderer renderer = gl.renderer();

        renderer.renderMesh(triangle(), 0, 5, null);

        assertEquals(List.of(5), gl.instancedDrawCounts);
        assertStatistics(renderer, 15, 5, 1);
    }

    @Test
    public void testOrdinaryDrawUsesOneInstanceInStatsAndSubmission() {
        RecordingGl gl = new RecordingGl();
        GLRenderer renderer = gl.renderer();

        renderer.renderMesh(triangle(), 0, 1, null);

        assertEquals(1, gl.ordinaryDraws);
        assertEquals(List.of(), gl.instancedDrawCounts);
        assertStatistics(renderer, 3, 1, 1);
    }

    @Test
    public void testExplicitZeroCountStillSkipsMeshOwnedInstances() {
        RecordingGl gl = new RecordingGl();
        GLRenderer renderer = gl.renderer();
        Mesh mesh = triangle();
        mesh.setBuffer(instanceBuffer(4));

        renderer.getCaps().remove(Caps.MeshInstancing);

        renderer.renderMesh(mesh, 0, 0, null);

        assertEquals(0, gl.ordinaryDraws);
        assertEquals(List.of(), gl.instancedDrawCounts);
        assertStatistics(renderer, 0, 0, 0);
    }

    @Test
    public void testMeshOwnedInstancesAreIncludedInCapabilityValidation() {
        RecordingGl gl = new RecordingGl();
        GLRenderer renderer = gl.renderer();
        renderer.getCaps().remove(Caps.MeshInstancing);
        Mesh mesh = triangle();
        mesh.setBuffer(instanceBuffer(4));

        // A missing shader attribute must not bypass mesh-level capability validation.
        Shader shader = shader(2);
        shader.getAttribute(VertexBuffer.Type.InstanceData).setLocation(-1);
        renderer.setShader(shader);

        assertThrows(RendererException.class, () -> renderer.renderMesh(mesh, 0, 1, null));
        assertEquals(0, gl.ordinaryDraws);
        assertEquals(List.of(), gl.instancedDrawCounts);
        assertEquals(1, gl.nextBufferId);
        assertStatistics(renderer, 0, 0, 0);
    }

    @Test
    public void testIndexedLodUsesTheGreaterMeshOrCallerCount() {
        RecordingGl gl = new RecordingGl();
        Mesh mesh = triangle();
        VertexBuffer fullIndices = indexBuffer((short) 0, (short) 1, (short) 2,
                (short) 0, (short) 2, (short) 1);
        mesh.setBuffer(fullIndices);
        mesh.setLodLevels(new VertexBuffer[] {fullIndices,
                indexBuffer((short) 0, (short) 1, (short) 2)});
        mesh.setBuffer(instanceBuffer(4));
        GLRenderer renderer = gl.renderer();

        renderer.renderMesh(mesh, 1, 2, null);
        renderer.renderMesh(mesh, 1, 6, null);

        assertEquals(List.of(4, 6), gl.instancedDrawCounts);
        assertEquals(List.of(3, 3), gl.instancedElementCounts);
        assertEquals(0, gl.ordinaryDraws);
        assertStatistics(renderer, 30, 10, 2);
    }

    private static VertexBuffer indexBuffer(short... indices) {
        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Type.Index);
        buffer.setupData(VertexBuffer.Usage.Static, 3, VertexBuffer.Format.UnsignedShort,
                BufferUtils.createShortBuffer(indices));
        return buffer;
    }

    private static void assertStatistics(GLRenderer renderer, int vertices, int triangles, int objects) {
        int[] values = new int[new Statistics().getLabels().length];
        renderer.getStatistics().getData(values);
        assertEquals(vertices, values[0]);
        assertEquals(triangles, values[1]);
        assertEquals(objects, values[3]);
    }

    private static Mesh triangle() {
        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, new float[] {0, 0, 0, 1, 0, 0, 0, 1, 0});
        return mesh;
    }

    private static VertexBuffer instanceBuffer(int count) {
        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Type.InstanceData);
        buffer.setupData(VertexBuffer.Usage.Static, 1, VertexBuffer.Format.Float,
                BufferUtils.createFloatBuffer(count));
        buffer.setInstanceSpan(1);
        return buffer;
    }

    private static Shader shader(int id) {
        Shader shader = new Shader();
        shader.setId(id);
        shader.clearUpdateNeeded();
        shader.getAttribute(VertexBuffer.Type.Position).setLocation(0);
        shader.getAttribute(VertexBuffer.Type.InstanceData).setLocation(1);
        return shader;
    }

    private static final class RecordingGl implements InvocationHandler {
        final List<Integer> instancedDrawCounts = new ArrayList<>();
        final List<Integer> instancedElementCounts = new ArrayList<>();
        int nextBufferId = 1;
        int ordinaryDraws;

        private <T> T proxy(Class<T> api) {
            return api.cast(Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[] {api}, this));
        }

        GLRenderer renderer() {
            GLRenderer renderer = new GLRenderer(proxy(GL.class), proxy(GLExt.class), proxy(GLFbo.class));
            renderer.getCaps().add(Caps.MeshInstancing);
            renderer.getStatistics().setEnabled(true);
            renderer.setShader(shader(1));
            return renderer;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] arguments) {
            switch (method.getName()) {
                case "glGenBuffers" -> ((IntBuffer) arguments[0]).put(0, nextBufferId++);
                case "glDrawArraysInstancedARB" -> instancedDrawCounts.add((Integer) arguments[3]);
                case "glDrawElementsInstancedARB" -> {
                    instancedElementCounts.add((Integer) arguments[1]);
                    instancedDrawCounts.add((Integer) arguments[4]);
                }
                case "glDrawArrays", "glDrawRangeElements" -> ordinaryDraws++;
                default -> { }
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
