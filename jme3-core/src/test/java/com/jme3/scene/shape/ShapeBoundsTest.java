/*
 * Copyright (c) 2009-2019 jMonkeyEngine
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
package com.jme3.scene.shape;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests that all shapes have had a world bound calculated. Test added for issue #1121. This is a clear
 * failure: BoundingBox [Center: (0.0, 0.0, 0.0) xExtent: 0.0 yExtent: 0.0 zExtent: 0.0]
 *
 * @author lou
 */
public class ShapeBoundsTest {

    @Test
    public void testBox() {
        Box shape = new Box(2f, 3f, 0.8f);
        Geometry geometry = new Geometry("geom", shape);
        BoundingVolume bv = geometry.getWorldBound();
        testBounds(bv);
    }

    @Test
    public void testCurve() {
        Vector3f[] controlp = new Vector3f[4];
        controlp[0] = new Vector3f(0, 0, 0);
        controlp[1] = new Vector3f(1, 1, 1);
        controlp[2] = new Vector3f(2, 1, 1);
        controlp[3] = new Vector3f(3, 2, 1);
        Curve shape = new Curve(controlp, 32);
        Geometry geometry = new Geometry("geom", shape);
        BoundingVolume bv = geometry.getWorldBound();
        testBounds(bv);
    }

    @Test
    public void testCylinder() {
        Cylinder shape = new Cylinder(16, 16, 2f, 3f);
        Geometry geometry = new Geometry("geom", shape);
        BoundingVolume bv = geometry.getWorldBound();
        testBounds(bv);
    }

    @Test
    public void testDome() {
        Dome shape = new Dome(16, 16, 5f);
        Geometry geometry = new Geometry("geom", shape);
        BoundingVolume bv = geometry.getWorldBound();
        testBounds(bv);
    }

    @Test
    public void testLine() {
        Line shape = new Line(new Vector3f(0, 0, 0), new Vector3f(1, 2, 3));
        Geometry geometry = new Geometry("geom", shape);
        BoundingVolume bv = geometry.getWorldBound();
        testBounds(bv);
    }

    @Test
    public void testPqTorus() {
        PQTorus shape = new PQTorus(2f, 3f, 0.8f, 0.2f, 64, 16);
        Geometry geometry = new Geometry("geom", shape);
        BoundingVolume bv = geometry.getWorldBound();
        testBounds(bv);
    }

    @Test
    public void testQuad() {
        Quad shape = new Quad(64, 16);
        Geometry geometry = new Geometry("geom", shape);
        BoundingVolume bv = geometry.getWorldBound();
        BoundingBox bb = (BoundingBox) bv;
        Assert.assertTrue(bb.getXExtent() > 0 && bb.getYExtent() > 0);//Quad z extent 0 is normal
    }

    @Test
    public void Sphere() {
        Sphere shape = new Sphere(32, 32, 5f);
        Geometry geometry = new Geometry("geom", shape);
        BoundingVolume bv = geometry.getWorldBound();
        testBounds(bv);
    }

    @Test
    public void StripBox() {
        StripBox shape = new StripBox(0.2f, 64f, 16f);
        Geometry geometry = new Geometry("geom", shape);
        BoundingVolume bv = geometry.getWorldBound();
        testBounds(bv);
    }

    @Test
    public void Torus() {
        Torus shape = new Torus(32, 32, 2f, 3f);
        Geometry geometry = new Geometry("geom", shape);
        BoundingVolume bv = geometry.getWorldBound();
        testBounds(bv);
    }

    private void testBounds(BoundingVolume bv) {
        if (bv instanceof BoundingBox) {
            BoundingBox bb = (BoundingBox) bv;
            Assert.assertTrue(bb.getXExtent() > 0 && bb.getYExtent() > 0 && bb.getZExtent() > 0);
        } else if (bv instanceof BoundingSphere) {
            BoundingSphere bs = (BoundingSphere) bv;
            Assert.assertTrue(bs.getRadius() > 1f);
        }
    }
}
