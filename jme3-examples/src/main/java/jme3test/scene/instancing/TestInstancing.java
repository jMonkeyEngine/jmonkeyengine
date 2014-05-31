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

package jme3test.scene.instancing;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.instancing.InstancedGeometry;
import com.jme3.scene.shape.Sphere;

public class TestInstancing extends SimpleApplication  {

    private InstancedGeometry instancedGeometry;
    
    public static void main(String[] args){
        TestInstancing app = new TestInstancing();
        //app.setShowSettings(false);
        //app.setDisplayFps(false);
        //app.setDisplayStatView(false);
        app.start();
    }

    private Geometry createInstance(float x, float z) {
        // Note: it doesn't matter what mesh or material we set here.
        Geometry geometry = new Geometry("randomGeom", instancedGeometry.getMesh());
        geometry.setMaterial(instancedGeometry.getMaterial());
        geometry.setLocalTranslation(x, 0, z);
        return geometry;
    }
    
    @Override
    public void simpleInitApp() {
        Sphere sphere = new Sphere(10, 10, 0.5f, true, false);
        Material material = new Material(assetManager, "Common/MatDefs/Misc/ShowNormals.j3md");
        material.setBoolean("UseInstancing", true);
        
        instancedGeometry = new InstancedGeometry(InstancedGeometry.Mode.Auto, "instanced_geom");
        instancedGeometry.setMaxNumInstances(60 * 60);
        instancedGeometry.setCurrentNumInstances(60 * 60);
        instancedGeometry.setCullHint(CullHint.Never);
        instancedGeometry.setMesh(sphere);
        instancedGeometry.setMaterial(material);
        rootNode.attachChild(instancedGeometry);
        
        Node instancedGeoms = new Node("instances_node");
        
        // Important: Do not render these geometries, only
        // use their world transforms to instance them via
        // InstancedGeometry.
        instancedGeoms.setCullHint(CullHint.Always);
        
        for (int y = -30; y < 30; y++) {
            for (int x = -30; x < 30; x++) {
                Geometry instance = createInstance(x, y);
                instancedGeoms.attachChild(instance);
            }
        }
        
        rootNode.attachChild(instancedGeoms);
        rootNode.setCullHint(CullHint.Never);

        int instanceIndex = 0;
        for (Spatial child : instancedGeoms.getChildren()) {
            if (instanceIndex < instancedGeometry.getMaxNumInstances()) {
                instancedGeometry.setInstanceTransform(instanceIndex++, child.getWorldTransform());
            }
        }
        
        instancedGeometry.setCurrentNumInstances(instanceIndex);

        cam.setLocation(new Vector3f(38.373516f, 6.689055f, 38.482082f));
        cam.setRotation(new Quaternion(-0.04004206f, 0.918326f, -0.096310444f, -0.38183528f));
        flyCam.setMoveSpeed(15);
    }

}
