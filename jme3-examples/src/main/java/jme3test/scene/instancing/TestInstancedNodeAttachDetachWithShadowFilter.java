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
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.instancing.InstancedNode;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.system.AppSettings;

/**
 * A test case for using instancing with shadow filter.
 *
 * Based on distance from camera, swap in/out more/less detailed geometry to/from an InstancedNode.
 *
 * @author duncanj
 */
public class TestInstancedNodeAttachDetachWithShadowFilter extends SimpleApplication {
    public static void main(String[] args) {
        TestInstancedNodeAttachDetachWithShadowFilter app = new TestInstancedNodeAttachDetachWithShadowFilter();
        AppSettings settings = new AppSettings(true);
        settings.setVSync(false);
        app.setSettings(settings);
        app.start();
    }

    private FilterPostProcessor filterPostProcessor;
    private InstancedNode instancedNode;

    final private Vector3f[] locations = new Vector3f[10];
    final private Geometry[] spheres = new Geometry[10];
    final private Geometry[] boxes = new Geometry[10];

    @Override
    public void simpleInitApp() {
        filterPostProcessor = new FilterPostProcessor(assetManager);
        getViewPort().addProcessor(filterPostProcessor);

        addDirectionalLight();
        addAmbientLight();

        Material instancingMaterial = createLightingMaterial(true, ColorRGBA.LightGray);

        instancedNode = new InstancedNode("theParentInstancedNode");
        instancedNode.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        rootNode.attachChild(instancedNode);

        // create 10 spheres & boxes, along the z-axis, successively further from the camera
        Mesh sphereMesh = new Sphere(32, 32, 1f);
        Mesh boxMesh = new Box(0.7f, 0.7f, 0.7f);
        for (int z = 0; z < 10; z++) {
            Vector3f location = new Vector3f(0, -3, -(z * 4));
            locations[z] = location;

            Geometry sphere = new Geometry("sphere", sphereMesh);
            sphere.setMaterial(instancingMaterial);
            sphere.setLocalTranslation(location);
            instancedNode.attachChild(sphere);       // initially just add the spheres to the InstancedNode
            spheres[z] = sphere;

            Geometry box = new Geometry("box", boxMesh);
            box.setMaterial(instancingMaterial);
            box.setLocalTranslation(location);
            boxes[z] = box;
        }

        instancedNode.instance();


        Geometry floor = new Geometry("floor", new Box(20, 0.1f, 40));
        floor.setMaterial(createLightingMaterial(false, ColorRGBA.Yellow));
        floor.setLocalTranslation(5, -5, 0);
        floor.setShadowMode(RenderQueue.ShadowMode.Receive);
        rootNode.attachChild(floor);

        flyCam.setMoveSpeed(30);
    }

    @Override
    public void simpleUpdate(float tpf) {
        // Each frame, determine the distance to each sphere/box from the camera.
        // If the object is > 25 units away, switch in the Box.  If it's nearer, switch in the Sphere.
        // Normally we wouldn't do this every frame, only when player has moved a sufficient distance, etc.

        boolean modified = false;
        for (int i = 0; i < 10; i++) {
            Vector3f location = locations[i];
            float distance = location.distance(cam.getLocation());

            if(distance > 25.0f && boxes[i].getParent() == null) {
                modified = true;
                instancedNode.attachChild(boxes[i]);
                instancedNode.detachChild(spheres[i]);
            } else if(distance <= 25.0f && spheres[i].getParent() == null) {
                modified = true;
                instancedNode.attachChild(spheres[i]);
                instancedNode.detachChild(boxes[i]);
            }
        }

        if(modified) {
            instancedNode.instance();
        }
    }

    private Material createLightingMaterial(boolean useInstancing, ColorRGBA color) {
        Material material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        material.setBoolean("UseMaterialColors", true);
        material.setBoolean("UseInstancing", useInstancing);
        material.setColor("Ambient", color);
        material.setColor("Diffuse", color);
        material.setColor("Specular", color);
        material.setFloat("Shininess", 1.0f);
        return material;
    }

    private void addAmbientLight() {
        AmbientLight ambientLight = new AmbientLight(new ColorRGBA(0.1f, 0.1f, 0.1f, 1.0f));
        rootNode.addLight(ambientLight);
    }

    private void addDirectionalLight() {
        DirectionalLight light = new DirectionalLight();

        light.setColor(ColorRGBA.White);
        light.setDirection(new Vector3f(-1, -1, -1));

        DirectionalLightShadowFilter dlsf = new DirectionalLightShadowFilter(assetManager, 1024, 1);
        dlsf.setLight(light);
        dlsf.setEdgeFilteringMode(EdgeFilteringMode.PCFPOISSON);
        filterPostProcessor.addFilter(dlsf);

        rootNode.addLight(light);
    }
}