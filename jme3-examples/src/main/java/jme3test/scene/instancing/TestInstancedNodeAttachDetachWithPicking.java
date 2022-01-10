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
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.instancing.InstancedNode;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;


/**
 * A test case for using instancing with ray casting.
 *
 * Based on distance from camera, swap in/out more/less detailed geometry to/from an InstancedNode.
 *
 * @author duncanj
 */
public class TestInstancedNodeAttachDetachWithPicking extends SimpleApplication {
    public static void main(String[] args) {
        TestInstancedNodeAttachDetachWithPicking app = new TestInstancedNodeAttachDetachWithPicking();
        AppSettings settings = new AppSettings(true);
        settings.setVSync(false);
        app.setSettings(settings);
        app.start();
    }

    private InstancedNode instancedNode;

    final private Vector3f[] locations = new Vector3f[10];
    final private Geometry[] spheres = new Geometry[10];
    final private Geometry[] boxes = new Geometry[10];

    @Override
    public void simpleInitApp() {
        addPointLight();
        addAmbientLight();

        Material material = createInstancedLightingMaterial();

        instancedNode = new InstancedNode("theParentInstancedNode");
        rootNode.attachChild(instancedNode);
        Sphere sphereMesh = new Sphere(16, 16, 1f);
        Box boxMesh = new Box(0.7f, 0.7f, 0.7f);
        // create 10 spheres & boxes, positioned along Z-axis successively further from the camera
        for (int i = 0; i < 10; i++) {
            Vector3f location = new Vector3f(0, -3, -(i*5));
            locations[i] = location;

            Geometry sphere = new Geometry("sphere", sphereMesh);
            sphere.setMaterial(material);
            sphere.setLocalTranslation(location);
            instancedNode.attachChild(sphere);       // initially just add the spheres to the InstancedNode
            spheres[i] = sphere;

            Geometry box = new Geometry("box", boxMesh);
            box.setMaterial(material);
            box.setLocalTranslation(location);
            boxes[i] = box;
        }
        instancedNode.instance();

        flyCam.setMoveSpeed(30);


        addCrossHairs();

        // when you left-click, print the distance to the object to system.out
        inputManager.addMapping("leftClick", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if( isPressed ) {
                    CollisionResult result = pickFromCamera();
                    if( result != null ) {
                        System.out.println("Picked = " + result.getGeometry() + ", Distance = "+result.getDistance());
                    }
                }
            }
        }, "leftClick");
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

    private Material createInstancedLightingMaterial() {
        Material material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        material.setBoolean("UseMaterialColors", true);
        material.setBoolean("UseInstancing", true);
        material.setColor("Ambient", ColorRGBA.Red);
        material.setColor("Diffuse", ColorRGBA.Red);
        material.setColor("Specular", ColorRGBA.Red);
        material.setFloat("Shininess", 1.0f);
        return material;
    }

    private void addAmbientLight() {
        AmbientLight ambientLight = new AmbientLight(new ColorRGBA(0.2f, 0.2f, 0.2f, 1.0f));
        rootNode.addLight(ambientLight);
    }

    private void addPointLight() {
        PointLight pointLight = new PointLight();
        pointLight.setColor(ColorRGBA.White);
        pointLight.setRadius(100f);
        pointLight.setPosition(new Vector3f(10f, 10f, 0));
        rootNode.addLight(pointLight);
    }

    private void addCrossHairs() {
        BitmapText ch = new BitmapText(guiFont);
        ch.setSize(guiFont.getCharSet().getRenderedSize()+4);
        ch.setText("+"); // crosshairs
        ch.setColor(ColorRGBA.White);
        ch.setLocalTranslation( // center
                settings.getWidth() / 2 - ch.getLineWidth() / 2,
                settings.getHeight() / 2 + ch.getLineHeight() / 2, 0);
        guiNode.attachChild(ch);
    }

    private CollisionResult pickFromCamera() {
        CollisionResults results = new CollisionResults();
        Ray ray = new Ray(cam.getLocation(), cam.getDirection());
        instancedNode.collideWith(ray, results);
        return results.getClosestCollision();
    }
}