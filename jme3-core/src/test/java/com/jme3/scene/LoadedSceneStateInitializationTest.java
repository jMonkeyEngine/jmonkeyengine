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

import com.jme3.bounding.BoundingBox;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.material.MatParamOverride;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.shape.Box;
import com.jme3.shader.VarType;
import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Tests restoration of derived transforms, bounds, and material overrides after loading. */
public class LoadedSceneStateInitializationTest {

    /**
     * Tests composition of local transforms throughout a loaded tree.
     *
     * @param depth the number of nested source nodes
     */
    @ParameterizedTest(name = "depth {0}")
    @MethodSource("depths")
    public void loadedWorldTransformsAndBoundsMatchSavedScene(int depth) {
        Node source = transformedScene(depth);
        update(source);
        Node loaded = BinaryExporter.saveAndLoad(null, source);
        for (int frame = 0; frame < 3; frame++) {
            update(loaded);
            assertTransformsAndBounds(source, loaded);
        }
    }

    @Test
    public void standaloneRootTranslationIsRecomputedFromRestoredLocalValues() {
        Node source = new Node("root");
        Geometry child = new Geometry("child", new Box(1, 1, 1));
        source.attachChild(child);
        source.setLocalTranslation(3, 4, 5);
        child.setLocalTranslation(1, 0, 0);
        update(source);
        Node loaded = BinaryExporter.saveAndLoad(null, source);
        Spatial loadedChild = loaded.getChild("child");
        assertEquals(new Vector3f(3, 4, 5), loaded.getLocalTranslation());
        assertEquals(new Vector3f(1, 0, 0), loadedChild.getLocalTranslation());
        update(loaded);
        assertEquals(new Vector3f(3, 4, 5), loaded.getWorldTranslation());
        assertEquals(new Vector3f(4, 4, 5), loadedChild.getWorldTranslation());
        assertEquals(new Vector3f(4, 4, 5), loadedChild.getWorldBound().getCenter());
    }

    @Test
    public void materialOverridesRemainLocalAndInheritOnlyAlongTheirBranch() {
        Node source = transformedScene(3);
        Node branch = (Node) source.getChild("branch");
        source.addMatParamOverride(new MatParamOverride(VarType.Boolean, "UseVertexColor", true));
        branch.addMatParamOverride(new MatParamOverride(VarType.Float, "AlphaDiscardThreshold", 0.5f));
        update(source);
        Node loaded = BinaryExporter.saveAndLoad(null, source);
        assertEquals(1, loaded.getLocalMatParamOverrides().size());
        assertEquals(1, loaded.getChild("branch").getLocalMatParamOverrides().size());
        update(loaded);
        assertEquals(1, loaded.getWorldMatParamOverrides().size());
        assertEquals(1, loaded.getChild("observer").getWorldMatParamOverrides().size());
        assertEquals(2, loaded.getChild("source-geometry").getWorldMatParamOverrides().size());
        assertSame(loaded.getLocalMatParamOverrides().get(0),
                loaded.getChild("observer").getWorldMatParamOverrides().get(0));
        assertSame(loaded.getChild("branch").getLocalMatParamOverrides().get(0),
                loaded.getChild("source-geometry").getWorldMatParamOverrides().get(1));
    }

    @Test
    public void attachingBeforeFirstUpdateComposesWithHostTransformAndOverrides() {
        Node source = transformedScene(3);
        source.addMatParamOverride(new MatParamOverride(VarType.Float, "AlphaDiscardThreshold", 0.5f));
        Node loaded = BinaryExporter.saveAndLoad(null, source);
        Node expectedHost = new Node("host");
        Node actualHost = new Node("host");
        for (Node host : new Node[]{expectedHost, actualHost}) {
            host.setLocalTranslation(9, 8, 7);
            host.addMatParamOverride(new MatParamOverride(VarType.Boolean, "UseVertexColor", true));
            update(host);
        }
        expectedHost.attachChild(source);
        actualHost.attachChild(loaded);
        update(expectedHost);
        update(actualHost);
        assertTransformsAndBounds(expectedHost, actualHost);
        assertEquals(2, loaded.getChild("source-geometry").getWorldMatParamOverrides().size());
    }

    @Test
    public void loadedTreeCanBeClonedAndSerializedAgain() {
        Node source = transformedScene(3);
        source.addMatParamOverride(new MatParamOverride(VarType.Boolean, "UseVertexColor", true));
        update(source);
        Node loaded = BinaryExporter.saveAndLoad(null, source);
        Node cloned = (Node) loaded.clone();
        update(cloned);
        assertTransformsAndBounds(source, cloned);
        assertEquals(1, cloned.getChild("source-geometry").getWorldMatParamOverrides().size());
        update(loaded);
        Node second = BinaryExporter.saveAndLoad(null, loaded);
        update(second);
        assertTransformsAndBounds(source, second);
        assertEquals(1, second.getChild("source-geometry").getWorldMatParamOverrides().size());
    }

    @Test
    public void controlReadsStillSeeLoadedChildrenAndLocalState() {
        Node source = transformedScene(2);
        source.addMatParamOverride(new MatParamOverride(VarType.Boolean, "UseVertexColor", true));
        source.addControl(new LocalStateCheckingControl());
        Node loaded = BinaryExporter.saveAndLoad(null, source);
        assertTrue(loaded.getControl(LocalStateCheckingControl.class).sawLocalState);
        update(loaded);
        update(source);
        assertTransformsAndBounds(source, loaded);
    }

    /**
     * Tests world getters before a loaded subtree has acquired its outer parent.
     *
     * @param owner the spatial whose control queries world state while reading
     * @param target whether the control queries itself, a descendant, or both
     */
    @ParameterizedTest(name = "control owner {0}, query {1}")
    @MethodSource("getterConfigurations")
    public void controlWorldGettersDoNotConsumeTransformsBeforeParentInitialization(
            String owner, String target) {
        TransformGuardNode root = new TransformGuardNode("guard-root");
        TransformGuardNode branch = new TransformGuardNode("guard-branch");
        TransformGuardGeometry leaf = new TransformGuardGeometry("guard-leaf");
        root.attachChild(branch);
        branch.attachChild(leaf);
        root.setLocalTranslation(3, 4, 5);
        branch.setLocalTranslation(1, 2, 3);
        leaf.setLocalTranslation(4, 5, 6);
        Spatial controlOwner = "root".equals(owner) ? root : "branch".equals(owner) ? branch : leaf;
        controlOwner.addControl(new WorldGetterControl(target));
        update(root);
        Node loaded = BinaryExporter.saveAndLoad(null, root);
        Spatial loadedOwner = "root".equals(owner) ? loaded : loaded.getChild("guard-" + owner);
        assertTrue(loadedOwner.getControl(WorldGetterControl.class).queriedWorldTransforms);
        update(loaded);
        assertEquals(new Vector3f(8, 11, 14), loaded.getChild("guard-leaf").getWorldTranslation());
        assertTransformsAndBounds(root, loaded);
    }

    private static Stream<Arguments> getterConfigurations() {
        return Stream.of(
                Arguments.of("root", "self"),
                Arguments.of("root", "descendant"),
                Arguments.of("root", "both"),
                Arguments.of("branch", "self"),
                Arguments.of("branch", "descendant"),
                Arguments.of("branch", "both"),
                Arguments.of("leaf", "self"));
    }

    private static Stream<Integer> depths() {
        return Stream.of(1, 4, 12);
    }

    private static Node transformedScene(int depth) {
        Node root = new Node("root");
        root.setLocalTranslation(3, 4, 5);
        root.setLocalScale(1.5f, 2, 0.5f);
        root.setLocalRotation(new Quaternion().fromAngles(0.1f, 0.2f, 0.3f));
        Node current = root;
        for (int i = 0; i < depth; i++) {
            Node child = new Node(i == 0 ? "branch" : "nested-" + i);
            child.setLocalTranslation(1, 2, 3);
            child.setLocalScale(0.8f, 1.1f, 0.9f);
            child.setLocalRotation(new Quaternion().fromAngles(0.2f, 0.1f, 0.05f));
            current.attachChild(child);
            current = child;
        }
        current.attachChild(new Geometry("source-geometry", new Box(1, 2, 3)));
        root.attachChild(new Geometry("observer", new Box(2, 1, 1)));
        return root;
    }

    private static void update(Node root) {
        root.updateLogicalState(0.016f);
        root.updateGeometricState();
    }

    private static void assertTransformsAndBounds(Spatial expected, Spatial actual) {
        assertEquals(expected.getLocalTransform(), actual.getLocalTransform(), actual.getName());
        assertEquals(expected.getWorldTransform(), actual.getWorldTransform(), actual.getName());
        BoundingBox expectedBounds = (BoundingBox) expected.getWorldBound();
        BoundingBox actualBounds = (BoundingBox) actual.getWorldBound();
        assertEquals(expectedBounds.getCenter(), actualBounds.getCenter(), actual.getName());
        assertEquals(expectedBounds.getXExtent(), actualBounds.getXExtent(), actual.getName());
        assertEquals(expectedBounds.getYExtent(), actualBounds.getYExtent(), actual.getName());
        assertEquals(expectedBounds.getZExtent(), actualBounds.getZExtent(), actual.getName());
        if (expected instanceof Node) {
            Node expectedNode = (Node) expected;
            Node actualNode = (Node) actual;
            assertEquals(expectedNode.getQuantity(), actualNode.getQuantity());
            for (int i = 0; i < expectedNode.getQuantity(); i++) {
                assertTransformsAndBounds(expectedNode.getChild(i), actualNode.getChild(i));
            }
        }
    }

    /** A Node that detects virtual transform updates before its read has finished. */
    public static class TransformGuardNode extends Node {
        private boolean initialized;

        public TransformGuardNode() {
        }

        TransformGuardNode(String name) {
            super(name);
            initialized = true;
        }

        @Override
        protected void updateWorldTransforms() {
            assertTrue(initialized, "Do not update a partially read Node subclass");
            super.updateWorldTransforms();
        }

        @Override
        public void read(JmeImporter importer) throws IOException {
            super.read(importer);
            initialized = true;
        }
    }

    /** A Geometry that detects virtual transform updates before its read has finished. */
    public static class TransformGuardGeometry extends Geometry {
        private boolean initialized;

        public TransformGuardGeometry() {
        }

        TransformGuardGeometry(String name) {
            super(name, new Box(1, 1, 1));
            initialized = true;
        }

        @Override
        protected void updateWorldTransforms() {
            assertTrue(initialized, "Do not update a partially read Geometry subclass");
            super.updateWorldTransforms();
        }

        @Override
        public void read(JmeImporter importer) throws IOException {
            super.read(importer);
            initialized = true;
        }
    }

    /** A control that queries world transforms while its owner is being read. */
    public static class WorldGetterControl extends AbstractControl {
        boolean queriedWorldTransforms;
        private String target;

        public WorldGetterControl() {
        }

        WorldGetterControl(String target) {
            this.target = target;
        }

        @Override
        public void write(JmeExporter exporter) throws IOException {
            super.write(exporter);
            exporter.getCapsule(this).write(target, "target", "self");
        }

        @Override
        public void read(JmeImporter importer) throws IOException {
            super.read(importer);
            target = importer.getCapsule(this).readString("target", "self");
            if (!"descendant".equals(target)) {
                spatial.getWorldTranslation();
            }
            if (!"self".equals(target)) {
                ((Node) spatial).getChild("guard-leaf").getWorldTranslation();
            }
            queriedWorldTransforms = true;
        }

        @Override
        protected void controlUpdate(float tpf) {
        }

        @Override
        protected void controlRender(RenderManager renderManager, ViewPort viewPort) {
        }
    }

    /** A control that checks local data and child links at deserialization time. */
    public static class LocalStateCheckingControl extends AbstractControl {
        boolean sawLocalState;

        public LocalStateCheckingControl() {
        }

        @Override
        public void read(JmeImporter importer) throws IOException {
            super.read(importer);
            Node node = (Node) spatial;
            assertEquals(new Vector3f(3, 4, 5), node.getLocalTranslation());
            assertEquals(1, node.getLocalMatParamOverrides().size());
            assertEquals(2, node.getQuantity());
            for (Spatial child : node.getChildren()) {
                assertSame(node, child.getParent());
            }
            sawLocalState = true;
        }

        @Override
        protected void controlUpdate(float tpf) {
        }

        @Override
        protected void controlRender(RenderManager renderManager, ViewPort viewPort) {
        }
    }
}
