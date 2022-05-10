/*
 Copyright (c) 2019 jMonkeyEngine
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
package com.jme3.bullet.animation;

import com.jme3.math.Vector3f;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A simplified collection of Vector3f values without duplicates, implemented
 * using a Collection.
 * <p>
 * This class is shared between JBullet and Native Bullet.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class VectorSet {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(VectorSet.class.getName());
    // *************************************************************************
    // fields

    /**
     * collection of values
     */
    final private Set<Vector3f> set;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an empty set with the specified initial capacity and default
     * load factor.
     *
     * @param numVectors the initial capacity of the hash table (&gt;0)
     */
    public VectorSet(int numVectors) {
        set = new HashSet<>(numVectors);
    }
    // *************************************************************************
    // VectorSet methods

    /**
     * Add the value of the specified Vector3f to this set.
     *
     * @param vector the value to add (not null, unaffected)
     */
    public void add(Vector3f vector) {
        set.add(vector.clone());
    }

    /**
     * Test whether this set contains the value of the specified Vector3f.
     *
     * @param vector the value to find (not null, unaffected)
     * @return true if found, otherwise false
     */
    public boolean contains(Vector3f vector) {
        boolean result = set.contains(vector);
        return result;
    }

    /**
     * Calculate the sample mean for each axis over the Vector3f values in this
     * set.
     *
     * @param storeResult (modified if not null)
     * @return the sample mean for each axis (either storeResult or a new
     * Vector3f)
     */
    public Vector3f mean(Vector3f storeResult) {
        int numVectors = numVectors();
        assert numVectors > 0 : numVectors;
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;

        result.zero();
        for (Vector3f tempVector : set) {
            result.addLocal(tempVector);
        }
        result.divideLocal(numVectors);

        return result;
    }

    /**
     * Calculate the number of Vector3f values in this set.
     *
     * @return the count (&ge;0)
     */
    public int numVectors() {
        int numVectors = set.size();
        assert numVectors >= 0 : numVectors;
        return numVectors;
    }

    /**
     * Access the buffer containing all the Vector3f values in this set. No
     * further add() is allowed.
     *
     * @return a new buffer, flipped
     */
    public FloatBuffer toBuffer() {
        int numFloats = 3 * set.size();
        FloatBuffer buffer = BufferUtils.createFloatBuffer(numFloats);
        for (Vector3f tempVector : set) {
            buffer.put(tempVector.x);
            buffer.put(tempVector.y);
            buffer.put(tempVector.z);
        }
        buffer.flip();

        return buffer;
    }
}
