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
package com.jme3.scene.instancing;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.light.DefaultLightFilter;
import com.jme3.light.LightList;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.shader.VarType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies that instance bounds follow changes to the source geometries.
 */
public class InstancedBoundsTest {

    @Test
    public void movingInstanceUpdatesBounds() {
        checkMovedInstanceBounds(false);
    }

    @Test
    public void movingInstanceAfterItsBatchUpdatesBounds() {
        checkMovedInstanceBounds(true);
    }

    private void checkMovedInstanceBounds(boolean batchFirst) {
        Fixture fixture = new Fixture(false);
        if (batchFirst) {
            fixture.node.swapChildren(0, 1);
        }
        fixture.geometry.move(20, 0, 0);
        fixture.update();

        assertEquals(new Vector3f(20, 0, 10), fixture.geometry.getWorldBound().getCenter());
        fixture.assertBounds();
    }

    @Test
    public void movingNestedParentUpdatesBounds() {
        Fixture fixture = new Fixture(true);
        fixture.geometry.getParent().move(20, 0, 0);
        fixture.update();
        fixture.assertBounds();
    }

    @Test
    public void parentTraversalUpdatesInstancedChildBounds() {
        Fixture fixture = new Fixture(false);
        Node root = new Node("root");
        root.attachChild(fixture.node);
        root.updateGeometricState();

        fixture.geometry.move(20, 0, 0);
        root.updateLogicalState(0);
        root.updateGeometricState();

        fixture.assertBounds();
        assertEquals(fixture.geometry.getWorldBound(), root.getWorldBound());
    }

    @Test
    public void changingModelBoundUpdatesBounds() {
        Fixture fixture = new Fixture(false);
        fixture.geometry.setModelBound(new BoundingBox(Vector3f.ZERO, 4, 5, 6));
        fixture.update();
        fixture.assertBounds();
    }

    @Test
    public void queryingNodeBoundUpdatesInstanceBounds() {
        Fixture fixture = new Fixture(false);
        fixture.geometry.move(20, 0, 0);
        // A direct bounds query also runs the normal lazy bound update path.
        assertEquals(fixture.geometry.getWorldBound(), fixture.node.getWorldBound());
        fixture.assertBounds();
    }

    @Test
    public void movingInstanceKeepsNearbyPointLight() {
        Fixture fixture = new Fixture(false);
        PointLight light = new PointLight(new Vector3f(20, 0, 10));
        light.setRadius(3);
        fixture.node.addLight(light);
        fixture.update();

        Camera camera = new Camera(100, 100);
        camera.setFrustum(1, 100, -50, 50, 50, -50);
        camera.lookAtDirection(Vector3f.UNIT_Z, Vector3f.UNIT_Y);
        DefaultLightFilter filter = new DefaultLightFilter();
        LightList filtered = new LightList(fixture.batch);
        filter.setCamera(camera);
        filter.filterLights(fixture.batch, filtered);
        assertEquals(0, filtered.size(), "The light initially does not reach the instance");

        fixture.geometry.move(20, 0, 0);
        fixture.update();
        fixture.batch.updateInstances(camera);
        assertEquals(1, fixture.batch.getNumVisibleInstances());
        filter.setCamera(camera);
        filtered.clear();
        filter.filterLights(fixture.batch, filtered);
        assertEquals(1, filtered.size(), "The light now reaches the moved instance");
        assertSame(light, filtered.get(0));
    }

    private static class Fixture {
        final InstancedNode node = new InstancedNode("instances");
        final Geometry geometry = new Geometry("instance", new Box(1, 1, 1));
        final InstancedGeometry batch;

        Fixture(boolean nested) {
            MaterialDef def = new MaterialDef(new DesktopAssetManager(), "test");
            def.addMaterialParam(VarType.Boolean, "UseInstancing", false);
            Material material = new Material(def);
            material.setBoolean("UseInstancing", true);
            geometry.setMaterial(material);
            geometry.setLocalTranslation(0, 0, 10);
            Node parent = node;
            if (nested) {
                parent = new Node("nested");
                node.attachChild(parent);
            }
            parent.attachChild(geometry);
            node.instance();
            InstancedGeometry found = null;
            for (Spatial child : node.getChildren()) {
                if (child instanceof InstancedGeometry) {
                    found = (InstancedGeometry) child;
                }
            }
            assertNotNull(found);
            batch = found;
            update();
            assertBounds();
        }

        void update() {
            node.updateLogicalState(0);
            node.updateGeometricState();
        }

        void assertBounds() {
            assertEquals(geometry.getWorldBound(), batch.getWorldBound());
            assertEquals(geometry.getWorldBound(), node.getWorldBound());
        }
    }
}
