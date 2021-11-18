/*
 * Copyright (c) 2009-2015 jMonkeyEngine
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

import static com.jme3.collision.CollisionUtil.*;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.math.FastMath;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import org.junit.Test;

/**
 * Tests collision detection between bounding volumes.
 * 
 * @author Kirill Vainer
 */
public class BoundingCollisionTest {
    
    @Test
    public void testBoxBoxCollision() {
        BoundingBox box1 = new BoundingBox(Vector3f.ZERO, 1, 1, 1);
        BoundingBox box2 = new BoundingBox(Vector3f.ZERO, 1, 1, 1);
        checkCollision(box1, box2, 1);
        
        // Put it at the very edge - should still intersect.
        box2.setCenter(new Vector3f(2f, 0f, 0f));
        checkCollision(box1, box2, 1);
        
        // Put it a wee bit farther - no intersection expected
        box2.setCenter(new Vector3f(2f + FastMath.ZERO_TOLERANCE, 0, 0));
        checkCollision(box1, box2, 0);
        
        // Check the corners.
        box2.setCenter(new Vector3f(2f, 2f, 2f));
        checkCollision(box1, box2, 1);
        
        box2.setCenter(new Vector3f(2f, 2f, 2f + FastMath.ZERO_TOLERANCE));
        checkCollision(box1, box2, 0);
    }
    
    @Test
    public void testSphereSphereCollision() {
        BoundingSphere sphere1 = new BoundingSphere(1, Vector3f.ZERO);
        BoundingSphere sphere2 = new BoundingSphere(1, Vector3f.ZERO);
        checkCollision(sphere1, sphere2, 1);
        
        // Put it at the very edge - should still intersect.
        sphere2.setCenter(new Vector3f(2f, 0f, 0f));
        checkCollision(sphere1, sphere2, 1);
        
        // Put it a wee bit farther - no intersection expected
        sphere2.setCenter(new Vector3f(2f + FastMath.ZERO_TOLERANCE, 0, 0));
        checkCollision(sphere1, sphere2, 0);
    }
    
    @Test
    public void testBoxSphereCollision() {
        BoundingBox box1 = new BoundingBox(Vector3f.ZERO, 1, 1, 1);
        BoundingSphere sphere2 = new BoundingSphere(1, Vector3f.ZERO);
        checkCollision(box1, sphere2, 1);
        
        // Put it at the very edge - for sphere vs. box, it will not intersect
        sphere2.setCenter(new Vector3f(2f, 0f, 0f));
        checkCollision(box1, sphere2, 0);
        
        // Put it a wee bit closer - should intersect.
        sphere2.setCenter(new Vector3f(2f - FastMath.ZERO_TOLERANCE, 0, 0));
        checkCollision(box1, sphere2, 1);
        
        // Test if the algorithm converts the sphere 
        // to a box before testing the collision (incorrect)
        float sqrt3 = FastMath.sqrt(3);
        
        sphere2.setCenter(Vector3f.UNIT_XYZ.mult(2));
        sphere2.setRadius(sqrt3);
        checkCollision(box1, sphere2, 0);
        
        // Make it a wee bit larger.
        sphere2.setRadius(sqrt3 + FastMath.ZERO_TOLERANCE);
        checkCollision(box1, sphere2, 1);
    }
    
    @Test
    public void testBoxRayCollision() {
        BoundingBox box = new BoundingBox(Vector3f.ZERO, 1, 1, 1);
        Ray ray = new Ray(Vector3f.ZERO, Vector3f.UNIT_Z);
        
        // XXX: seems incorrect, ray inside box should only generate
        // one result...
        checkCollision(box, ray, 2);
        
        ray.setOrigin(new Vector3f(0, 0, -5));
        checkCollision(box, ray, 2);
        
        // XXX: is this right? the ray origin is on the box's side.
        ray.setOrigin(new Vector3f(0, 0, 2f));
        checkCollision(box, ray, 0);
        
        ray.setOrigin(new Vector3f(0, 0, -2f));
        checkCollision(box, ray, 2);
        
        // parallel to the edge, touching the side
        ray.setOrigin(new Vector3f(0, 1f, -2f));
        checkCollision(box, ray, 2);
        
        // still parallel, but not touching the side
        ray.setOrigin(new Vector3f(0, 1f + FastMath.ZERO_TOLERANCE, -2f));
        checkCollision(box, ray, 0);
    }
    
    @Test
    public void testBoxTriangleCollision() {
        BoundingBox box = new BoundingBox(Vector3f.ZERO, 1, 1, 1);
        Geometry geom = new Geometry("geom", new Quad(1, 1));
        checkCollision(box, geom, 2); // Both triangles intersect
        
        // The box touches the edges of the triangles.
        box.setCenter(new Vector3f(-1f, 0, 0));
        checkCollision(box, geom, 2);
        
        // Move it slightly farther.
        box.setCenter(new Vector3f(-1f - FastMath.ZERO_TOLERANCE, 0, 0));
        checkCollision(box, geom, 0);
        
        // Parallel triangle / box side, touching
        box.setCenter(new Vector3f(0, 0, -1f));
        checkCollision(box, geom, 2);
        
        // Not touching
        box.setCenter(new Vector3f(0, 0, -1f - FastMath.ZERO_TOLERANCE));
        checkCollision(box, geom, 0);
        
        // Test collisions only against one of the triangles
        box.setCenter(new Vector3f(-1f, 1.5f, 0f));
        checkCollision(box, geom, 1);
        
        box.setCenter(new Vector3f(1.5f, -1f, 0f));
        checkCollision(box, geom, 1);
    }
    
    @Test
    public void testSphereTriangleCollision() {
        BoundingSphere sphere = new BoundingSphere(1, Vector3f.ZERO);
        Geometry geom = new Geometry("geom", new Quad(1, 1));
        checkCollision(sphere, geom, 2);
        
        // The box touches the edges of the triangles.
        sphere.setCenter(new Vector3f(-1f + FastMath.ZERO_TOLERANCE, 0, 0));
        checkCollision(sphere, geom, 2);
        
        // Move it slightly farther.
        sphere.setCenter(new Vector3f(-1f - FastMath.ZERO_TOLERANCE, 0, 0));
        checkCollision(sphere, geom, 0);
        
        // Parallel triangle / box side, touching
        sphere.setCenter(new Vector3f(0, 0, -1f));
        checkCollision(sphere, geom, 2);
        
        // Not touching
        sphere.setCenter(new Vector3f(0, 0, -1f - FastMath.ZERO_TOLERANCE));
        checkCollision(sphere, geom, 0);
        
        // Test collisions only against one of the triangles
        sphere.setCenter(new Vector3f(-0.9f, 1.2f, 0f));
        checkCollision(sphere, geom, 1);
        
        sphere.setCenter(new Vector3f(1.2f, -0.9f, 0f));
        checkCollision(sphere, geom, 1);
    }
}
