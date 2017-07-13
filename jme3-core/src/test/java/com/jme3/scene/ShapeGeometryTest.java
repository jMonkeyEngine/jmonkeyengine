/*
 * Copyright (c) 2009-2017 jMonkeyEngine
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

import com.jme3.collision.CollisionResults;
import com.jme3.math.FastMath;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.shape.Cylinder;
import java.util.Random;
import org.junit.Test;

/**
 * Ensures that geometries behave correctly, by casting rays and ensure they don't break.
 *
 * @author Christophe Carpentier
 */
public class ShapeGeometryTest {

    protected static final int NUMBER_OF_TRIES = 1000;
    
    @Test
    public void testCylinders() {
        Random random = new Random();
        
        // Create a cylinder, cast a random ray, and ensure everything goes well.
        Node scene = new Node("Scene Node");

        for (int i = 0; i < NUMBER_OF_TRIES; i++) {
            scene.detachAllChildren();

            Cylinder cylinder = new Cylinder(2, 8, 1, 1, true);
            Geometry geometry = new Geometry("cylinder", cylinder);
            geometry.rotate(FastMath.HALF_PI, 0, 0);
            scene.attachChild(geometry);

            // Cast a random ray, and count successes and IndexOutOfBoundsExceptions.
            Vector3f randomPoint = new Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat());
            Vector3f randomDirection = new Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat());
            randomDirection.normalizeLocal();

            Ray ray = new Ray(randomPoint, randomDirection);
            CollisionResults collisionResults = new CollisionResults();

            // If the geometry is invalid, this should throw various exceptions.
            scene.collideWith(ray, collisionResults);
        }
    }
}
