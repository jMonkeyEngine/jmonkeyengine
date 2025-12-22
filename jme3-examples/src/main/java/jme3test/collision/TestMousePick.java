/*
 * Copyright (c) 2009-2025 jMonkeyEngine
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
package jme3test.collision;

import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.Materials;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Sphere;
import com.jme3.scene.shape.Torus;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.EdgeFilteringMode;

/**
 * The primary purpose of TestMousePick is to illustrate how to detect intersections 
 * between a ray (originating from the camera's cursor position) and 3D objects in the scene. 
 * <p> 
 * When an intersection occurs, a visual marker (a red arrow) 
 * is placed at the collision point, and the name of the 
 * intersected object is displayed on the HUD.
 * 
 * @author capdevon
 */
public class TestMousePick extends SimpleApplication {

    public static void main(String[] args) {
        TestMousePick app = new TestMousePick();
        app.start();
    }

    private BitmapText hud;
    private Node shootables;
    private Geometry mark;

    @Override
    public void simpleInitApp() {
        hud = createLabel(10, 10, "Text");
        configureCamera();
        initMark();
        setupScene();
        setupLights();
    }

    private void configureCamera() {
        flyCam.setMoveSpeed(15f);
        flyCam.setDragToRotate(true);

        cam.setLocation(Vector3f.UNIT_XYZ.mult(6));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
    }

    private void setupScene() {
        /* Create four colored boxes and a floor to shoot at: */
        shootables = new Node("Shootables");
        shootables.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        rootNode.attachChild(shootables);

        Geometry sphere = makeShape("Sphere", new Sphere(32, 32, 1f), ColorRGBA.randomColor());
        sphere.setLocalTranslation(-2f, 0f, 1f);
        shootables.attachChild(sphere);

        Geometry box = makeShape("Box", new Box(1, 1, 1), ColorRGBA.randomColor());
        box.setLocalTranslation(1f, -2f, 0f);
        shootables.attachChild(box);

        Geometry cylinder = makeShape("Cylinder", new Cylinder(16, 16, 1.0f, 1.0f, true), ColorRGBA.randomColor());
        cylinder.setLocalTranslation(0f, 1f, -2f);
        cylinder.rotate(90 * FastMath.DEG_TO_RAD, 0, 0);
        shootables.attachChild(cylinder);

        Geometry torus = makeShape("Torus", new Torus(16, 16, 0.15f, 0.5f), ColorRGBA.randomColor());
        torus.setLocalTranslation(1f, 0f, -4f);
        shootables.attachChild(torus);

        // load a character from jme3-testdata
        Spatial golem = assetManager.loadModel("Models/Oto/Oto.mesh.xml");
        golem.scale(0.5f);
        golem.setLocalTranslation(-1.0f, -1.5f, -0.6f);
        shootables.attachChild(golem);

        Geometry floor = makeShape("Floor", new Box(15, .2f, 15), ColorRGBA.Gray);
        floor.setLocalTranslation(0, -4, -5);
        shootables.attachChild(floor);
    }

    private void setupLights() {
        // We must add a light to make the model visible
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.1f, -0.7f, -1.0f).normalizeLocal());
        rootNode.addLight(sun);

        // init shadows
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        DirectionalLightShadowFilter dlsf = new DirectionalLightShadowFilter(assetManager, 2048, 3);
        dlsf.setLight(sun);
        dlsf.setLambda(0.55f);
        dlsf.setShadowIntensity(0.8f);
        dlsf.setEdgeFilteringMode(EdgeFilteringMode.PCFPOISSON);
        fpp.addFilter(dlsf);
        viewPort.addProcessor(fpp);
    }

    private final CollisionResults results = new CollisionResults();
    private final Quaternion tempQuat = new Quaternion();

    @Override
    public void simpleUpdate(float tpf){

        Ray ray = cam.screenPointToRay(inputManager.getCursorPosition());
        results.clear();
        shootables.collideWith(ray, results);

        if (results.size() > 0) {
            CollisionResult closest = results.getClosestCollision();
            Vector3f point = closest.getContactPoint();
            Vector3f normal = closest.getContactNormal();

            tempQuat.lookAt(normal, Vector3f.UNIT_Y);
            mark.setLocalRotation(tempQuat);
            mark.setLocalTranslation(point);

            rootNode.attachChild(mark);
            hud.setText(closest.getGeometry().toString());

        } else {
            hud.setText("No collision");
            rootNode.detachChild(mark);
        }
    }

    private BitmapText createLabel(int x, int y, String text) {
        BitmapText bmp = guiFont.createLabel(text);
        bmp.setLocalTranslation(x, settings.getHeight() - y, 0);
        bmp.setColor(ColorRGBA.Red);
        guiNode.attachChild(bmp);
        return bmp;
    }

    private Geometry makeShape(String name, Mesh mesh, ColorRGBA color) {
        Geometry geo = new Geometry(name, mesh);
        Material mat = new Material(assetManager, Materials.LIGHTING);
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Diffuse", color);
        geo.setMaterial(mat);
        return geo;
    }

    /**
     * A red arrow to mark the spot being picked.
     */
    private void initMark() {
        Arrow arrow = new Arrow(Vector3f.UNIT_Z.mult(2f));
        mark = new Geometry("Marker", arrow);
        Material mat = new Material(assetManager, Materials.UNSHADED);
        mat.setColor("Color", ColorRGBA.Red);
        mark.setMaterial(mat);
    }

}
