/*
 * Copyright (c) 2009-2025 jMonkeyEngine
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.scene.mesh.MorphTarget;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the name-based morph state accessors work on a geometry whose
 * morph state array has not been allocated yet.
 *
 * <p>The array is allocated lazily, and before this was fixed only
 * {@link Geometry#setMorphState(float[])} and {@link Geometry#getMorphState()}
 * did so. The name-based overloads indexed it directly, so calling either of
 * them first threw a {@link NullPointerException} -- which is exactly what
 * happens when morph targets are driven from application code rather than by a
 * morph animation, since nothing else has touched the array by then.
 *
 * @author jMonkeyEngine
 */
public class GeometryMorphStateTest {

    private static final String FIRST = "first";
    private static final String SECOND = "second";

    /**
     * A geometry with two named morph targets and no morph state array yet.
     * Geometries start out dirty, so the flag is cleared to let the tests below
     * observe whether a call actually sets it.
     */
    private static Geometry geometryWithMorphTargets() {
        Mesh mesh = new Mesh();
        mesh.addMorphTarget(new MorphTarget(FIRST));
        mesh.addMorphTarget(new MorphTarget(SECOND));
        Geometry geometry = new Geometry("test", mesh);
        geometry.setDirtyMorph(false);
        return geometry;
    }

    /**
     * Sets a morph state by name on an otherwise untouched geometry.
     */
    @Test
    public void setMorphStateByNameAllocatesTheStateArray() {
        Geometry geometry = geometryWithMorphTargets();

        geometry.setMorphState(SECOND, 0.75f);

        assertEquals(0.75f, geometry.getMorphState(SECOND), 0f);
        assertEquals(0f, geometry.getMorphState(FIRST), 0f);
        assertTrue(geometry.isDirtyMorph());
    }

    /**
     * Reads a morph state by name on an otherwise untouched geometry.
     */
    @Test
    public void getMorphStateByNameAllocatesTheStateArray() {
        Geometry geometry = geometryWithMorphTargets();

        assertEquals(0f, geometry.getMorphState(FIRST), 0f);
        assertEquals(2, geometry.getMorphState().length);
    }

    /**
     * An unknown morph name is ignored on write and reports -1 on read, and must
     * not disturb the states that do exist.
     */
    @Test
    public void unknownMorphNameIsIgnored() {
        Geometry geometry = geometryWithMorphTargets();

        geometry.setMorphState("nonexistent", 1f);

        assertFalse(geometry.isDirtyMorph());
        assertEquals(-1f, geometry.getMorphState("nonexistent"), 0f);
        assertEquals(0f, geometry.getMorphState(FIRST), 0f);
        assertEquals(0f, geometry.getMorphState(SECOND), 0f);
    }
}
