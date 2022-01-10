/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
package com.jme3.collision;

import com.jme3.bounding.BoundingVolume;

/**
 * Utilities for testing collision.
 * 
 * @author Kirill Vainer
 */
final class CollisionUtil {

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private CollisionUtil() {
    }

    private static void checkCollisionBase(Collidable a, Collidable b, int expected) {
        // Test bounding volume methods
        if (a instanceof BoundingVolume && b instanceof BoundingVolume) {
            BoundingVolume bv1 = (BoundingVolume) a;
            BoundingVolume bv2 = (BoundingVolume) b;
            assert bv1.intersects(bv2) == (expected != 0);
        }

        // Test standard collideWith method
        CollisionResults results = new CollisionResults();
        int numCollisions = a.collideWith(b, results);
        assert results.size() == numCollisions;
        assert numCollisions == expected;

        // Force the results to be sorted here.
        results.getClosestCollision();

        if (results.size() > 0) {
            assert results.getCollision(0) == results.getClosestCollision();
        }
        if (results.size() == 1) {
            assert results.getClosestCollision() == results.getFarthestCollision();
        }
    }
    
    /**
     * Tests various collisions between the two collidables and 
     * the transitive property.
     * 
     * @param a First collidable
     * @param b Second collidable
     * @param expected the expected number of results
     */
    public static void checkCollision(Collidable a, Collidable b, int expected) {
        checkCollisionBase(a, b, expected);
        checkCollisionBase(b, a, expected);
    }
}
