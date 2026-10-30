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
import com.jme3.bounding.BoundingVolume;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.shader.VarType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies that changed instance bounds invalidate only the affected groups,
 * and that each group is refreshed at most once during a scene update.
 */
public class InstancedBoundsRefreshTest {

    @Test
    public void unchangedGroupsRetainCachedBounds() {
        Fixture fixture = new Fixture();
        fixture.update();

        fixture.assertRefreshCounts(0, 0);
        fixture.assertBounds();
    }

    @Test
    public void severalSourceChangesRefreshOnlyTheirGroupOnce() {
        Fixture fixture = new Fixture();
        fixture.geometries[0][0].move(20, 0, 0);
        fixture.geometries[0][1].move(30, 0, 0);
        fixture.geometries[0][0].move(10, 0, 0);
        fixture.update();

        fixture.assertRefreshCounts(1, 0);
        fixture.assertBounds();
    }

    @Test
    public void movingNodeRefreshesEachGroupOnce() {
        Fixture fixture = new Fixture();
        fixture.node.move(20, 0, 0);
        fixture.update();

        fixture.assertRefreshCounts(1, 1);
        fixture.assertBounds();
    }

    @Test
    public void movingUninstancedChildLeavesGroupsCached() {
        Fixture fixture = new Fixture();
        fixture.uninstanced.move(20, 0, 0);
        fixture.update();

        fixture.assertRefreshCounts(0, 0);
        fixture.assertBounds();
    }

    @Test
    public void membershipChangesRefreshOnlyTheirGroup() {
        Fixture fixture = new Fixture();
        Geometry added = fixture.createGeometry(0, 50);
        fixture.node.attachChild(added);
        fixture.node.instance();
        fixture.update();

        fixture.assertRefreshCounts(1, 0);
        fixture.assertBounds();

        fixture.resetCounts();
        fixture.node.detachChild(added);
        fixture.update();

        fixture.assertRefreshCounts(1, 0);
        fixture.assertBounds();
    }

    @Test
    public void boundOnlyRefreshInvalidatesOnlyItsGroup() {
        Fixture fixture = new Fixture();
        fixture.meshes[0].setBound(new CountingBox(fixture.counters[0], 4, 5, 6));
        fixture.geometries[0][0].forceRefresh(false, true, false);
        fixture.update();

        fixture.assertRefreshCounts(1, 0);
        fixture.assertBounds();
    }

    @Test
    public void changingModelBoundRefreshesOnlyItsGroup() {
        Fixture fixture = new Fixture();
        fixture.geometries[0][0].setModelBound(
                new CountingBox(fixture.counters[0], 4, 5, 6));
        fixture.update();

        fixture.assertRefreshCounts(1, 0);
        fixture.assertBounds();
    }

    @Test
    public void queryingBatchBoundRefreshesBeforeNodeTraversal() {
        Fixture fixture = new Fixture();
        fixture.geometries[0][0].move(20, 0, 0);
        fixture.batches[0].getWorldBound();

        fixture.assertRefreshCounts(1, 0);
        fixture.update();
        fixture.assertRefreshCounts(1, 0);
        fixture.assertBounds();
    }

    @Test
    public void batchesBeforeSourcesAreRefreshedOnlyOnce() {
        Fixture fixture = new Fixture();
        fixture.node.swapChildren(fixture.node.getChildIndex(fixture.batches[0]), 0);
        fixture.node.swapChildren(fixture.node.getChildIndex(fixture.batches[1]), 1);
        fixture.geometries[0][0].move(20, 0, 0);
        fixture.geometries[0][1].move(30, 0, 0);
        fixture.update();

        fixture.assertRefreshCounts(1, 0);
        fixture.assertBounds();
    }

    private static class Fixture {
        final InstancedNode node = new InstancedNode("instances");
        final Geometry[][] geometries = new Geometry[2][2];
        final Box[] meshes = new Box[2];
        final Material[] materials = new Material[2];
        final InstancedGeometry[] batches = new InstancedGeometry[2];
        final RefreshCounter[] counters = {new RefreshCounter(), new RefreshCounter()};
        final Geometry uninstanced = new Geometry("uninstanced", new Box(1, 1, 1));

        Fixture() {
            MaterialDef def = new MaterialDef(new DesktopAssetManager(), "test");
            def.addMaterialParam(VarType.Boolean, "UseInstancing", false);
            for (int group = 0; group < geometries.length; group++) {
                meshes[group] = new Box(1, 1, 1);
                meshes[group].setBound(new CountingBox(counters[group], 1, 1, 1));
                materials[group] = new Material(def);
                materials[group].setBoolean("UseInstancing", true);
                for (int i = 0; i < geometries[group].length; i++) {
                    Geometry geometry = createGeometry(group, group * 20 + i * 4);
                    geometries[group][i] = geometry;
                    node.attachChild(geometry);
                }
            }
            uninstanced.setMaterial(materials[0]);
            uninstanced.setBatchHint(Spatial.BatchHint.Never);
            node.attachChild(uninstanced);
            node.instance();
            for (Spatial child : node.getChildren()) {
                if (child instanceof InstancedGeometry) {
                    InstancedGeometry batch = (InstancedGeometry) child;
                    for (int group = 0; group < materials.length; group++) {
                        if (batch.getMaterial() == materials[group]) {
                            batches[group] = batch;
                        }
                    }
                }
            }
            update();
            for (int group = 0; group < batches.length; group++) {
                assertNotNull(batches[group]);
                counters[group].batchBound = batches[group].getWorldBound();
            }
            assertBounds();
            resetCounts();
        }

        Geometry createGeometry(int group, float x) {
            Geometry geometry = new Geometry("source", meshes[group]);
            geometry.setMaterial(materials[group]);
            geometry.setLocalTranslation(x, 0, 10);
            return geometry;
        }

        void update() {
            node.updateLogicalState(0);
            node.updateGeometricState();
        }

        void resetCounts() {
            for (RefreshCounter counter : counters) {
                counter.refreshes = 0;
            }
        }

        void assertRefreshCounts(int first, int second) {
            assertEquals(first, counters[0].refreshes, "First group refreshes");
            assertEquals(second, counters[1].refreshes, "Second group refreshes");
        }

        void assertBounds() {
            for (InstancedGeometry batch : batches) {
                BoundingVolume expected = null;
                for (Geometry geometry : batch.getGeometries()) {
                    if (geometry == null) {
                        continue;
                    }
                    if (expected == null) {
                        expected = geometry.getWorldBound().clone();
                    } else {
                        expected.mergeLocal(geometry.getWorldBound());
                    }
                }
                assertEquals(expected, batch.getWorldBound());
            }
        }
    }

    private static class RefreshCounter {
        BoundingVolume batchBound;
        int refreshes;
    }

    /**
     * A group rebuild starts by copying its first source bound into the cached
     * group bound. Count only copies into that exact object, so ordinary node
     * updates and bound queries do not affect the measurement.
     */
    private static class CountingBox extends BoundingBox {
        private final RefreshCounter counter;

        CountingBox(RefreshCounter counter, float xExtent, float yExtent, float zExtent) {
            super(Vector3f.ZERO, xExtent, yExtent, zExtent);
            this.counter = counter;
        }

        @Override
        public BoundingVolume transform(Transform transform, BoundingVolume store) {
            if (!(store instanceof CountingBox)) {
                store = new CountingBox(counter, 0, 0, 0);
            }
            return super.transform(transform, store);
        }

        @Override
        public BoundingVolume clone(BoundingVolume store) {
            if (store != null && store == counter.batchBound) {
                counter.refreshes++;
            }
            return super.clone(store);
        }
    }
}
