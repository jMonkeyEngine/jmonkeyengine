/*
 * Copyright (c) 2009-2018 jMonkeyEngine
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
package com.jme3.light;

import com.jme3.bounding.BoundingSphere;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.util.TempVars;
import org.junit.Before;
import org.junit.Test;

/**
 * Test light filtering for various light types.
 * 
 * @author Kirill Vainer
 */
public class LightFilterTest {
    
    private DefaultLightFilter filter;
    private Camera cam;
    private Geometry geom;
    private LightList list;
    
    private void checkFilteredLights(int expected) {
        geom.updateGeometricState();
        filter.setCamera(cam); // setCamera resets the intersection cache
        list.clear();
        filter.filterLights(geom, list);
        assert list.size() == expected;
    }
    
    @Before
    public void setUp() {
        filter = new DefaultLightFilter();
        
        cam = new Camera(512, 512);
        cam.setFrustumPerspective(45, 1, 1, 1000);
        cam.setLocation(Vector3f.ZERO);
        cam.lookAtDirection(Vector3f.UNIT_Z, Vector3f.UNIT_Y);
        filter.setCamera(cam);
        
        Box box = new Box(1, 1, 1);
        geom = new Geometry("geom", box);
        geom.setLocalTranslation(0, 0, 10);
        geom.updateGeometricState();
        list = new LightList(geom);
    }
    
    @Test
    public void testAmbientFiltering() {
        geom.addLight(new AmbientLight());
        checkFilteredLights(1); // Ambient lights must never be filtered
        
        // Test for bounding Sphere
        geom.setModelBound(new BoundingSphere(0.5f, Vector3f.ZERO));
        checkFilteredLights(1); // Ambient lights must never be filtered
    }
    
    @Test
    public void testDirectionalFiltering() {
        geom.addLight(new DirectionalLight(Vector3f.UNIT_Y));
        checkFilteredLights(1); // Directional lights must never be filtered
        
        // Test for bounding Sphere
        geom.setModelBound(new BoundingSphere(0.5f, Vector3f.ZERO));
        checkFilteredLights(1); // Directional lights must never be filtered
    }
    
    @Test
    public void testPointFiltering() {
        PointLight pl = new PointLight(Vector3f.ZERO);
        geom.addLight(pl);
        checkFilteredLights(1); // Infinite point lights must never be filtered
        
        // Light at origin does not intersect geom which is at Z=10
        pl.setRadius(1);
        checkFilteredLights(0);
        
        // Put it closer to geom, the very edge of the sphere touches the box.
        // Still not considered an intersection though.
        pl.setPosition(new Vector3f(0, 0, 8f));
        checkFilteredLights(0);
        
        // And closer - now they intersect.
        pl.setPosition(new Vector3f(0, 0, 8f + FastMath.ZERO_TOLERANCE));
        checkFilteredLights(1);
        
        // Move the geometry away
        geom.move(0, 0, FastMath.ZERO_TOLERANCE);
        checkFilteredLights(0);
        
        // Test if the algorithm converts the sphere 
        // to a box before testing the collision (incorrect)
        float sqrt3 = FastMath.sqrt(3);
        
        pl.setPosition(new Vector3f(2, 2, 8));
        pl.setRadius(sqrt3);
        checkFilteredLights(0);
        
        // Make it a wee bit larger.
        pl.setRadius(sqrt3 + FastMath.ZERO_TOLERANCE);
        checkFilteredLights(1);
        
        // Rotate the camera so it is up, light is outside frustum.
        cam.lookAtDirection(Vector3f.UNIT_Y, Vector3f.UNIT_Y);
        checkFilteredLights(0);
        
        // ==================================
        // Tests for bounding Sphere
        geom.setModelBound(new BoundingSphere(1f, Vector3f.ZERO));
        geom.setLocalTranslation(0, 0, 2);
        pl.setPosition(new Vector3f(0, 0, 2f));

        // Infinite point lights must never be filtered
        pl.setRadius(0);
        checkFilteredLights(1);
     
        pl.setRadius(1f);
        // Put the light very close to the geometry.
        // The edge of the sphere touches the other bounding sphere.
        // Still not considered an intersection though.
        pl.setPosition(new Vector3f(0, 0, 0));
        checkFilteredLights(0);

        // And closer - now they intersect.
        pl.setPosition(new Vector3f(0, 0, 0f + FastMath.ZERO_TOLERANCE));
        checkFilteredLights(1);
               
        geom.setLocalTranslation(0, 0, 0);
        // In this case, it's an intersection for PointLight versus box,
        // but not for PointLight versus sphere.
        // Vector3f(0, 0.5f, 0.5f).normalize().mult(2) ~ >= (0.0, 1.4142135, 1.4142135)
        //pl.setPosition(new Vector3f(0, 0.5f, 0.5f).normalizeLocal().multLocal(2 + FastMath.ZERO_TOLERANCE));
        pl.setPosition(new Vector3f(0f, 1.4142135f, 1.4142135f).multLocal(1+FastMath.ZERO_TOLERANCE));
        checkFilteredLights(0);
        
        // Make the distance a wee bit closer. Now it intersects.
        //pl.setPosition(new Vector3f(0, 0.5f, 0.5f).normalizeLocal().multLocal(2 - FastMath.ZERO_TOLERANCE));
        pl.setPosition(new Vector3f(0f, 1.4142135f, 1.4142135f).multLocal(1-FastMath.ZERO_TOLERANCE));
        checkFilteredLights(1);
        
        // It's a point light; also test for the other corner.
        pl.setPosition(new Vector3f(0f, -1.4142135f, -1.4142135f).multLocal(1-FastMath.ZERO_TOLERANCE));
        checkFilteredLights(0);

    }
    
    @Test
    public void testSpotFiltering() {
        SpotLight sl = new SpotLight(Vector3f.ZERO, Vector3f.UNIT_Z);
        sl.setSpotRange(0);
        geom.addLight(sl);
        checkFilteredLights(1); // Infinite spot lights are only filtered
                                // if the geometry is outside the infinite cone.
        
        TempVars vars = TempVars.get();
        try {
            // The spot is not touching the near plane of the camera yet, 
            // should still be culled.
            sl.setSpotRange(1f - FastMath.ZERO_TOLERANCE);
            assert !sl.intersectsFrustum(cam, vars);
            // should be culled from the geometry's PoV
            checkFilteredLights(0);
            
            // Now it touches the near plane.
            sl.setSpotRange(1f);
            // still culled from the geometry's PoV
            checkFilteredLights(0);
            assert sl.intersectsFrustum(cam, vars);
        } finally {
            vars.release();
        }
        
        // make it barely reach the geometry
        sl.setSpotRange(9f);
        checkFilteredLights(0);
        
        // make it reach the geometry (touching its bound)
        sl.setSpotRange(9f + FastMath.ZERO_TOLERANCE);
        checkFilteredLights(1);
        
        // rotate the cone a bit so it no longer faces the geom
        sl.setDirection(new Vector3f(0.316f, 0, 0.948f).normalizeLocal());
        checkFilteredLights(0);
        
        // extent the range much farther
        sl.setSpotRange(20);
        checkFilteredLights(0);
        
        // Create box of size X=10 (double the extent)
        // now, the spot will touch the box.
        geom.setMesh(new Box(5, 1, 1));
        checkFilteredLights(1);
        
        // ==================================
        // Tests for bounding sphere, with a radius of 1f (in the box geom)
        sl.setPosition(Vector3f.ZERO);
        sl.setDirection(Vector3f.UNIT_Z);
        geom.setLocalTranslation(Vector3f.ZERO);
        geom.setModelBound(new BoundingSphere(1f, Vector3f.ZERO));
       
        // Infinite spot lights are only filtered
        // if the geometry is outside the infinite cone.
        sl.setSpotRange(0);
        checkFilteredLights(1);
       
        //the geometry is outside the infinite cone (cone direction going away from the geom)
        sl.setPosition(Vector3f.UNIT_Z.mult(1+FastMath.ZERO_TOLERANCE));
        checkFilteredLights(0);
       
        //place the spot light in the corner of the box geom, (in order to test bounding sphere)
        sl.setDirection(new Vector3f(1, 1, 0).normalizeLocal());
        geom.setLocalTranslation(0, 0, 10);
        sl.setPosition(sl.getDirection().mult(-2f).add(geom.getLocalTranslation()));

        // make it barely reach the sphere, incorrect with a box
        sl.setSpotRange(1f - FastMath.ZERO_TOLERANCE);
        checkFilteredLights(0);
       
        // make it reach the sphere
        sl.setSpotRange(1f + FastMath.ZERO_TOLERANCE);
        checkFilteredLights(1);
       
        // extend the range
        sl.setPosition(Vector3f.ZERO);
        sl.setDirection(Vector3f.UNIT_Z);
        sl.setSpotRange(20);
        checkFilteredLights(1);
               
        // rotate the cone a bit so it no longer faces the geom
        sl.setDirection(new Vector3f(0, 0.3f, 0.7f).normalizeLocal());
        checkFilteredLights(0);
       
        // Create sphere of size X=10 (double the radius)
        // now, the spot will touch the sphere.
        geom.setModelBound(new BoundingSphere(5f, Vector3f.ZERO));
        checkFilteredLights(1);
    }
}
