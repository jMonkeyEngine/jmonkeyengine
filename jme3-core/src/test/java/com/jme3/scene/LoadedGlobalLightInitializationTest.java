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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.export.JmeImporter;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;
import com.jme3.light.DefaultLightFilter;
import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.light.PointLight;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.shape.Box;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Tests initial light discovery in a deserialized, standalone Node root. */
public class LoadedGlobalLightInitializationTest {

    /**
     * Exercises root, node, and geometry owners at different depths and sibling orders.
     *
     * @param depth the depth of the source branch
     * @param owner the global light owner
     * @param observerFirst whether the observer precedes the source branch
     */
    @ParameterizedTest(name = "depth {0}, owner {1}, observer first {2}")
    @MethodSource("sceneConfigurations")
    public void standaloneLoadedRootDiscoversGlobals(int depth, String owner, boolean observerFirst) {
        Node source = scene(depth, owner, observerFirst);
        update(source);
        Node loaded = BinaryExporter.saveAndLoad(null, source);
        assertNull(loaded.getParent());
        List<Light> globals = new ArrayList<>();
        gatherGlobals(loaded, globals);
        assertEquals(1, globals.size(), "The global flag must survive serialization");
        for (int frame = 0; frame < 4; frame++) {
            updateAndVerify(loaded);
        }
    }

    @Test
    public void rootAndSeparateBranchesRestoreAllGlobalLights() {
        Node root = scene(4, "node", true);
        addLight(root, "root-global", true, true);
        Geometry geometry = new Geometry("other-source", new Box(1, 1, 1));
        addLight(geometry, "geometry-global", true, true);
        root.attachChild(geometry);
        update(root);
        Node loaded = BinaryExporter.saveAndLoad(null, root);
        List<Light> globals = new ArrayList<>();
        gatherGlobals(loaded, globals);
        assertEquals(3, globals.size());
        updateAndVerify(loaded);
    }

    @Test
    public void localOnlyLightsKeepAncestorInheritance() {
        Node root = scene(3, "none", false);
        addLight(root, "root-local", false, true);
        addLight(root.getChild("branch"), "branch-local", false, true);
        addLight(root.getChild("source-geometry"), "geometry-local", false, true);
        Node loaded = BinaryExporter.saveAndLoad(null, root);
        updateAndVerify(loaded);
        assertEquals(1, loaded.getChild("observer").getWorldLightList().size());
        assertEquals(3, loaded.getChild("source-geometry").getWorldLightList().size());
    }

    @Test
    public void rootLocalLightIsRetainedWithDescendantGlobalLight() {
        Node root = scene(4, "geometry", false);
        addLight(root, "root-local", false, true);
        addLight(root.getChild("branch"), "branch-local", false, true);
        updateAndVerify(BinaryExporter.saveAndLoad(null, root));
    }

    @Test
    public void disabledGlobalRemainsInWorldListsButIsFiltered() {
        Node root = scene(3, "node", true);
        addLight(root.getChild("source-geometry"), "disabled-global", true, false);
        Node loaded = BinaryExporter.saveAndLoad(null, root);
        List<Light> globals = new ArrayList<>();
        gatherGlobals(loaded, globals);
        assertEquals(2, globals.size());
        assertFalse(globals.get(1).isEnabled());
        updateAndVerify(loaded);
        assertEquals(2, loaded.getChild("observer").getWorldLightList().size());
    }

    @Test
    public void emptyRootAndEmptyBranchesRemainEmpty() {
        updateAndVerify(BinaryExporter.saveAndLoad(null, new Node("empty")));
        updateAndVerify(BinaryExporter.saveAndLoad(null, scene(8, "none", true)));
    }

    @Test
    public void explicitExporterImporterRoundTripInitializesStandaloneRoot() throws IOException {
        Node root = scene(3, "geometry", false);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        BinaryExporter.getInstance().save(root, output);
        Node loaded = (Node) new BinaryImporter().load(new ByteArrayInputStream(output.toByteArray()));
        updateAndVerify(loaded);
    }

    @Test
    public void attachingLoadedTreeBeforeFirstUpdateInitializesHost() {
        final Node loaded = BinaryExporter.saveAndLoad(null, scene(3, "node", true));
        Node host = new Node("host");
        host.attachChild(new Geometry("host-observer", new Box(1, 1, 1)));
        addLight(host, "host-global", true, true);
        update(host);
        host.attachChild(loaded);
        updateAndVerify(host);
    }

    @Test
    public void loadedTreeCanMoveBeforeItsFirstUpdate() {
        Node loaded = BinaryExporter.saveAndLoad(null, scene(3, "geometry", true));
        Node firstHost = new Node("first-host");
        Node secondHost = new Node("second-host");
        firstHost.attachChild(loaded);
        secondHost.attachChild(loaded);
        updateAndVerify(firstHost);
        updateAndVerify(secondHost);
    }

    @Test
    public void cloneOfLoadedTreeInitializesIndependently() {
        Node loaded = BinaryExporter.saveAndLoad(null, scene(3, "node", false));
        Node cloned = (Node) loaded.clone();
        updateAndVerify(cloned);
        updateAndVerify(loaded);
    }

    @Test
    public void secondRoundTripAlsoInitializesStandaloneRoot() {
        Node first = BinaryExporter.saveAndLoad(null, scene(3, "geometry", true));
        updateAndVerify(first);
        Node second = BinaryExporter.saveAndLoad(null, first);
        updateAndVerify(second);
        updateAndVerify(first);
    }

    @Test
    public void readingDoesNotInvokeSpatialRefreshOverrides() {
        LoadGuardNode root = new LoadGuardNode("guard-root");
        LoadGuardNode child = new LoadGuardNode("guard-child");
        root.attachChild(child);
        addLight(root, "root-global", true, true);
        addLight(child, "child-global", true, true);
        updateAndVerify(BinaryExporter.saveAndLoad(null, root));
    }

    @Test
    public void controlsStillReadAfterChildrenAreLinked() {
        Node root = scene(3, "node", true);
        root.addControl(new ChildCheckingControl());
        Node loaded = BinaryExporter.saveAndLoad(null, root);
        ChildCheckingControl control = loaded.getControl(ChildCheckingControl.class);
        assertNotNull(control);
        assertTrue(control.sawLinkedChildren);
        updateAndVerify(loaded);
    }

    /**
     * Counts global checks during loading, before normal frame-time collection.
     *
     * @param depth the number of nested nodes
     */
    @ParameterizedTest(name = "load depth {0}")
    @MethodSource("loadDepths")
    public void readingScansOnlyEachLocalLightList(int depth) {
        Node root = new Node("count-root");
        Node current = root;
        for (int i = 0; i < depth; i++) {
            Node child = new Node("count-" + i);
            current.attachChild(child);
            CountingPointLight local = new CountingPointLight();
            CountingPointLight global = new CountingPointLight(true);
            child.addLight(local);
            child.addLight(global);
            current = child;
        }
        CountingPointLight.globalChecks = 0;
        Node loaded = BinaryExporter.saveAndLoad(null, root);
        assertTrue(CountingPointLight.globalChecks <= 2 * depth,
                "Loading must not rescan already-read descendants");
        updateAndVerify(loaded);
        CountingPointLight.globalChecks = 0;
        for (int frame = 0; frame < 4; frame++) {
            update(loaded);
        }
        assertEquals(0, CountingPointLight.globalChecks, "Unchanged updates must not rescan lights");
    }

    private static Stream<Arguments> sceneConfigurations() {
        List<Arguments> configurations = new ArrayList<>();
        for (int depth : new int[]{1, 4, 12}) {
            for (String owner : new String[]{"root", "node", "geometry"}) {
                configurations.add(Arguments.of(depth, owner, true));
                configurations.add(Arguments.of(depth, owner, false));
            }
        }
        return configurations.stream();
    }

    private static Stream<Integer> loadDepths() {
        return Stream.of(1, 16, 64);
    }

    private static Node scene(int depth, String owner, boolean observerFirst) {
        Node root = new Node("root");
        Node branch = new Node("branch");
        Node source = branch;
        for (int i = 1; i < depth; i++) {
            Node child = new Node("source-" + i);
            source.attachChild(child);
            source = child;
        }
        Geometry geometry = new Geometry("source-geometry", new Box(1, 1, 1));
        source.attachChild(geometry);
        Geometry observer = new Geometry("observer", new Box(1, 1, 1));
        root.attachChild(observerFirst ? observer : branch);
        root.attachChild(observerFirst ? branch : observer);
        if (!"none".equals(owner)) {
            Spatial lightOwner = "root".equals(owner) ? root : "node".equals(owner) ? source : geometry;
            addLight(lightOwner, "global", true, true);
        }
        return root;
    }

    private static void addLight(Spatial owner, String name, boolean global, boolean enabled) {
        PointLight light = new PointLight(global);
        light.setName(name);
        light.setEnabled(enabled);
        owner.addLight(light);
    }

    private static void update(Node root) {
        root.updateLogicalState(0.016f);
        root.updateGeometricState();
    }

    private static void updateAndVerify(Node root) {
        update(root);
        List<Light> globals = new ArrayList<>();
        gatherGlobals(root, globals);
        verifyTree(root, globals, Collections.emptyList());
    }

    private static void gatherGlobals(Spatial spatial, List<Light> globals) {
        for (Light light : spatial.getLocalLightList()) {
            if (light.isGlobal()) {
                globals.add(light);
            }
        }
        if (spatial instanceof Node) {
            for (Spatial child : ((Node) spatial).getChildren()) {
                gatherGlobals(child, globals);
            }
        }
    }

    private static void verifyTree(Spatial spatial, List<Light> globals, List<Light> inherited) {
        List<Light> locals = new ArrayList<>(inherited);
        for (Light light : spatial.getLocalLightList()) {
            if (!light.isGlobal()) {
                locals.add(light);
            }
        }
        List<Light> expected = new ArrayList<>(globals);
        expected.addAll(locals);
        assertLights(spatial.getWorldLightList(), expected, spatial.getName());
        if (spatial instanceof Node) {
            for (Spatial child : ((Node) spatial).getChildren()) {
                assertSame(spatial, child.getParent());
                verifyTree(child, globals, locals);
            }
        } else if (spatial instanceof Geometry) {
            Geometry geometry = (Geometry) spatial;
            DefaultLightFilter filter = new DefaultLightFilter();
            filter.setCamera(new Camera(64, 64));
            LightList filtered = new LightList(geometry);
            filter.filterLights(geometry, filtered);
            expected.removeIf(light -> !light.isEnabled());
            assertLights(filtered, expected, "filtered " + spatial.getName());
        }
    }

    private static void assertLights(LightList actual, List<Light> expected, String context) {
        assertEquals(expected.size(), actual.size(), context);
        for (Light light : expected) {
            int occurrences = 0;
            for (Light candidate : actual) {
                if (candidate == light) {
                    occurrences++;
                }
            }
            assertEquals(1, occurrences, context + ": " + light.getName());
        }
    }

    /** A serializable probe for overridable refresh calls during partial loading. */
    public static class LoadGuardNode extends Node {
        private boolean initialized;

        public LoadGuardNode() {
        }

        LoadGuardNode(String name) {
            super(name);
            initialized = true;
        }

        @Override
        protected boolean hasGlobalLights() {
            assertTrue(initialized, "Do not query subclass state during superclass read");
            return super.hasGlobalLights();
        }

        @Override
        protected void setLightListRefresh() {
            assertTrue(initialized, "Do not invoke subclass refresh during superclass read");
            super.setLightListRefresh();
        }

        @Override
        protected void setTransformRefresh() {
            assertTrue(initialized, "Do not invoke subclass transform refresh during superclass read");
            super.setTransformRefresh();
        }

        @Override
        protected void setBoundRefresh() {
            assertTrue(initialized, "Do not invoke subclass bound refresh during superclass read");
            super.setBoundRefresh();
        }

        @Override
        protected void setMatParamOverrideRefresh() {
            assertTrue(initialized, "Do not invoke subclass override refresh during superclass read");
            super.setMatParamOverrideRefresh();
        }

        @Override
        public void read(JmeImporter importer) throws IOException {
            super.read(importer);
            initialized = true;
        }
    }

    /** A serializable counter for local-list scanning during load. */
    public static class CountingPointLight extends PointLight {
        static int globalChecks;

        public CountingPointLight() {
        }

        CountingPointLight(boolean global) {
            super(global);
        }

        @Override
        public boolean isGlobal() {
            globalChecks++;
            return super.isGlobal();
        }
    }

    /** A control that validates Node's established children-before-controls ordering. */
    public static class ChildCheckingControl extends AbstractControl {
        boolean sawLinkedChildren;

        public ChildCheckingControl() {
        }

        @Override
        public void read(JmeImporter importer) throws IOException {
            super.read(importer);
            Node node = (Node) spatial;
            assertEquals(2, node.getQuantity());
            for (Spatial child : node.getChildren()) {
                assertSame(node, child.getParent());
            }
            sawLinkedChildren = true;
        }

        @Override
        protected void controlUpdate(float tpf) {
        }

        @Override
        protected void controlRender(RenderManager renderManager, ViewPort viewPort) {
        }
    }
}
