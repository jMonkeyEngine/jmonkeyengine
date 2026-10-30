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
package com.jme3.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;
import com.jme3.light.DefaultLightFilter;
import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.light.PointLight;
import com.jme3.renderer.Camera;
import com.jme3.scene.shape.Box;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Verifies that global lights follow the complete scene tree across light-list
 * and attachment changes, using the public scene update lifecycle.
 */
public class GlobalLightLifecycleTest {

    /**
     * Tests incremental collection independently of branch depth and sibling order.
     *
     * @param depth the source depth
     * @param order the sibling attachment order
     */
    @ParameterizedTest(name = "depth {0}, order {1}")
    @MethodSource("treeConfigurations")
    public void incrementalGlobalAdditionRetainsCleanBranches(int depth, String order) {
        Scene scene = new Scene(depth, order);
        update(scene.root);
        scene.first.addLight(scene.firstLight);
        update(scene.root);
        assertLights(scene.observer, scene.firstLight);
        scene.second.addLight(scene.secondLight);
        update(scene.root);
        assertTreeLights(scene.root, scene.firstLight, scene.secondLight);
        assertFilteredLights(scene.observer, scene.firstLight, scene.secondLight);
        for (int frame = 0; frame < 3; frame++) {
            update(scene.root);
            assertTreeLights(scene.root, scene.firstLight, scene.secondLight);
        }
    }

    @Test
    public void removingGlobalLightRetainsCleanSiblingAndClearsLastLight() {
        Scene scene = new Scene(3, "ABT");
        scene.addBothLights();
        scene.second.removeLight(scene.secondLight);
        update(scene.root);
        assertTreeLights(scene.root, scene.firstLight);
        scene.first.removeLight(scene.firstLight);
        update(scene.root);
        assertTreeLights(scene.root);
        update(scene.root);
        assertTreeLights(scene.root);
    }

    @Test
    public void localLightEditOnGlobalSourcePreservesOtherBranches() {
        Scene scene = new Scene(3, "ABT");
        scene.addBothLights();
        PointLight local = new PointLight();
        scene.second.addLight(local);
        update(scene.root);
        assertLights(scene.root, scene.firstLight, scene.secondLight);
        assertLights(scene.observer, scene.firstLight, scene.secondLight);
        assertLights(scene.second, scene.firstLight, scene.secondLight, local);
        scene.second.removeLight(local);
        update(scene.root);
        assertTreeLights(scene.root, scene.firstLight, scene.secondLight);
    }

    @Test
    public void localLightsStillFollowOnlyTheirAncestors() {
        Scene scene = new Scene(3, "BAT");
        PointLight rootLocal = new PointLight();
        scene.first.addLight(scene.firstLight);
        update(scene.root);
        scene.root.addLight(rootLocal);
        PointLight branchLocal = new PointLight();
        scene.second.addLight(branchLocal);
        update(scene.root);
        assertLights(scene.root, rootLocal, scene.firstLight);
        assertLights(scene.first, rootLocal, scene.firstLight);
        assertLights(scene.second, branchLocal, rootLocal, scene.firstLight);
        assertLights(scene.observer, rootLocal, scene.firstLight);
    }

    @Test
    public void geometryCanSupplyGlobalLights() {
        Scene scene = new Scene(3, "TBA");
        Geometry source = new Geometry("source", new Box(1, 1, 1));
        scene.first.attachChild(source);
        source.addLight(scene.firstLight);
        update(scene.root);
        scene.second.addLight(scene.secondLight);
        update(scene.root);
        assertTreeLights(scene.root, scene.firstLight, scene.secondLight);
        source.removeFromParent();
        update(scene.root);
        assertTreeLights(scene.root, scene.secondLight);
    }

    @Test
    public void rootCanSupplyGlobalLights() {
        Scene scene = new Scene(3, "TBA");
        scene.root.addLight(scene.firstLight);
        update(scene.root);
        scene.second.addLight(scene.secondLight);
        update(scene.root);
        assertTreeLights(scene.root, scene.firstLight, scene.secondLight);
        scene.root.removeLight(scene.firstLight);
        update(scene.root);
        assertTreeLights(scene.root, scene.secondLight);
    }

    @Test
    public void detachingDirectSourceRefreshesBothTrees() {
        Scene scene = new Scene(1, "ABT");
        scene.addBothLights();
        assertSame(scene.firstBranch, scene.root.detachChildAt(0));
        update(scene.root);
        update(scene.firstBranch);
        assertTreeLights(scene.root, scene.secondLight);
        assertTreeLights(scene.firstBranch, scene.firstLight);
    }

    @Test
    public void detachingNestedSourceRefreshesBothTrees() {
        Scene scene = new Scene(3, "ABT");
        scene.addBothLights();
        scene.root.detachChild(scene.firstBranch);
        update(scene.root);
        update(scene.firstBranch);
        assertTreeLights(scene.root, scene.secondLight);
        assertTreeLights(scene.firstBranch, scene.firstLight);
    }

    @Test
    public void reparentingWithinTreePreservesCleanGlobalLights() {
        Scene scene = new Scene(3, "ABT");
        scene.addBothLights();
        scene.targetBranch.attachChild(scene.firstBranch);
        update(scene.root);
        assertTreeLights(scene.root, scene.firstLight, scene.secondLight);
    }

    @Test
    public void reparentingAcrossRootsRefreshesSourceAndDestination() {
        Scene previous = new Scene(3, "ABT");
        Scene destination = new Scene(3, "TBA");
        previous.addBothLights();
        destination.second.addLight(destination.secondLight);
        update(destination.root);
        destination.targetBranch.attachChild(previous.firstBranch);
        update(destination.root);
        update(previous.root);
        assertTreeLights(previous.root, previous.secondLight);
        assertTreeLights(destination.root, previous.firstLight, destination.secondLight);
    }

    @Test
    public void attachingAlreadyDirtySubtreeReplacesPreviousCollection() {
        Scene scene = new Scene(3, "TBA");
        scene.first.addLight(scene.firstLight);
        update(scene.root);
        Node subtree = new Node("subtree");
        Node nested = new Node("nested");
        nested.addLight(scene.secondLight);
        subtree.attachChild(nested);
        scene.second.attachChild(subtree);
        update(scene.root);
        assertTreeLights(scene.root, scene.firstLight, scene.secondLight);
        assertFilteredLights(scene.observer, scene.firstLight, scene.secondLight);
    }

    @Test
    public void reparentingAlreadyDirtySubtreeReplacesPreviousCollection() {
        Scene scene = new Scene(3, "TBA");
        scene.addBothLights();
        PointLight added = new PointLight(true);
        scene.first.addLight(added);
        scene.second.attachChild(scene.firstBranch);
        update(scene.root);
        assertTreeLights(scene.root, scene.firstLight, scene.secondLight, added);
    }

    @Test
    public void multipleChangesBeforeUpdateUseFinalMembership() {
        Scene scene = new Scene(3, "TBA");
        scene.addBothLights();
        PointLight replacement = new PointLight(true);
        scene.first.removeLight(scene.firstLight);
        scene.second.removeLight(scene.secondLight);
        scene.first.addLight(replacement);
        scene.root.detachChild(scene.secondBranch);
        scene.root.attachChild(scene.secondBranch);
        update(scene.root);
        assertTreeLights(scene.root, replacement);
    }

    @Test
    public void detachingAllChildrenClearsDescendantGlobalLights() {
        Scene scene = new Scene(3, "ABT");
        scene.addBothLights();
        PointLight rootLight = new PointLight(true);
        scene.root.addLight(rootLight);
        update(scene.root);
        scene.root.detachAllChildren();
        update(scene.root);
        assertLights(scene.root, rootLight);
        update(scene.firstBranch);
        update(scene.secondBranch);
        assertTreeLights(scene.firstBranch, scene.firstLight);
        assertTreeLights(scene.secondBranch, scene.secondLight);
    }

    @Test
    public void unchangedUpdatesDoNotCollectGlobalLightsAgain() {
        CountingNode root = new CountingNode();
        Node branch = new Node("branch");
        root.attachChild(branch);
        PointLight light = new PointLight(true);
        branch.addLight(light);
        update(root);
        int initialReads = root.localLightListReads;
        assertTrue(initialReads > 0);
        for (int frame = 0; frame < 5; frame++) {
            update(root);
            assertTreeLights(root, light);
        }
        assertEquals(initialReads, root.localLightListReads);
    }

    @Test
    public void cloneRebuildsGlobalsIndependently() {
        Scene original = new Scene(3, "ABT");
        original.addBothLights();
        Node cloned = (Node) original.root.clone();
        verifyCopiedScene(cloned);
        assertTreeLights(original.root, original.firstLight, original.secondLight);
    }

    @Test
    public void binaryRoundTripRetainsGlobalLifecycle() throws IOException {
        Scene original = new Scene(3, "ABT");
        original.addBothLights();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        BinaryExporter.getInstance().save(original.root, output);
        ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
        Node loaded = (Node) BinaryImporter.getInstance().load(input);
        Node host = new Node("host");
        host.attachChild(loaded);
        verifyCopiedScene(host);
    }

    @Test
    public void emptyRootAndLocalOnlyTreeRemainValid() {
        Node root = new Node("root");
        update(root);
        assertLights(root);
        Geometry child = new Geometry("child", new Box(1, 1, 1));
        root.attachChild(child);
        PointLight local = new PointLight();
        root.addLight(local);
        update(root);
        assertTreeLights(root, local);
        child.removeFromParent();
        update(root);
        update(child);
        assertLights(root, local);
        assertLights(child);
    }

    private static Stream<Arguments> treeConfigurations() {
        List<Arguments> configurations = new ArrayList<>();
        for (int depth : new int[]{1, 3}) {
            for (String order : new String[]{"ABT", "ATB", "BAT", "BTA", "TAB", "TBA"}) {
                configurations.add(Arguments.of(depth, order));
            }
        }
        return configurations.stream();
    }

    private static void verifyCopiedScene(Node root) {
        Node first = (Node) root.getChild("first-source");
        Node second = (Node) root.getChild("second-source");
        Light firstLight = first.getLocalLightList().get(0);
        Light secondLight = second.getLocalLightList().get(0);
        assertTrue(firstLight.isGlobal());
        assertTrue(secondLight.isGlobal());
        update(root);
        assertTreeLights(root, firstLight, secondLight);
        first.removeLight(firstLight);
        update(root);
        assertTreeLights(root, secondLight);
    }

    private static void update(Spatial root) {
        root.updateLogicalState(1f / 60f);
        root.updateGeometricState();
    }

    private static void assertFilteredLights(Geometry observer, Light... expected) {
        DefaultLightFilter filter = new DefaultLightFilter();
        filter.setCamera(new Camera(64, 64));
        LightList output = new LightList(observer);
        filter.filterLights(observer, output);
        assertLights(output, expected);
    }

    private static void assertTreeLights(Spatial spatial, Light... expected) {
        assertLights(spatial, expected);
        if (spatial instanceof Node) {
            for (Spatial child : ((Node) spatial).getChildren()) {
                assertTreeLights(child, expected);
            }
        }
    }

    private static void assertLights(Spatial spatial, Light... expected) {
        assertLights(spatial.getWorldLightList(), expected);
    }

    private static void assertLights(LightList actual, Light... expected) {
        assertEquals(expected.length, actual.size(), "Unexpected light-list size");
        for (Light light : expected) {
            int count = 0;
            for (Light entry : actual) {
                if (entry == light) {
                    count++;
                }
            }
            assertEquals(1, count, "Each expected light must occur exactly once");
        }
    }

    private static class Scene {
        final Node root = new Node("root");
        final Node firstBranch = new Node("first-branch");
        final Node secondBranch = new Node("second-branch");
        final Node targetBranch = new Node("target-branch");
        final Node first;
        final Node second;
        final Geometry observer = new Geometry("observer", new Box(1, 1, 1));
        final PointLight firstLight = new PointLight(true);
        final PointLight secondLight = new PointLight(true);

        Scene(int depth, String order) {
            first = addChain(firstBranch, "first", depth);
            second = addChain(secondBranch, "second", depth);
            addChain(targetBranch, "target", depth).attachChild(observer);
            for (char branch : order.toCharArray()) {
                root.attachChild(branch == 'A' ? firstBranch : branch == 'B' ? secondBranch : targetBranch);
            }
            // Zero radius is the public infinite-range mode, avoiding finite-radius filtering.
            firstLight.setRadius(0);
            secondLight.setRadius(0);
        }

        void addBothLights() {
            first.addLight(firstLight);
            second.addLight(secondLight);
            update(root);
            assertTreeLights(root, firstLight, secondLight);
        }

        private static Node addChain(Node top, String name, int depth) {
            for (int index = 1; index < depth; index++) {
                Node child = new Node(name + index);
                top.attachChild(child);
                top = child;
            }
            top.setName(name + "-source");
            return top;
        }
    }

    private static class CountingNode extends Node {
        int localLightListReads;

        @Override
        public LightList getLocalLightList() {
            localLightListReads++;
            return super.getLocalLightList();
        }
    }
}
