/*
 * Copyright (c) 2026 jMonkeyEngine
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
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT OWNER AND CONTRIBUTORS
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
package com.jme3.scene.control;

import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Box;
import com.jme3.util.BufferUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Tests LOD updates when the projected size of a spatial changes. */
public class LodControlTest {

    @Test
    public void testChangingSpatialScaleRecalculatesLodAtFixedDistance() {
        Geometry geometry = geometryWithLodLevels(0.1f);

        TestLodControl control = new TestLodControl();
        geometry.addControl(control);

        Camera camera = new Camera(800, 600);
        camera.setFrustumPerspective(60f, 4f / 3f, 1f, 1000f);
        ViewPort viewPort = new ViewPort("test", camera);

        control.render(viewPort);
        assertEquals(1, geometry.getLodLevel(), "small geometry should use the coarse LOD");

        geometry.setLocalScale(10f);
        geometry.updateGeometricState();
        control.render(viewPort);

        assertEquals(0, geometry.getLodLevel(), "larger geometry should use the detailed LOD");
    }

    @Test
    public void testMovingSpatialSidewaysDoesNotChangePerspectiveLodAtFixedDepth() {
        Geometry geometry = geometryWithLodLevels(1f);

        TestLodControl control = new TestLodControl();
        control.setTrisPerPixel(0.2f);
        geometry.addControl(control);

        Camera camera = new Camera(800, 600);
        camera.setFrustumPerspective(90f, 4f / 3f, 1f, 1000f);
        camera.lookAtDirection(new Vector3f(0f, 0f, -1f), Vector3f.UNIT_Y);
        ViewPort viewPort = new ViewPort("test", camera);

        control.render(viewPort);
        assertEquals(0, geometry.getLodLevel(), "centered geometry should use the detailed LOD");

        geometry.setLocalTranslation(80f, 0f, -100f);
        geometry.updateGeometricState();
        control.render(viewPort);

        assertEquals(0, geometry.getLodLevel(),
                "moving sideways at the same view depth should not reduce detail");
    }

    private static Geometry geometryWithLodLevels(float scale) {
        Mesh mesh = new Box(1f, 1f, 1f);
        VertexBuffer detailed = mesh.getBuffer(VertexBuffer.Type.Index);
        VertexBuffer coarse = oneTriangleIndexBuffer();
        mesh.setLodLevels(new VertexBuffer[]{detailed, coarse});

        Geometry geometry = new Geometry("lod-test", mesh);
        geometry.setLocalTranslation(0f, 0f, -100f);
        geometry.setLocalScale(scale);
        geometry.updateGeometricState();
        return geometry;
    }

    private static VertexBuffer oneTriangleIndexBuffer() {
        VertexBuffer indexBuffer = new VertexBuffer(VertexBuffer.Type.Index);
        indexBuffer.setupData(VertexBuffer.Usage.Static, 3, VertexBuffer.Format.UnsignedShort,
                BufferUtils.createShortBuffer((short) 0, (short) 1, (short) 2));
        return indexBuffer;
    }

    private static final class TestLodControl extends LodControl {

        private void render(ViewPort viewPort) {
            controlRender(null, viewPort);
        }
    }
}
