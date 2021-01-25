/*
 * Copyright (c) 2009-2020 jMonkeyEngine
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
package com.jme3.math;

import com.jme3.collision.Collidable;
import com.jme3.collision.CollisionResults;

/**
 * A Collidable with a triangular shape.
 */
public abstract class AbstractTriangle implements Collidable {
    /**
     * Determine the location of the first vertex.
     *
     * @return a location vector
     */
    public abstract Vector3f get1();

    /**
     * Determine the location of the 2nd vertex.
     *
     * @return a location vector
     */
    public abstract Vector3f get2();

    /**
     * Determine the location of the 3rd vertex.
     *
     * @return a location vector
     */
    public abstract Vector3f get3();

    /**
     * Alter all 3 vertex locations.
     *
     * @param v1 the location for the first vertex
     * @param v2 the location for the 2nd vertex
     * @param v3 the location for the 3rd vertex
     */
    public abstract void set(Vector3f v1, Vector3f v2, Vector3f v3);

    /**
     * Generate collision results for this triangle with another Collidable.
     *
     * @param other the other Collidable
     * @param results storage for collision results
     * @return the number of collisions found
     */
    @Override
    public int collideWith(Collidable other, CollisionResults results) {
        return other.collideWith(this, results);
    }
}
