/*
 * Copyright (c) 2023 jMonkeyEngine
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.scene.mesh.MorphTarget;
import com.jme3.scene.shape.Box;
import com.jme3.util.BufferUtils;
import com.jme3.util.clone.Cloner;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests independent per-geometry morph weights after scene cloning. */
public class GeometryMorphStateCloneTest {

    /** The supported scene-cloning paths exercised by these tests. */
    enum CloneMode {
        MATERIALS, SHARED_MATERIALS, DEEP, SUBTREE
    }

    /**
     * Tests copying initialized weights and independent array-setter mutations.
     *
     * @param mode the cloning path to exercise
     */
    @ParameterizedTest
    @EnumSource(CloneMode.class)
    public void arraySetterDoesNotChangeTheOtherGeometry(CloneMode mode) {
        Geometry source = createGeometry();
        source.setMorphState(new float[]{0.2f, 0.4f});
        source.setDirtyMorph(false);
        Geometry clone = cloneGeometry(source, mode);
        assertArrayEquals(new float[]{0.2f, 0.4f}, clone.getMorphState());
        assertFalse(clone.isDirtyMorph());

        clone.setMorphState(new float[]{0.8f, 0.6f});
        assertArrayEquals(new float[]{0.2f, 0.4f}, source.getMorphState());
        assertArrayEquals(new float[]{0.8f, 0.6f}, clone.getMorphState());
        assertFalse(source.isDirtyMorph());
        assertTrue(clone.isDirtyMorph());
        assertNotSame(source.getMorphState(), clone.getMorphState());

        clone.setDirtyMorph(false);
        source.setMorphState(new float[]{0.1f, 0.3f});
        assertArrayEquals(new float[]{0.8f, 0.6f}, clone.getMorphState());
        assertFalse(clone.isDirtyMorph());
        assertTrue(source.isDirtyMorph());
    }

    /**
     * Tests named setters, including a second clone of an existing clone.
     *
     * @param mode the cloning path to exercise
     */
    @ParameterizedTest
    @EnumSource(CloneMode.class)
    public void namedSetterKeepsEachCloneIndependent(CloneMode mode) {
        Geometry source = createGeometry();
        source.setMorphState("smile", 0.2f);
        source.setMorphState("blink", 0.4f);
        Geometry clone = cloneGeometry(source, mode);
        Geometry secondClone = cloneGeometry(clone, mode);

        clone.setMorphState("smile", 0.8f);
        secondClone.setMorphState("blink", 0.6f);
        assertArrayEquals(new float[]{0.2f, 0.4f}, source.getMorphState());
        assertArrayEquals(new float[]{0.8f, 0.4f}, clone.getMorphState());
        assertArrayEquals(new float[]{0.2f, 0.6f}, secondClone.getMorphState());
    }

    /**
     * Tests arrays initialized by a getter before cloning.
     *
     * @param mode the cloning path to exercise
     */
    @ParameterizedTest
    @EnumSource(CloneMode.class)
    public void lazilyInitializedWeightsAreIndependent(CloneMode mode) {
        Geometry source = createGeometry();
        assertArrayEquals(new float[]{0f, 0f}, source.getMorphState());
        Geometry clone = cloneGeometry(source, mode);
        clone.setMorphState("blink", 0.7f);
        assertEquals(0f, source.getMorphState("blink"));
        assertEquals(0.7f, clone.getMorphState("blink"));
    }

    /**
     * Tests the already-working uninitialized-weight control.
     *
     * @param mode the cloning path to exercise
     */
    @ParameterizedTest
    @EnumSource(CloneMode.class)
    public void uninitializedWeightsRemainIndependent(CloneMode mode) {
        Geometry source = createGeometry();
        Geometry clone = cloneGeometry(source, mode);
        source.setMorphState("smile", 0.3f);
        clone.setMorphState("blink", 0.9f);
        assertArrayEquals(new float[]{0.3f, 0f}, source.getMorphState());
        assertArrayEquals(new float[]{0f, 0.9f}, clone.getMorphState());
    }

    /**
     * Tests that the Cloner preserves graph references to the copied weight array.
     *
     * @param arrayFirst whether to clone the array before the geometry
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void preservesWeightArrayReferencesWithinTheClonedGraph(boolean arrayFirst) {
        Geometry source = createGeometry();
        source.setMorphState(new float[]{0.2f, 0.4f});
        int geometryIndex = arrayFirst ? 1 : 0;
        int arrayIndex = arrayFirst ? 0 : 1;
        Object[] graph = new Object[2];
        graph[geometryIndex] = source;
        graph[arrayIndex] = source.getMorphState();

        Object[] clonedGraph = new Cloner().clone(graph);
        Geometry clone = (Geometry) clonedGraph[geometryIndex];
        assertSame(clonedGraph[arrayIndex], clone.getMorphState());
        assertNotSame(source.getMorphState(), clone.getMorphState());
        clone.setMorphState("smile", 0.8f);
        assertEquals(0.2f, source.getMorphState("smile"));
    }

    private static Geometry createGeometry() {
        Mesh mesh = new Box(1f, 1f, 1f);
        MorphTarget smile = new MorphTarget("smile");
        smile.setBuffer(VertexBuffer.Type.Position,
                BufferUtils.createFloatBuffer(mesh.getVertexCount() * 3));
        MorphTarget blink = new MorphTarget("blink");
        blink.setBuffer(VertexBuffer.Type.Position,
                BufferUtils.createFloatBuffer(mesh.getVertexCount() * 3));
        mesh.addMorphTarget(smile);
        mesh.addMorphTarget(blink);
        return new Geometry("face", mesh);
    }

    private static Geometry cloneGeometry(Geometry source, CloneMode mode) {
        switch (mode) {
            case MATERIALS:
                return source.clone();
            case SHARED_MATERIALS:
                return source.clone(false);
            case DEEP:
                return (Geometry) source.deepClone();
            case SUBTREE:
                Node root = new Node("model");
                root.attachChild(source);
                Node clonedRoot = root.clone(true);
                return (Geometry) clonedRoot.getChild("face");
            default:
                throw new AssertionError(mode);
        }
    }
}
