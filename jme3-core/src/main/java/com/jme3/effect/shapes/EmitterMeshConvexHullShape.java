/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package com.jme3.effect.shapes;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import java.util.List;

/**
 * This emiter shape emits the particles from the given shape's interior constrained by its convex hull
 * (a geometry that tightly wraps the mesh). So in case of multiple meshes some vertices may appear
 * in a space between them.
 * @author Marcin Roguski (Kaelthas)
 */
public class EmitterMeshConvexHullShape extends EmitterMeshFaceShape {

    /**
     * Empty constructor. Sets nothing.
     */
    public EmitterMeshConvexHullShape() {
    }

    /**
     * Constructor. It stores a copy of vertex list of all meshes.
     * @param meshes
     *        a list of meshes that will form the emitter's shape
     */
    public EmitterMeshConvexHullShape(List<Mesh> meshes) {
        super(meshes);
    }

    /**
     * This method fills the point with coordinates of randomly selected point inside a convex hull
     * of randomly selected mesh.
     * @param store
     *        the variable to store with coordinates of randomly selected selected point inside a convex hull
     *        of randomly selected mesh
     */
    @Override
    public void getRandomPoint(Vector3f store) {
        super.getRandomPoint(store);
        // now move the point from the meshe's face towards the center of the mesh
        // the center is in (0, 0, 0) in the local coordinates
        store.multLocal(FastMath.nextRandomFloat());
    }

    /**
     * This method fills the point with coordinates of randomly selected point inside a convex hull
     * of randomly selected mesh.
     * The normal param is not used.
     * @param store
     *        the variable to store with coordinates of randomly selected selected point inside a convex hull
     *        of randomly selected mesh
     * @param normal
     *        not used in this class
     */
    @Override
    public void getRandomPointAndNormal(Vector3f store, Vector3f normal) {
        super.getRandomPointAndNormal(store, normal);
        // now move the point from the meshe's face towards the center of the mesh
        // the center is in (0, 0, 0) in the local coordinates
        store.multLocal(FastMath.nextRandomFloat());
    }
}
