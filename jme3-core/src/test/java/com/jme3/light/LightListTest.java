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
package com.jme3.light;

import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import java.lang.reflect.Field;
import com.jme3.util.SortUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test cases for LightList.
 */
public class LightListTest {

    /**
     * Verify sort() remains correct after the list previously held more lights.
     */
    @Test
    public void testSortAfterRetainedCapacity() {
        Geometry owner = new Geometry("owner", new Mesh());
        LightList list = new LightList(owner);

        for (int i = 0; i < 32; i++) {
            list.add(new PointLight(new Vector3f(100f + i, 0f, 0f)));
        }
        list.sort(true);
        list.clear();

        PointLight far = new PointLight(new Vector3f(10f, 0f, 0f));
        PointLight near = new PointLight(new Vector3f(1f, 0f, 0f));
        PointLight middle = new PointLight(new Vector3f(5f, 0f, 0f));

        list.add(far);
        list.add(near);
        list.add(middle);
        list.sort(true);

        Assertions.assertSame(near, list.get(0));
        Assertions.assertSame(middle, list.get(1));
        Assertions.assertSame(far, list.get(2));
    }

    /**
     * Verify indexed removal preserves order for front, middle, and tail removals.
     */
    @Test
    public void testRemovePreservesOrder() {
        LightList list = new LightList(new Geometry("owner", new Mesh()));
        AmbientLight first = new AmbientLight();
        AmbientLight second = new AmbientLight();
        AmbientLight third = new AmbientLight();
        AmbientLight fourth = new AmbientLight();

        list.add(first);
        list.add(second);
        list.add(third);
        list.add(fourth);

        list.remove(0);
        Assertions.assertEquals(3, list.size());
        Assertions.assertSame(second, list.get(0));
        Assertions.assertSame(third, list.get(1));
        Assertions.assertSame(fourth, list.get(2));

        list.remove(1);
        Assertions.assertEquals(2, list.size());
        Assertions.assertSame(second, list.get(0));
        Assertions.assertSame(fourth, list.get(1));

        list.remove(1);
        Assertions.assertEquals(1, list.size());
        Assertions.assertSame(second, list.get(0));
    }

    /**
     * Verify unfiltered world-list updates preserve local-then-parent ordering.
     */
    @Test
    public void testUpdateCopiesLocalAndParentLights() {
        LightList local = new LightList(new Geometry("local", new Mesh()));
        LightList parent = new LightList(new Geometry("parent", new Mesh()));
        LightList world = new LightList(new Geometry("world", new Mesh()));

        AmbientLight localFirst = new AmbientLight();
        AmbientLight localSecond = new AmbientLight();
        AmbientLight parentFirst = new AmbientLight();
        AmbientLight parentSecond = new AmbientLight();

        local.add(localFirst);
        local.add(localSecond);
        parent.add(parentFirst);
        parent.add(parentSecond);

        world.update(local, parent);

        Assertions.assertEquals(4, world.size());
        Assertions.assertSame(localFirst, world.get(0));
        Assertions.assertSame(localSecond, world.get(1));
        Assertions.assertSame(parentFirst, world.get(2));
        Assertions.assertSame(parentSecond, world.get(3));
    }

    /**
     * Verify filtered world-list updates keep only accepted local lights.
     */
    @Test
    public void testUpdateFiltersLocalLights() {
        LightList local = new LightList(new Geometry("local", new Mesh()));
        LightList parent = new LightList(new Geometry("parent", new Mesh()));
        LightList world = new LightList(new Geometry("world", new Mesh()));

        AmbientLight keep = new AmbientLight();
        AmbientLight discard = new AmbientLight();
        AmbientLight parentLight = new AmbientLight();

        local.add(keep);
        local.add(discard);
        parent.add(parentLight);

        world.update(local, parent, light -> light != discard);

        Assertions.assertEquals(2, world.size());
        Assertions.assertSame(keep, world.get(0));
        Assertions.assertSame(parentLight, world.get(1));
    }

    @Test
    public void testRemovalAcrossCopySizes() {
        Random random = new Random(92741L);
        for (int count : new int[]{1, 4, 8, 16, 32, 64, 256}) {
            LightList list = new LightList(new Geometry("owner", new Mesh()));
            List<Light> expected = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Light light = new AmbientLight();
                list.add(light);
                expected.add(light);
            }
            while (!expected.isEmpty()) {
                int index = random.nextInt(expected.size());
                list.remove(index);
                expected.remove(index);
                Assertions.assertEquals(expected.size(), list.size());
                for (int i = 0; i < expected.size(); i++) {
                    Assertions.assertSame(expected.get(i), list.get(i));
                }
            }
        }
    }

    @Test
    public void testStableSortAcrossSizesAndOrders() {
        Random random = new Random(21743L);
        for (int count : new int[]{2, 8, 16, 31, 32, 33, 63, 64, 65, 256}) {
            for (int order = 0; order < 3; order++) {
                Geometry owner = new Geometry("owner", new Mesh());
                LightList list = new LightList(owner);
                List<PointLight> input = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    input.add(new PointLight(new Vector3f(i % 9 - 4, 0f, 0f)));
                }
                input.sort(Comparator.comparingDouble(light ->
                        light.getPosition().distanceSquared(owner.getWorldTranslation())));
                if (order == 1) {
                    Collections.reverse(input);
                } else if (order == 2) {
                    Collections.shuffle(input, random);
                }
                for (PointLight light : input) {
                    list.add(light);
                }
                List<PointLight> expected = new ArrayList<>(input);
                expected.sort(Comparator.comparingDouble(light ->
                        light.getPosition().distanceSquared(owner.getWorldTranslation())));
                list.sort(true);
                for (int i = 0; i < count; i++) {
                    Assertions.assertSame(expected.get(i), list.get(i));
                }

                list.remove(count / 2);
                expected.remove(count / 2);
                list.sort(false);
                for (int i = 0; i < expected.size(); i++) {
                    Assertions.assertSame(expected.get(i), list.get(i));
                }

                owner.setLocalTranslation(-2f, 1f, 3f);
                owner.updateGeometricState();
                expected.sort(Comparator.comparingDouble(light ->
                        light.getPosition().distanceSquared(owner.getWorldTranslation())));
                list.sort(true);
                for (int i = 0; i < expected.size(); i++) {
                    Assertions.assertSame(expected.get(i), list.get(i));
                }
            }
        }
    }

    @Test
    public void testNaNDistanceKeepsLegacyOrderAndSingleDistanceUpdate() {
        Geometry owner = new Geometry("owner", new Mesh());
        for (int count : new int[]{2, 8, 32, 64, 65}) {
            LightList list = new LightList(owner);
            Light[] input = new Light[count];
            for (int i = 0; i < count; i++) {
                float x = i == count / 2 ? Float.NaN : count - i;
                CountingPointLight light = new CountingPointLight(new Vector3f(x, 0f, 0f));
                light.lastDistance = light.getPosition().distanceSquared(owner.getWorldTranslation());
                input[i] = light;
                list.add(light);
            }
            Comparator<Light> comparator = (a, b) ->
                    a.lastDistance < b.lastDistance ? -1 : a.lastDistance > b.lastDistance ? 1 : 0;
            Light[] expected = input.clone();
            SortUtil.msort(input.clone(), expected, 0, count - 1, comparator);
            list.sort(true);
            for (int i = 0; i < count; i++) {
                Assertions.assertSame(expected[i], list.get(i));
                Assertions.assertEquals(1, ((CountingPointLight) input[i]).distanceUpdates);
            }
            assertScratchEmptyUnchecked(list);
            list.sort(false);
            assertScratchEmptyUnchecked(list);
            for (Light light : input) {
                Assertions.assertEquals(1, ((CountingPointLight) light).distanceUpdates);
            }
        }
    }

    @Test
    public void testScratchDoesNotRetainLightsAcrossShrinkingSorts() throws Exception {
        LightList list = new LightList(new Geometry("owner", new Mesh()));
        for (int i = 0; i < 128; i++) {
            list.add(new AmbientLight());
        }
        list.sort(false);
        assertScratchEmpty(list);
        Light[] scratch = backingArray(list, "tlist");
        for (int size : new int[]{80, 65, 64, 8, 1, 0}) {
            while (list.size() > size) {
                list.remove(list.size() - 1);
            }
            list.sort(false);
            Assertions.assertEquals(size, list.size());
            assertScratchEmpty(list);
            Assertions.assertSame(scratch, backingArray(list, "tlist"));
        }
    }

    @Test
    public void testRemoveAllAndEmptyClearDoNotRetainSortReferences() throws Exception {
        LightList list = new LightList(new Geometry("owner", new Mesh()));
        for (int i = 0; i < 128; i++) {
            list.add(new AmbientLight());
        }
        list.sort(false);
        while (list.size() > 0) {
            list.remove(list.size() - 1);
        }
        list.clear();
        assertScratchEmpty(list);
        for (Light light : backingArray(list, "list")) {
            Assertions.assertNull(light);
        }
    }

    @Test
    public void testScratchIsEmptyAfterMergeComparisonThrows() throws Exception {
        LightList list = new LightList(new Geometry("owner", new Mesh()));
        for (int i = 0; i < 127; i++) {
            list.add(new AmbientLight());
        }
        list.add(null);
        Assertions.assertThrows(NullPointerException.class, () -> list.sort(false));
        assertScratchEmpty(list);
    }

    @Test
    public void testScratchIsEmptyAfterDistanceComputationThrows() throws Exception {
        LightList list = new LightList(new Geometry("owner", new Mesh()));
        for (int i = 0; i < 127; i++) {
            list.add(new PointLight());
        }
        list.add(new PointLight() {
            @Override
            public void computeLastDistance(Spatial owner) {
                throw new IllegalStateException("distance failure");
            }
        });
        Assertions.assertThrows(IllegalStateException.class, () -> list.sort(true));
        assertScratchEmpty(list);
    }

    @Test
    public void testDirtyParentedOwnerCanReenterWorldLightSort() throws Exception {
        Node parent = new Node("parent");
        Geometry owner = new Geometry("owner", new Box(1, 1, 1));
        parent.attachChild(owner);
        for (int i = 0; i < 128; i++) {
            owner.addLight(new CountingPointLight(new Vector3f(i * 2, 0, 0)));
        }
        parent.updateGeometricState();
        LightList world = owner.getWorldLightList();
        List<Light> expected = new ArrayList<>();
        for (Light light : world) {
            ((CountingPointLight) light).distanceUpdates = 0;
            expected.add(light);
        }
        owner.setLocalTranslation(11, 3, -2);
        world.sort(true);
        int distanceUpdates = 0;
        for (Light light : expected) {
            distanceUpdates += ((CountingPointLight) light).distanceUpdates;
        }
        Assertions.assertTrue(distanceUpdates > expected.size(), "Must exercise the nested world-list sort");
        expected.sort((a, b) -> a.lastDistance < b.lastDistance ? -1
                : a.lastDistance > b.lastDistance ? 1 : 0);
        for (int i = 0; i < expected.size(); i++) {
            Assertions.assertSame(expected.get(i), world.get(i));
        }
        assertScratchEmpty(world);
    }

    @Test
    public void testFilteredUpdateDoesNotReserveRejectedLights() throws Exception {
        Geometry owner = new Geometry("owner", new Mesh());
        LightList local = new LightList(owner);
        LightList parent = new LightList(owner);
        for (int i = 0; i < 257; i++) {
            local.add(new AmbientLight());
            parent.add(new AmbientLight());
        }
        for (int stride : new int[]{0, 16}) {
            LightList world = new LightList(owner);
            int[] calls = {0};
            world.update(local, parent, light -> {
                int index = calls[0]++;
                return stride != 0 && index % stride == 0;
            });
            int accepted = stride == 0 ? 0 : 17;
            Assertions.assertEquals(257, calls[0]);
            Assertions.assertEquals(accepted + parent.size(), world.size());
            Assertions.assertTrue(backingArray(world, "list").length <= 512,
                    "Rejected locals must not force a larger retained array");
            for (int i = 0; i < accepted; i++) {
                Assertions.assertSame(local.get(i * stride), world.get(i));
            }
            for (int i = 0; i < parent.size(); i++) {
                Assertions.assertSame(parent.get(i), world.get(accepted + i));
            }
        }
        LightList empty = new LightList(owner);
        int originalCapacity = backingArray(empty, "list").length;
        empty.update(local, null, light -> false);
        Assertions.assertEquals(originalCapacity, backingArray(empty, "list").length);
    }

    @Test
    public void testFilterCanChangeParentBeforeItIsCopied() {
        Geometry owner = new Geometry("owner", new Mesh());
        for (boolean grow : new boolean[]{true, false}) {
            LightList local = new LightList(owner);
            LightList parent = new LightList(owner);
            LightList world = new LightList(owner);
            for (int i = 0; i < 3; i++) {
                local.add(new AmbientLight());
            }
            parent.add(new AmbientLight());
            int[] calls = {0};
            world.update(local, parent, light -> {
                if (calls[0]++ == 0) {
                    if (grow) {
                        for (int i = 0; i < 8; i++) {
                            parent.add(new AmbientLight());
                        }
                    } else {
                        parent.clear();
                    }
                }
                return false;
            });
            Assertions.assertEquals(local.size(), calls[0]);
            Assertions.assertEquals(grow ? 9 : 0, world.size());
            for (int i = 0; i < parent.size(); i++) {
                Assertions.assertSame(parent.get(i), world.get(i));
            }
        }
    }

    private static Light[] backingArray(LightList list, String name) throws Exception {
        Field field = LightList.class.getDeclaredField(name);
        field.setAccessible(true);
        return (Light[]) field.get(list);
    }

    private static void assertScratchEmpty(LightList list) throws Exception {
        Light[] scratch = backingArray(list, "tlist");
        if (scratch != null) {
            for (int i = 0; i < scratch.length; i++) {
                Assertions.assertNull(scratch[i], "Scratch reference at index " + i);
            }
        }
    }

    private static void assertScratchEmptyUnchecked(LightList list) {
        try {
            assertScratchEmpty(list);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private static class CountingPointLight extends PointLight {
        private int distanceUpdates;

        CountingPointLight(Vector3f position) {
            super(position);
        }

        @Override
        public void computeLastDistance(Spatial owner) {
            distanceUpdates++;
            super.computeLastDistance(owner);
        }
    }
}
