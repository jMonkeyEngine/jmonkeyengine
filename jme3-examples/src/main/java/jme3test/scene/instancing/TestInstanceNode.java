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

package jme3test.scene.instancing;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.Node;
import com.jme3.scene.instancing.InstancedGeometry;
import com.jme3.scene.instancing.InstancedNode;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;

public class TestInstanceNode extends SimpleApplication  {

    private Mesh mesh1;
    private Mesh mesh2;
    private final Material[] materials = new Material[6];
    private Node instancedNode;
    private float time = 0;
    final private boolean INSTANCING = true;
    
    public static void main(String[] args){
        TestInstanceNode app = new TestInstanceNode();
        AppSettings settings = new AppSettings(true);
        settings.setVSync(false);
        app.setSettings(settings);
        app.start();
    }

    private Geometry createInstance(float x, float z) {
        Mesh mesh; 
        if (FastMath.nextRandomInt(0, 1) == 1) mesh = mesh2;
        else mesh = mesh1;
        Geometry geometry = new Geometry("randomGeom", mesh);
        geometry.setMaterial(materials[FastMath.nextRandomInt(0, materials.length - 1)]);
        geometry.setLocalTranslation(x, 0, z);
        return geometry;
    }
    
    @Override
    public void simpleInitApp() {
        mesh1 = new Sphere(13, 13, 0.4f, true, false);
        mesh2 = new Box(0.4f, 0.4f, 0.4f);
        
        materials[0] = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        materials[0].setBoolean("UseInstancing", INSTANCING);
        materials[0].setColor("Color", ColorRGBA.Red);
        
        materials[1] = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        materials[1].setBoolean("UseInstancing", INSTANCING);
        materials[1].setColor("Color", ColorRGBA.Green);
        
        materials[2] = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        materials[2].setBoolean("UseInstancing", INSTANCING);
        materials[2].setColor("Color", ColorRGBA.Blue);
        
        materials[3] = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        materials[3].setBoolean("UseInstancing", INSTANCING);
        materials[3].setColor("Color", ColorRGBA.Cyan);
        
        materials[4] = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        materials[4].setBoolean("UseInstancing", INSTANCING);
        materials[4].setColor("Color", ColorRGBA.Magenta);
        
        materials[5] = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        materials[5].setBoolean("UseInstancing", INSTANCING);
        materials[5].setColor("Color", ColorRGBA.Yellow);
       
        instancedNode = new InstancedNode("instanced_node");
        
        rootNode.attachChild(instancedNode);
        
        int extent = 30;
        
        for (int y = -extent; y < extent; y++) {
            for (int x = -extent; x < extent; x++) {
                Geometry instance = createInstance(x, y);
                
                float height = (smoothstep(0, 1, FastMath.nextRandomFloat()) * 2.5f) - 1.25f;
                instance.setUserData("height", height);
                instance.setUserData("dir", 1f);
                
                instancedNode.attachChild(instance);
            }
        }
        
        if (INSTANCING) {
            ((InstancedNode)instancedNode).instance();
        }
        
        //instancedNode = (InstancedNode) instancedNode.clone();
        //instancedNode.move(0, 5, 0);
        //rootNode.attachChild(instancedNode);
        
        cam.setLocation(new Vector3f(38.373516f, 6.689055f, 38.482082f));
        cam.setRotation(new Quaternion(-0.04004206f, 0.918326f, -0.096310444f, -0.38183528f));
        flyCam.setMoveSpeed(15);
        flyCam.setEnabled(false);
    }
    
    private float smoothstep(float edge0, float edge1, float x) {
        // Scale, bias and saturate x to 0..1 range
        x = FastMath.clamp((x - edge0) / (edge1 - edge0), 0.0f, 1.0f);
        // Evaluate polynomial
        return x * x * (3 - 2 * x);
    }
    
    
    @Override
    public void simpleUpdate(float tpf) {
        time += tpf;

        if (time > 1f) {
            time = 0f;
            
            for (Spatial instance : instancedNode.getChildren()) {
                if (!(instance instanceof InstancedGeometry)) {
                    Geometry geom = (Geometry) instance;
                    geom.setMaterial(materials[FastMath.nextRandomInt(0, materials.length - 1)]);

                    Mesh mesh; 
                    if (FastMath.nextRandomInt(0, 1) == 1) mesh = mesh2;
                    else mesh = mesh1;
                    geom.setMesh(mesh);
                }
            }
        }
        
        for (Spatial child : instancedNode.getChildren()) {
            if (!(child instanceof InstancedGeometry)) {
                float val = ((Float)child.getUserData("height")).floatValue();
                float dir = ((Float)child.getUserData("dir")).floatValue();

                val += (dir + ((FastMath.nextRandomFloat() * 0.5f) - 0.25f)) * tpf;

                if (val > 1f) {
                    val = 1f;
                    dir = -dir;
                } else if (val < 0f) {
                    val = 0f;
                    dir = -dir;
                }

                Vector3f translation = child.getLocalTranslation();
                translation.y = (smoothstep(0, 1, val) * 2.5f) - 1.25f;

                child.setUserData("height", val);
                child.setUserData("dir", dir);

                child.setLocalTranslation(translation);
            }
        }
    }
}
