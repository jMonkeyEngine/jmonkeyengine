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
package com.jme3.light;

import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import org.junit.Test;

/**
 * Test light sorting (in the scene graph) for various light types.
 * 
 * @author Kirill Vainer
 */
public class LightSortTest {
    
    @Test
    public void testSimpleSort() {
        Geometry g = new Geometry("test", new Mesh());
        LightList list = new LightList(g);
        
        list.add(new SpotLight(Vector3f.ZERO, Vector3f.UNIT_X));
        list.add(new PointLight(Vector3f.UNIT_X));
        list.add(new DirectionalLight(Vector3f.UNIT_X));
        list.add(new AmbientLight());
        
        list.sort(true);
        
        assert list.get(0) instanceof AmbientLight;     // Ambients always first
        assert list.get(1) instanceof DirectionalLight; // ... then directionals
        assert list.get(2) instanceof SpotLight;        // Spot is 0 units away from geom
        assert list.get(3) instanceof PointLight;       // ... and point is 1 unit away.
    }
    
    @Test
    public void testSceneGraphSort() {
        Node n = new Node("node");
        Geometry g = new Geometry("geom", new Mesh());
        SpotLight spot = new SpotLight(Vector3f.ZERO, Vector3f.UNIT_X);
        PointLight point = new PointLight(Vector3f.UNIT_X);
        DirectionalLight directional = new DirectionalLight(Vector3f.UNIT_X);
        AmbientLight ambient = new AmbientLight();
        
        // Some lights are on the node
        n.addLight(spot);
        n.addLight(point);
        
        // ... and some on the geometry.
        g.addLight(directional);
        g.addLight(ambient);
        
        n.attachChild(g);
        n.updateGeometricState();
        
        LightList list = g.getWorldLightList();
        
        // check the sorting (when geom is at 0,0,0)
        assert list.get(0) instanceof AmbientLight;
        assert list.get(1) instanceof DirectionalLight;
        assert list.get(2) instanceof SpotLight;
        assert list.get(3) instanceof PointLight;
        
        // move the geometry closer to the point light
        g.setLocalTranslation(Vector3f.UNIT_X);
        n.updateGeometricState();
        
        assert list.get(0) instanceof AmbientLight;
        assert list.get(1) instanceof DirectionalLight;
        assert list.get(2) instanceof PointLight;
        assert list.get(3) instanceof SpotLight;
        
        // now move the point light away from the geometry
        // and the spot light closer
        
        // XXX: doesn't work! jME can't detect that the light moved!
//        point.setPosition(Vector3f.ZERO);
//        spot.setPosition(Vector3f.UNIT_X);
//        n.updateGeometricState();
//        
//        assert list.get(0) instanceof AmbientLight;
//        assert list.get(1) instanceof DirectionalLight;
//        assert list.get(2) instanceof SpotLight;
//        assert list.get(3) instanceof PointLight;
    }
}
